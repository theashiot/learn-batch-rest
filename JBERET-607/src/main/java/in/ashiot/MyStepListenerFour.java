package in.ashiot;

import org.jboss.logging.Logger;

import jakarta.batch.api.listener.StepListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Dependent
@Named("MyStepListenerFour")
public class MyStepListenerFour implements StepListener{

    @Inject
    private StepContext stepContext;

    @Inject
    private JobContext jobContext;

    private static final Logger LOGGER = Logger.getLogger(MyStepListenerFour.class);

    @Override
    public void beforeStep() throws Exception {
        final String stepName = stepContext.getStepName();
        LOGGER.infof("Starting the %s step",stepName);
        System.out.printf("Starting the %s step\n",stepName);
    }

    @Override
    public void afterStep() throws Exception {
        final String stepName = stepContext.getStepName();
        LOGGER.infof("Stopping the %s step",stepName);
        System.out.printf("Stopping the %s step\n",stepName);

        // Increment step counter in job context
        Object counterObj = jobContext.getTransientUserData();
        int currentCount = (counterObj != null) ? (Integer) counterObj : 0;
        int newCount = currentCount + 1;
        jobContext.setTransientUserData(newCount);
        LOGGER.infof("Step counter incremented to: %d", newCount);
        System.out.printf("Step counter incremented to: %d\n", newCount);
    }
}
