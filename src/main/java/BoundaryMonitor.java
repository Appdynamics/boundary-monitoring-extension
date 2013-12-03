import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

import java.util.HashMap;
import java.util.Map;

public class BoundaryMonitor extends AManagedMonitor {

    private Map cachedMetrics;

    public static void main(String[] args) throws Exception{
		Map<String, String> taskArguments = new HashMap<String, String>();
        taskArguments.put("org-id", "L3gI1Wuw6qDgikcRH01Vhe7GtY9");
        taskArguments.put("api-key", "26pIU77FtyZxx1ZPXi75G1tmyg6");

        BoundaryMonitor boundaryMonitor = new BoundaryMonitor();
        boundaryMonitor.execute(taskArguments, null);
	}

    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        BoundaryWrapper boundaryWrapper = new BoundaryWrapper(taskArguments, cachedMetrics);
        try {
            Map map = boundaryWrapper.gatherMetrics();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void cacheMetrics(Map metrics) {
        this.cachedMetrics = metrics;
    }
    public Map getCachedMetrics() {
        return this.cachedMetrics;
    }
}