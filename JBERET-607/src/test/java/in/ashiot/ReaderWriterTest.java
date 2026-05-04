package in.ashiot;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.StepExecution;

public class ReaderWriterTest {
    static final String XML_NAME = "simplejob";
    private final String COMPLETED_BATCH_STATUS = "COMPLETED";

    private JobOperator jobOperator;
    private Properties params;

    @BeforeEach
    public void setUp() {
        jobOperator = BatchRuntime.getJobOperator();
        params = new Properties();
        params.setProperty("input_file", "input.txt");
        params.setProperty("output_file", "output.txt");
        params.setProperty("item.count", "10");
        params.setProperty("time.limit", "2");
        params.setProperty("restartable", "true");
    }

    @Test
    public void validateExitStatus() throws Exception {
        params.setProperty("fail.after.seconds", "1000");
        final long jobId = jobOperator.start(XML_NAME, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobId);
        jobExecution.awaitTermination(0, TimeUnit.SECONDS);
        Assertions.assertEquals(COMPLETED_BATCH_STATUS, jobExecution.getBatchStatus().toString());
    }

    @Test
    public void testJobFailureAndRestart() throws Exception {
        // Create properties that will cause the job to fail after 10 seconds
        Properties failParams = new Properties();
        failParams.setProperty("input_file", "input.txt");
        failParams.setProperty("output_file", "output.txt");
        failParams.setProperty("item.count", "100");
        failParams.setProperty("time.limit", "100");
        failParams.setProperty("restartable", "true");
        failParams.setProperty("fail.after.seconds", "1");

        // Start the job that will fail
        final long jobId = jobOperator.start(XML_NAME, failParams);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobId);
        jobExecution.awaitTermination(0, TimeUnit.SECONDS);

        // Verify the job failed
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());
        System.out.println("Job failed as expected with job ID: " + jobId);

        // Restart the failed job
        final long restartExecutionId = jobOperator.restart(jobExecution.getExecutionId(), failParams);
        final JobExecutionImpl restartedExecution = (JobExecutionImpl) jobOperator.getJobExecution(restartExecutionId);

        // Verify that the restarted execution belongs to the same job instance
        Assertions.assertEquals(jobId, restartedExecution.getJobInstance().getInstanceId());
        System.out.println("Restarted job execution belongs to same job ID: " + restartedExecution.getJobInstance().getInstanceId());
    }

    @Test
    public void testJobRestartAtExpectedStep() throws Exception {
        // Create properties that will cause the job to fail after 10 seconds
        Properties failParams = new Properties();
        failParams.setProperty("input_file", "input.txt");
        failParams.setProperty("output_file", "output.txt");
        failParams.setProperty("item.count", "100");
        failParams.setProperty("time.limit", "100");
        failParams.setProperty("restartable", "true");
        failParams.setProperty("fail.after.seconds", "8");

        // Start the job that will fail
        final long jobId = jobOperator.start(XML_NAME, failParams);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobId);
        jobExecution.awaitTermination(0, TimeUnit.SECONDS);

        // Verify the job failed
        Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        // Get the step executions to find which step failed and get its persistent data
        java.util.List<StepExecution> stepExecutions = jobOperator.getStepExecutions(jobExecution.getExecutionId());
        String failedStepId = null;
        Object failedStepPersistentData = null;
        for (StepExecution stepExec : stepExecutions) {
            if (stepExec.getBatchStatus() == BatchStatus.FAILED) {
                failedStepId = stepExec.getStepName();
                failedStepPersistentData = stepExec.getPersistentUserData();
                System.out.println("Job failed at step: " + failedStepId);
                System.out.println("Failed step persistent data: " + failedStepPersistentData);
                break;
            }
        }
        Assertions.assertNotNull(failedStepId, "Failed step should be found");
        Assertions.assertNotNull(failedStepPersistentData, "Failed step should have persistent data");

        // Restart the failed job without the fail parameter so it completes
        Properties restartParams = new Properties();
        restartParams.setProperty("input_file", "input.txt");
        restartParams.setProperty("output_file", "output.txt");
        restartParams.setProperty("item.count", "100");
        restartParams.setProperty("time.limit", "100");
        restartParams.setProperty("restartable", "true");

        final long restartExecutionId = jobOperator.restart(jobExecution.getExecutionId(), restartParams);
        final JobExecutionImpl restartedExecution = (JobExecutionImpl) jobOperator.getJobExecution(restartExecutionId);
        restartedExecution.awaitTermination(0, TimeUnit.SECONDS);

        // Get step executions from the restarted job
        java.util.List<StepExecution> restartedStepExecutions = jobOperator.getStepExecutions(restartExecutionId);

        // The first step execution in the restart should be the failed step
        Assertions.assertFalse(restartedStepExecutions.isEmpty(), "Restarted job should have step executions");
        String firstRestartedStepId = restartedStepExecutions.get(0).getStepName();
        Object restartedStepPersistentData = restartedStepExecutions.get(0).getPersistentUserData();

        // Verify that the restart began at the step that failed
        Assertions.assertEquals(failedStepId, firstRestartedStepId);
        System.out.println("Restarted job started at expected step: " + firstRestartedStepId);

        // Verify that the persistent data from the step listener is preserved across restart
        Assertions.assertEquals(failedStepPersistentData, restartedStepPersistentData);
        System.out.println("Restarted step persistent data matches failed step: " + restartedStepPersistentData);
    }

    /*
    @Test
    public void testStepCounterEqualsNumberOfSteps() throws Exception {
        // Expected number of steps in simplejob.xml
        final int EXPECTED_STEP_COUNT = 6; // mychunk1, mychunk2, mychunk3, mychunk4, mychunk5, mytask

        // Start the job and wait for completion
        params.setProperty("fail.after.seconds", "1000"); // Set high value to avoid failure
        final long jobId = jobOperator.start(XML_NAME, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobId);
        jobExecution.awaitTermination(0, TimeUnit.SECONDS);

        // Verify the job completed successfully
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        System.out.println("Job completed successfully with job ID: " + jobId);

        // Get the step counter from job execution transient user data
        Object counterObj = jobExecution.
        Assertions.assertNotNull(counterObj, "Step counter should be stored in job transient user data");
        int actualStepCount = (Integer) counterObj;

        // Verify that the counter equals the expected number of steps
        Assertions.assertEquals(EXPECTED_STEP_COUNT, actualStepCount);
        System.out.printf("Step counter verification passed: Expected=%d, Actual=%d\n", EXPECTED_STEP_COUNT, actualStepCount);
    }
        */

}
