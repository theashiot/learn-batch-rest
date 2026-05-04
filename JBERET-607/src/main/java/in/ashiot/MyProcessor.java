package in.ashiot;

import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Dependent
@Named("MyProcessor")
public class MyProcessor implements ItemProcessor {
    @Inject
    private JobContext jobCtx;
    private long startTime = 0;

    public MyProcessor() {}

    @Override
    public Object processItem(Object obj) throws Exception {
        // Initialize start time on first item
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }

        // Check if we should fail after a certain time
        String failAfter = jobCtx.getProperties().getProperty("fail.after.seconds");
        if (failAfter != null && !failAfter.isEmpty()) {
            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            int failAfterSeconds = Integer.parseInt(failAfter);
            if (elapsedSeconds >= failAfterSeconds) {
                throw new RuntimeException("Intentional failure after " + failAfterSeconds + " seconds for testing");
            }
        }

        String line = (String) obj;
        //Simulate processing delay
        Thread.sleep(100);
        return line.toUpperCase();
    }
}
