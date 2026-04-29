package in.ashiot;

import org.jboss.logging.Logger;

import jakarta.batch.api.listener.StepListener;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Dependent
@Named("MyStepListener")
public class MyStepListener implements StepListener{

    @Inject
    private StepContext stepContext;

    private static final Logger LOGGER = Logger.getLogger(MyStepListener.class);


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

    }
    
}
