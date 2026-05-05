package in.ashiot;

import org.jboss.logging.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Dependent
@Named("MyWriter")
public class MyWriter implements jakarta.batch.api.chunk.ItemWriter {
     private static final Logger LOGGER = Logger.getLogger(MyWriter.class.getName());
    private BufferedWriter bwriter;
    @Inject
    private JobContext jobCtx;

    @Override
    public void open(Serializable ckpt) throws Exception {

        //Get the file name from the property defined in the job
        String fileName = jobCtx.getProperties()
                                .getProperty("output_file");
        
        /* 
            If there is no checkpoint, don't append: 
                FileWriter(fileName,false)
            Otherwise, append:
                FileWriter(fileName,true)
        */
        bwriter = new BufferedWriter(new FileWriter(fileName,
                                                    (ckpt != null)));
    }

    @Override
    public void writeItems(List<Object> items) throws Exception {
        for (int i = 0; i < items.size(); i++) {
            String line = (String) items.get(i);
            bwriter.write(line);
            bwriter.newLine();
        }
        LOGGER.infof("Wrote  %d items", items.size());
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return new MyCheckpoint();
    }

    @Override
    public void close() {
        try {
         bwriter.close();
        } catch (IOException e) {
        }
    }
}
