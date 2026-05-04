package in.ashiot;

import java.io.File;

import jakarta.batch.api.Batchlet;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Dependent
@Named("MyBatchlet")
public class MyBatchlet implements Batchlet {
    @Inject
    private JobContext jobCtx;

    //Print the length of the output file
    @Override
    public String process() throws Exception {
        String fileName = jobCtx.getProperties()
                                .getProperty("output_file");
        System.out.println(""+(new File(fileName)).length());
        return "COMPLETED";
    }

    @Override
    public void stop() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stop'");
    }
}
