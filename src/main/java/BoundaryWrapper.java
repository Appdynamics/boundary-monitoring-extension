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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.*;

public class BoundaryWrapper {

    private static final Logger logger = Logger.getLogger(BoundaryWrapper.class.getSimpleName());
    private String apiKey;
    private String orgId;

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

    // Should this be singleton??

    public BoundaryWrapper(Map<String, String> taskArguments) {
        this.apiKey = taskArguments.get("api-key");
        this.orgId = taskArguments.get("org-id");
    }

    /**
     * Connects to the couchbase cluster host and retrieves metrics using the CouchBase REST API
     * @return 	HashMap     Map containing metrics retrieved from using the CouchBase REST API
     */
    public Map gatherMetrics() throws Exception{
        HttpURLConnection connection = null;
        InputStream is = null;

        try {
            String observationIds = getObservationIds();
            JsonArray responseData = getResponseData(observationIds);

            Map metrics = constructMetricsMap(responseData);

            //System.out.println("Done gathering metrics" + responseString.toString());
            return metrics;
        } catch(MalformedURLException e) {
            logger.error("Invalid URL used to connect to CouchDB");
            throw e;
        } catch(JsonSyntaxException e) {
            logger.error("Error parsing the Json response");
            throw e;
        } catch(IOException e) {
            throw e;
        }
    }

    private Map constructMetricsMap(JsonArray responseData) {
        HashMap<String, HashMap<String, Long>> metrics = new HashMap<String, HashMap<String, Long>>();

        for (int i = 0; i < responseData.size(); i++) {
            JsonArray ipMetricsArray = responseData.get(i).getAsJsonArray();
            String ipAddress = ipMetricsArray.get(1).getAsString();
            HashMap<String, Long> ipMetrics = new HashMap<String, Long>();
            for (int j = 2; j < ipMetricsArray.size(); j++) {
                ipMetrics.put(metricNames.get(j - 2), ipMetricsArray.get(j).getAsLong());
            }
            metrics.put(ipAddress, ipMetrics);
        }

        return metrics;
    }


    private String getObservationIds() throws Exception {
        HttpGet httpGet = new HttpGet(constructMetersURL());
        httpGet.addHeader(BasicScheme.authenticate(
                new UsernamePasswordCredentials(apiKey, ""),
                "UTF-8", false));


        System.out.println("executing request " + httpGet.getRequestLine());
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
        StringBuilder observationIds = new StringBuilder();

        for (int i = 0; i < responseArray.size(); i++) {
            JsonObject obj = responseArray.get(i).getAsJsonObject();
            observationIds.append(obj.get("obs_domain_id").getAsString());
            if (i < responseArray.size() - 1) {
                observationIds.append(",");
            }
        }
        return observationIds.toString();
    }

    /**
     * Populates the cluster metrics hashmap
     * @param   observationIds     A JsonObject containing metrics for the entire cluster
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

        System.out.println("executing request " + httpPost.getRequestLine());
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

    private String constructMetricsURL() {
        return new StringBuilder()
                .append("https://api.boundary.com/")
                .append(orgId)
                .append("/volume_1m_meter_ip/history")
                .toString();
    }
    private String constructMetersURL() {
        return new StringBuilder()
                .append("https://api.boundary.com/")
                .append(orgId)
                .append("/meters")
                .toString();
    }
}

