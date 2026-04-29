package in.ashiot;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.jberet.runtime.JobExecutionImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
    }

    @Test
    public void validateExitStatus() throws Exception {
        final long jobId = jobOperator.start(XML_NAME, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobId);
        jobExecution.awaitTermination(0, TimeUnit.SECONDS);
        Assertions.assertEquals(COMPLETED_BATCH_STATUS, jobExecution.getBatchStatus().toString());
    }

    @Test
    public void validateStepID() throws Exception {
        
    } 

    /*
    private void validate(final BatchStatus expectedBatchStatus, final int expectedReaderCount, final int expectedWriterCount) throws Exception {
        final long jobId = jobOperator.start(XML_NAME, params);
        final JobExecutionImpl jobExecution = (JobExecutionImpl) jobOperator.getJobExecution(jobId);
        jobExecution.awaitTermination(10, TimeUnit.SECONDS);
        Assertions.assertEquals(expectedBatchStatus, jobExecution.getBatchStatus());

        final StepExecution step0 = jobExecution.getStepExecutions().get(0);
        //final ReaderWriterResult item = ReaderWriterResult.class.cast(step0.getPersistentUserData());
        //Assertions.assertTrue(item.isReaderClosed(), "Reader was not closed");
        //Assertions.assertTrue(item.isWriterClosed(), "Writer was not closed");
        //Assertions.assertEquals(expectedReaderCount, item.getReadCount(), "Unexpected reader count.");
        //Assertions.assertEquals(expectedWriterCount, item.getWriteCount(), "Unexpected writer count.");
    }
        */
}
