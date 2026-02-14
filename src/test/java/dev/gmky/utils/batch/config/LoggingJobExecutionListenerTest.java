package dev.gmky.utils.batch.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.*;

/**
 * Unit tests for LoggingJobExecutionListener.
 */
@ExtendWith(MockitoExtension.class)
class LoggingJobExecutionListenerTest {

    private LoggingJobExecutionListener listener;

    @Mock
    private JobExecution jobExecution;

    @Mock
    private JobInstance jobInstance;

    @BeforeEach
    void setUp() {
        listener = new LoggingJobExecutionListener();
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("testJob");
    }

    @Test
    void testBeforeJob() {
        JobParameters params = new JobParameters();
        when(jobExecution.getJobParameters()).thenReturn(params);

        listener.beforeJob(jobExecution);

        verify(jobExecution).getJobInstance();
        verify(jobExecution).getJobParameters();
    }

    @Test
    void testAfterJobCompleted() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));
        when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());

        listener.afterJob(jobExecution);

        verify(jobExecution).getStatus();
        verify(jobExecution, times(2)).getStartTime();
        verify(jobExecution, times(2)).getEndTime();
    }

    @Test
    void testAfterJobFailed() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));
        when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
        when(jobExecution.getAllFailureExceptions()).thenReturn(Collections.emptyList());

        listener.afterJob(jobExecution);

        verify(jobExecution).getStatus();
        verify(jobExecution).getAllFailureExceptions();
    }

    @Test
    void testAfterJobWithOtherStatus() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.STOPPED);
        when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));
        when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());

        listener.afterJob(jobExecution);

        verify(jobExecution, atLeastOnce()).getStatus();
    }

    @Test
    void testAfterJobWithNullStartTime() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStartTime()).thenReturn(null);
        when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());

        listener.afterJob(jobExecution);

        verify(jobExecution).getStatus();
    }

    @Test
    void testAfterJobWithNullEndTime() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));
        when(jobExecution.getEndTime()).thenReturn(null);

        listener.afterJob(jobExecution);

        verify(jobExecution).getStatus();
    }

    @Test
    void testAfterJobWithBothTimesNull() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStartTime()).thenReturn(null);
        when(jobExecution.getEndTime()).thenReturn(null);

        listener.afterJob(jobExecution);

        verify(jobExecution).getStatus();
    }
}
