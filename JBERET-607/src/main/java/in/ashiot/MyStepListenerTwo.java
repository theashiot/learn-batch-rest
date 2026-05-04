package in.ashiot;

import org.jboss.logging.Logger;

import jakarta.batch.api.listener.StepListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Dependent
@Named("MyStepListenerTwo")
public class MyStepListenerTwo implements StepListener{


    @Inject
    private StepContext stepContext;

    @Inject
    private JobContext jobContext;

    private static final Logger LOGGER = Logger.getLogger(MyStepListenerTwo.class);


    @Override
    public void beforeStep() throws Exception {
        final String stepName = stepContext.getStepName();
        final long stepExecutionId = stepContext.getStepExecutionId();
        LOGGER.infof("Starting the %s step, with the id %d",stepName, stepExecutionId);
        System.out.printf("Starting the %s step with the id %d%%n",stepName, stepExecutionId);

        // Store persistent data that will survive restart
        Object persistentData = stepContext.getPersistentUserData();
        if (persistentData == null) {
            // First time running this step - store initial timestamp
            Long startTimestamp = System.currentTimeMillis();
            stepContext.setPersistentUserData(startTimestamp);
            LOGGER.infof("Stored initial persistent data: %d", startTimestamp);
            System.out.printf("Stored initial persistent data: %d\n", startTimestamp);
        } else {
            // Step is restarting - log the preserved data
            LOGGER.infof("Found existing persistent data from previous execution: %s", persistentData);
            System.out.printf("Found existing persistent data from previous execution: %s\n", persistentData);
        }
    }
    

    @Override
    public void afterStep() throws Exception {
        final String stepName = stepContext.getStepName();
        final long stepExecutionId = stepContext.getStepExecutionId();
        LOGGER.infof("Stopping the %s step, with the id %d",stepName, stepExecutionId);
        System.out.printf("Stopping the %s step with the id %d%n",stepName, stepExecutionId);

        // Increment step counter in job context
        Object counterObj = jobContext.getTransientUserData();
        int currentCount = (counterObj != null) ? (Integer) counterObj : 0;
        int newCount = currentCount + 1;
        jobContext.setTransientUserData(newCount);
        LOGGER.infof("Step counter incremented to: %d", newCount);
        System.out.printf("Step counter incremented to: %d\n", newCount);
    }
    
}
