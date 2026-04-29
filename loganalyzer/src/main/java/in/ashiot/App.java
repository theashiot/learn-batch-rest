package in.ashiot;

import java.util.concurrent.TimeUnit;
import org.jberet.runtime.JobExecutionImpl;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.JobExecution;

public class App {
    public static void main(String[] args) {
        System.out.println("Hello World!");

        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Long jobExecutionId = jobOperator.start("loganalysis",null);
//        JobExecution jobExecution = jobOperator.getJobExecution(jobExecutionId);
        final JobExecutionImpl jobExecutionImpl = (JobExecutionImpl) jobOperator.getJobExecution(jobExecutionId);
        System.out.println("===================");
        System.out.println("Starting Job with the ID: " + jobExecutionId);
        System.out.println("===================");

        try {
            jobExecutionImpl.awaitTermination(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }      
        

    }
}
