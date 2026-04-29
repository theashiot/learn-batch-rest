package in.ashiot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;

import jakarta.batch.api.chunk.ItemReader;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Dependent
@Named("MyReader")
public class MyReader implements ItemReader{
    private MyCheckpoint checkpoint;
    private BufferedReader breader;
    @Inject
    JobContext jobCtx;

    @Inject
    private StepContext stepContext;
    
    public MyReader() {}

    @Override
    public void open(Serializable ckpt) throws Exception {

        /*
            If a checkpoint doesn't already exist, create a checkpoint.
            Otherwise, get the value of the exiting checkpoint.
        */

        if (ckpt == null)
            checkpoint = new MyCheckpoint();
        else
            checkpoint = (MyCheckpoint) ckpt;

        //Get file name from the property defined in the job
        String fileName = jobCtx.getProperties()
                                .getProperty("input_file");

        breader = new BufferedReader(new FileReader(fileName));

        /* Check the line number at which the job was restarted
           Re-read those lines
        */
        for (long i = 0; i < checkpoint.getLineNum(); i++)
            breader.readLine();
    }

    @Override
    public void close() throws Exception {
        breader.close();
    }

    @Override
    public Object readItem() throws Exception {
        String line = breader.readLine();
        return line;
    }

    @Override
    public Serializable checkpointInfo() {
        return checkpoint;
    }    

}
