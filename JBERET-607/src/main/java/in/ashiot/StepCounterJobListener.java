package in.ashiot;

import org.jboss.logging.Logger;

import jakarta.batch.api.listener.JobListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Dependent
@Named("StepCounterJobListener")
public class StepCounterJobListener implements JobListener {

    @Inject
    private JobContext jobContext;

    private static final Logger LOGGER = Logger.getLogger(StepCounterJobListener.class);
    private static final String STEP_COUNTER_KEY = "stepCounter";

    @Override
    public void beforeJob() throws Exception {
        // Initialize the step counter to 0
        jobContext.setTransientUserData(0);
        LOGGER.infof("Initialized step counter to 0");
        System.out.println("Initialized step counter to 0");
    }

    @Override
    public void afterJob() throws Exception {
        Object counterObj = jobContext.getTransientUserData();
        int finalCount = (counterObj != null) ? (Integer) counterObj : 0;
        LOGGER.infof("Job completed. Total steps executed: %d", finalCount);
        System.out.printf("Job completed. Total steps executed: %d\n", finalCount);
    }
}
