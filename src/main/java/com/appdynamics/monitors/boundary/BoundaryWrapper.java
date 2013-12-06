package com.appdynamics.monitors.boundary;

import com.google.gson.*;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.*;

public class BoundaryWrapper {

    private static final Logger logger = Logger.getLogger(BoundaryWrapper.class.getSimpleName());
    private String apiKey;
    private String orgId;
    private HashMap<String, String> meterIds = new HashMap<String, String>();

    private static List<String> metricNames = Arrays.asList(
            "ingressPackets",
            "ingressOctets",
            "egressPackets",
            "egressOctets",
            "appRttUsec",
            "handshakeRttUsec",
            "retransmits",
            "outOfOrder",
            "activeFlows");

    public BoundaryWrapper(Map<String, String> taskArguments) {
        this.apiKey = taskArguments.get("api-key");
        this.orgId = taskArguments.get("org-id");
    }

    /**
     * Retrieves metrics using the Boundary REST API
     * @return 	Map     Map containing metrics retrieved using the Boundary REST API
     */
    public Map gatherMetrics() throws Exception{
        try {
            populateObservationDomainIds();
            Iterator iterator = meterIds.entrySet().iterator();
            HashMap<String, HashMap<String, Long>> metrics = new HashMap<String, HashMap<String, Long>>();
            while (iterator.hasNext()) {
                Map.Entry<String, String> meterInfo = (Map.Entry<String,String>) iterator.next();
                String meterName = meterInfo.getKey();
                String meterObservationDomainId = meterInfo.getValue();
                JsonArray responseData = getResponseData(meterObservationDomainId);
                populateMetricsMap(responseData, metrics, meterName);
            }
            return metrics;
        } catch(MalformedURLException e) {
            logger.error("Invalid URL used to connect to Boundary");
            throw e;
        } catch(JsonSyntaxException e) {
            logger.error("Error parsing the Json response");
            throw e;
        } catch(IOException e) {
            throw e;
        }
    }

    /**
     * Constructs a metrics map based on the Json response data retrieved from the REST API
     * @param   responseData    The extracted 'data' field from the Json response
     * @param   metricsMap      Map that is to be populated with metrics parsed from the responseData
     * @param 	meterName       Name of the Boundary application node
     */
    private void populateMetricsMap(JsonArray responseData, Map metricsMap, String meterName) throws Exception{
        for (int i = 0; i < responseData.size(); i++) {
            JsonArray ipMetricsArray = responseData.get(i).getAsJsonArray();
            HashMap<String, Long> ipMetrics = new HashMap<String, Long>();
            for (int j = 1; j < ipMetricsArray.size(); j++) {
                ipMetrics.put(metricNames.get(j - 1), ipMetricsArray.get(j).getAsLong());
            }
            metricsMap.put(meterName, ipMetrics);
        }
    }

    /**
     * Retrieves observation domain ids from the /meters REST request
     * @return  Map       A map containing the name of the meter and it's corresponding observationDomainId
     */
    private void populateObservationDomainIds() throws Exception {
        HttpGet httpGet = new HttpGet(constructMetersURL());
        httpGet.addHeader(BasicScheme.authenticate(
                new UsernamePasswordCredentials(apiKey, ""),
                "UTF-8", false));

        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(entity.getContent()));
        StringBuilder responseString = new StringBuilder();
        String line = "";
        while ((line = bufferedReader2.readLine()) != null) {
            responseString.append(line);
        }

        JsonArray responseArray = new JsonParser().parse(responseString.toString()).getAsJsonArray();

        for (int i = 0; i < responseArray.size(); i++) {
            JsonObject obj = responseArray.get(i).getAsJsonObject();
            meterIds.put(obj.get("name").getAsString(), obj.get("obs_domain_id").getAsString());
        }
    }

    /**
     * Retrieves network traffic data in the past minute from Boundary
     * @param   observationIds     A comma separated list of valid observationIds needed to make the historical API REST request
     * @return  responseData       A JsonArray containing all the ip_addresses, and their respective network traffic metrics
     */
    private JsonArray getResponseData(String observationIds) throws Exception{
        String metricsURL = constructMetricsURL();
        HttpPost httpPost = new HttpPost(metricsURL);
        httpPost.addHeader(BasicScheme.authenticate(
                new UsernamePasswordCredentials(apiKey, ""),
                "UTF-8", false));

        // Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("aggregations", "observationDomainId"));
        params.add(new BasicNameValuePair("observationDomainIds", observationIds));
        Long currentTime = System.currentTimeMillis();
        Long oneMinuteAgo = currentTime - 60000;
        params.add(new BasicNameValuePair("from", oneMinuteAgo.toString()));
        params.add(new BasicNameValuePair("to", currentTime.toString()));
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(entity.getContent()));
        StringBuilder responseString = new StringBuilder();
        String line = "";
        while ((line = bufferedReader2.readLine()) != null) {
            responseString.append(line);
        }
        JsonObject responseObject = new JsonParser().parse(responseString.toString()).getAsJsonObject();
        JsonArray responseData = responseObject.getAsJsonArray("data");
        return responseData;
    }


    /**
     * Construct the REST URL for Boundary's historical data
     * @return	The Boundary history REST url string
     */
    private String constructMetricsURL() {
        return new StringBuilder()
                .append("https://api.boundary.com/")
                .append(orgId)
                .append("/volume_1m_meter/history")
                .toString();
    }

    /**
     * Construct the REST URL for Boundary's meters path
     * @return	The Boundary meters REST url string
     */
    private String constructMetersURL() {
        return new StringBuilder()
                .append("https://api.boundary.com/")
                .append(orgId)
                .append("/meters")
                .toString();
    }
}

