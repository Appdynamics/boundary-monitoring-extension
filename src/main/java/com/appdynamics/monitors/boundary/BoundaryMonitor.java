package com.appdynamics.monitors.boundary;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.*;

public class BoundaryMonitor extends AManagedMonitor {

    private static final String METRIC_PREFIX = "Custom Metrics|Boundary|Node IP|";
    private static final Logger logger = Logger.getLogger(BoundaryMonitor.class.getSimpleName());

    public static void main(String[] args) throws Exception{
		Map<String, String> taskArguments = new HashMap<String, String>();
        taskArguments.put("org-id", "");
        taskArguments.put("api-key", "");

        BoundaryMonitor boundaryMonitor = new BoundaryMonitor();
        boundaryMonitor.execute(taskArguments, null);
	}
    public BoundaryMonitor() {
        logger.setLevel(Level.INFO);
    }

    /**
     * Main execution method that uploads the metrics to the AppDynamics Controller
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        try {
            logger.info("Exceuting BoundaryMonitor...");
            BoundaryWrapper boundaryWrapper = new BoundaryWrapper(taskArguments);
            Map metrics = boundaryWrapper.gatherMetrics();
            logger.info("Gathered metrics successfully. Size of metrics: " + metrics.size());
            printMetrics(metrics);
            logger.info("Printed metrics successfully");
            return new TaskOutput("Task successful...");
        } catch (Exception e) {
            logger.error("Exception: ", e);
        }
        return new TaskOutput("Task failed with errors");
    }

    /**
     * Writes the Boundary metrics to the controller
     * @param 	metricsMap		HashMap containing all metrics for all ip addresses
     */
    private void printMetrics(Map metricsMap) throws Exception{
        Iterator ipIterator = metricsMap.entrySet().iterator();
        while (ipIterator.hasNext()) {
            Map.Entry<String, HashMap> mapEntry = (Map.Entry<String, HashMap>) ipIterator.next();
            String hostName = mapEntry.getKey();
            HashMap<String, Long> metrics = mapEntry.getValue();
            Iterator metricIterator = metrics.keySet().iterator();
            while (metricIterator.hasNext()) {
                String metricName = metricIterator.next().toString();
                Long metric = metrics.get(metricName);
                printMetric(METRIC_PREFIX + hostName + "|" + metricName, metric,
                        MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
            }
        }
    }

    /**
     * Returns the metric to the AppDynamics Controller.
     * @param 	metricName		Name of the Metric
     * @param 	metricValue		Value of the Metric
     * @param 	aggregation		Average OR Observation OR Sum
     * @param 	timeRollup		Average OR Current OR Sum
     * @param 	cluster			Collective OR Individual
     */
    private void printMetric(String metricName, Long metricValue, String aggregation, String timeRollup, String cluster) throws Exception
    {
        MetricWriter metricWriter = super.getMetricWriter(metricName,
                aggregation,
                timeRollup,
                cluster
        );
        metricWriter.printMetric(String.valueOf(metricValue));
    }
}