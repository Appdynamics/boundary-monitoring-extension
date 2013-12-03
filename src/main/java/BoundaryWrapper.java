import com.google.gson.*;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
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
    private Map cachedMetrics;

    // Should this be singleton??

    public BoundaryWrapper(Map<String, String> taskArguments, Map cachedMetrics) {
        this.apiKey = taskArguments.get("api-key");
        this.orgId = taskArguments.get("org-id");
        this.cachedMetrics = cachedMetrics;
    }

    /**
     * Connects to the couchbase cluster host and retrieves metrics using the CouchBase REST API
     * @return 	HashMap     Map containing metrics retrieved from using the CouchBase REST API
     */
    public HashMap gatherMetrics() throws Exception{
        HttpURLConnection connection = null;
        InputStream is = null;
        String metricsURL = constructMetricsURL();

        try {
            HttpPost httpPost = new HttpPost(metricsURL);
            httpPost.addHeader(BasicScheme.authenticate(
                    new UsernamePasswordCredentials(apiKey, ""),
                    "UTF-8", false));

            // Request parameters and other properties.
            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("aggregations", "observationDomainId"));
            params.add(new BasicNameValuePair("observationDomainIds", "6,7"));
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
            StringBuilder jsonString2 = new StringBuilder();
            String line = "";
            while ((line = bufferedReader2.readLine()) != null) {
                jsonString2.append(line);
            }

            System.out.println("Done gathering metrics" + jsonString2.toString());
            return new HashMap();
        } catch(MalformedURLException e) {
            logger.error("Invalid URL used to connect to CouchDB: " + metricsURL);
            throw e;
        } catch(JsonSyntaxException e) {
            logger.error("Error parsing the Json response");
            throw e;
        } catch(IOException e) {
            throw e;
        } finally {
            try {
                if (is != null && connection != null) {
                    is.close();
                    connection.disconnect();
                }
            }catch(Exception e) {
                logger.error("Exception", e);
            }
        }
    }

    /**
     * Populates the cluster metrics hashmap
     * @param   clusterStats     A JsonObject containing metrics for the entire cluster
     * @param   clusterMetrics   An initially empty map that is populated based on values retrieved from traversing the clusterStats JsonObject
     */
    private void populateClusterMetrics(JsonObject clusterStats, HashMap clusterMetrics) throws Exception{
        JsonObject ramStats = clusterStats.getAsJsonObject("ram");
        Iterator iterator = ramStats.entrySet().iterator();
        populateMetricsMapHelper(iterator, clusterMetrics, "ram_");

        JsonObject hddStats = clusterStats.getAsJsonObject("hdd");
        iterator = hddStats.entrySet().iterator();
        populateMetricsMapHelper(iterator, clusterMetrics, "hdd_");
    }

    /**
     * Populates the node metrics hashmap
     * @param   nodes           A JsonArray containing metrics for all nodes
     * @param   nodeMetrics     An initially empty map that is populated based on values retrieved from traversing the nodes JsonArray
     */
    private void populateNodeMetrics(JsonArray nodes, HashMap<String, HashMap<String, Number>> nodeMetrics) {
        for (JsonElement node : nodes) {
            JsonObject nodeObject = node.getAsJsonObject();
            HashMap<String, Number> metrics = new HashMap<String, Number>();
            nodeMetrics.put(nodeObject.get("hostname").getAsString(), metrics);

            JsonObject interestingStats = nodeObject.getAsJsonObject("interestingStats");
            Iterator iterator = interestingStats.entrySet().iterator();
            populateMetricsMapHelper(iterator, metrics, "");

            JsonObject systemStats = nodeObject.getAsJsonObject("systemStats");
            iterator = systemStats.entrySet().iterator();
            populateMetricsMapHelper(iterator, metrics, "");

            iterator = nodeObject.entrySet().iterator();
            populateMetricsMapHelper(iterator, metrics, "");
        }
    }

    /**
     * Populates the bucket metrics hashmap
     * @param   buckets        A JsonArray containing metrics for all buckets
     * @param   bucketMetrics  An initially empty map that is populated based on values retrieved from traversing the buckets JsonArray
     */
    private void populateBucketMetrics(JsonArray buckets, HashMap<String, HashMap<String, Number>> bucketMetrics) {
        for (JsonElement bucket : buckets) {
            JsonObject bucketObject = bucket.getAsJsonObject();
            HashMap<String, Number> metrics = new HashMap<String, Number>();
            bucketMetrics.put(bucketObject.get("name").getAsString(), metrics);

            JsonObject interestingStats = bucketObject.getAsJsonObject("quota");
            Iterator iterator = interestingStats.entrySet().iterator();
            populateMetricsMapHelper(iterator, metrics, "");

            JsonObject systemStats = bucketObject.getAsJsonObject("basicStats");
            iterator = systemStats.entrySet().iterator();
            populateMetricsMapHelper(iterator, metrics, "");
        }
    }

    /**
     * Populates an empty map with values retrieved from the entry set of a Json Object
     * @param   iterator    An entry set iterator for the json object
     * @param   metricsMap  Initially empty map that is populated based on the values retrieved from entry set
     * @param   prefix      Optional prefix for the metric name to distinguish duplicate metric names
     */
    private void populateMetricsMapHelper(Iterator iterator, HashMap metricsMap, String prefix) {
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            String metricName = (String)entry.getKey();
            JsonElement value = (JsonElement)entry.getValue();
            if (value instanceof JsonPrimitive && NumberUtils.isNumber(value.getAsString())) {
                Number val = value.getAsNumber();
                metricsMap.put(prefix + metricName, val);
            }
        }
    }


    private String constructMetricsURL() {
        return new StringBuilder()
                .append("https://api.boundary.com/")
                .append(orgId)
                .append("/volume_1m_meter/history")
                .toString();
    }
}

