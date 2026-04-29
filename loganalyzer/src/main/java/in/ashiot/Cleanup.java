package in.ashiot;

import java.io.File;

import jakarta.batch.api.Batchlet;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;

public class Cleanup implements Batchlet{

    @Inject
    private JobContext jobContext;
    @Override
    public String process() throws Exception {
        String fileName = jobContext.getProperties().getProperty("output_file");
        System.out.println("Wrote " + (new File(fileName)).length() + " lins in the output file " + fileName);
        System.out.println("==============");
        System.out.println("Batch Job Complete");
        System.out.println("==============");
        return "COMPLETED";
    }

    @Override
    public void stop() throws Exception {
    }
    
}
