package in.ashiot;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.FileReader;

import jakarta.batch.api.chunk.ItemReader;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Dependent
@Named("LogItemReader")
public class LogItemReader implements ItemReader {
    private LogCheckPoint logCheckPoint;
    private BufferedReader bufferedReader;
    @Inject
    JobContext jobContext;

    public void open (Serializable checkpoint) throws Exception{
        if (checkpoint == null) {
            this.logCheckPoint = new LogCheckPoint();
            System.out.println("MESSAGE FROM THE APP: No checkpoint exists, creating one now.");
        }
        else {
            this.logCheckPoint = (LogCheckPoint) checkpoint;
        }

        String fileName = jobContext.getProperties().getProperty("input_file");
        bufferedReader = new BufferedReader(new FileReader(fileName));
        long lineNo = logCheckPoint.getLineNumber();
        System.out.println("MESSAGE FROM THE APP: Reading from line no. " + lineNo);
        for(long i=0; i<lineNo; i++) {
            bufferedReader.readLine();
        }
    }

    public Object readItem() throws Exception {
        String line = bufferedReader.readLine();
        return line;
    }

    @Override
    public void close() throws Exception {
        bufferedReader.close();
        System.out.println("MESSAGE FROM APP: Buffered reader closed."); 
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        System.out.println("MESSAGE FROM APP: checkPointInfo encountered."); 
        return this.logCheckPoint;
    }
}