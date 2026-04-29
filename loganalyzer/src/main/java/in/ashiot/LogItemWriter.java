package in.ashiot;

import java.io.Serializable;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;

import jakarta.batch.api.chunk.ItemWriter;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Dependent
@Named("LogItemWriter")
public class LogItemWriter implements ItemWriter {

    @Inject
    JobContext jobContext;

    private BufferedWriter bufferedWriter;
    LogCheckPoint checkpoint;

    @Override
    public void open(Serializable checkpoint) throws Exception {
        this.checkpoint = (LogCheckPoint) checkpoint;
        bufferedWriter = new BufferedWriter(new FileWriter(jobContext.getProperties().getProperty("output_file"),(checkpoint != null)));
    }

    @Override
    public void close() throws Exception {
        bufferedWriter.close();
        System.out.println("MESSAGE FROM APP: Buffered writer closed."); 

    }

    @Override
    public void writeItems(List<Object> items) throws Exception {
        for (int i=0; i<items.size(); i++) {
            String line = (String) items.get(i);
            System.out.println("MESSAGE FROM APP: Wrote line number " + i);
            bufferedWriter.write(line);
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return checkpoint;
    }
    
}
