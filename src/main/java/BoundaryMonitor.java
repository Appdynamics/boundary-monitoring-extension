import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class BoundaryMonitor extends AManagedMonitor {

    private static final String METRIC_PREFIX = "Custom Metrics|Boundary|IP|";
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

    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {

        try {
            logger.info("Exceuting BoundaryMonitor...");
            BoundaryWrapper boundaryWrapper = new BoundaryWrapper(taskArguments);
            Map metrics = boundaryWrapper.gatherMetrics();
            logger.info("Gathered metrics successfully. Size of metrics: " + metrics.size());
            //printMetrics(metrics);
            logger.info("Printed metrics successfully");
            return new TaskOutput("Task successful...");
        } catch (Exception e) {
            logger.error("Exception: ", e);
        }
        return new TaskOutput("Task failed with errors");
    }


}