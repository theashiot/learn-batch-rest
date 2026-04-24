package in.ashiot;

import java.util.concurrent.TimeUnit;

import org.jberet.runtime.JobExecutionImpl;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.JobExecution;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {

        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Long executionId = jobOperator.start("simplejob",null);
        JobExecution jobExecutionId = jobOperator.getJobExecution(executionId);
        System.out.println("The job execution id is : "+jobExecutionId);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(executionId);
        try {
            jobExecution.awaitTermination(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
         
        System.out.printf("The job started at: %s\n",jobExecution.getStartTime().toString());
        System.out.printf("The job ended at: %s\n",jobExecution.getEndTime().toString());
        System.out.println("Execution time is: "+ (jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime()));
        
    }
}
