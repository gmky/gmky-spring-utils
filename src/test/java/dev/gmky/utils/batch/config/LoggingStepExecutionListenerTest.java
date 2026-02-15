package dev.gmky.utils.batch.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoggingStepExecutionListener.
 */
@ExtendWith(MockitoExtension.class)
class LoggingStepExecutionListenerTest {

    private LoggingStepExecutionListener listener;

    @Mock
    private StepExecution stepExecution;

    @BeforeEach
    void setUp() {
        listener = new LoggingStepExecutionListener();
    }

    @Test
    void testBeforeStep() {
        when(stepExecution.getStepName()).thenReturn("testStep");

        listener.beforeStep(stepExecution);

        verify(stepExecution).getStepName();
    }

    @Test
    void testAfterStepSuccess() {
        when(stepExecution.getStepName()).thenReturn("testStep");
        when(stepExecution.getReadCount()).thenReturn(100L);
        when(stepExecution.getFilterCount()).thenReturn(10L);
        when(stepExecution.getWriteCount()).thenReturn(90L);
        when(stepExecution.getSkipCount()).thenReturn(0L);
        when(stepExecution.getCommitCount()).thenReturn(10L);
        when(stepExecution.getFailureExceptions()).thenReturn(Collections.emptyList());
        when(stepExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);

        ExitStatus result = listener.afterStep(stepExecution);

        assertEquals(ExitStatus.COMPLETED, result);
        verify(stepExecution).getStepName();
        verify(stepExecution).getReadCount();
        verify(stepExecution).getFilterCount();
        verify(stepExecution).getWriteCount();
        verify(stepExecution).getSkipCount();
        verify(stepExecution).getCommitCount();
        verify(stepExecution, atLeast(1)).getFailureExceptions();
    }

    @Test
    void testAfterStepWithFailures() {
        when(stepExecution.getStepName()).thenReturn("testStep");
        when(stepExecution.getReadCount()).thenReturn(50L);
        when(stepExecution.getFilterCount()).thenReturn(5L);
        when(stepExecution.getWriteCount()).thenReturn(40L);
        when(stepExecution.getSkipCount()).thenReturn(5L);
        when(stepExecution.getCommitCount()).thenReturn(5L);
        when(stepExecution.getFailureExceptions()).thenReturn(
            Arrays.asList(new RuntimeException("Error 1"), new RuntimeException("Error 2"))
        );
        when(stepExecution.getExitStatus()).thenReturn(ExitStatus.FAILED);

        ExitStatus result = listener.afterStep(stepExecution);

        assertEquals(ExitStatus.FAILED, result);
        verify(stepExecution, atLeast(1)).getFailureExceptions();
    }

    @Test
    void testAfterStepWithNoFailures() {
        when(stepExecution.getStepName()).thenReturn("testStep");
        when(stepExecution.getReadCount()).thenReturn(100L);
        when(stepExecution.getFilterCount()).thenReturn(0L);
        when(stepExecution.getWriteCount()).thenReturn(100L);
        when(stepExecution.getSkipCount()).thenReturn(0L);
        when(stepExecution.getCommitCount()).thenReturn(10L);
        when(stepExecution.getFailureExceptions()).thenReturn(Collections.emptyList());
        when(stepExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);

        ExitStatus result = listener.afterStep(stepExecution);

        assertEquals(ExitStatus.COMPLETED, result);
        verify(stepExecution, atLeastOnce()).getFailureExceptions();
    }

    @Test
    void testAfterStepWithNullFailures() {
        when(stepExecution.getStepName()).thenReturn("testStep");
        when(stepExecution.getReadCount()).thenReturn(100L);
        when(stepExecution.getFilterCount()).thenReturn(0L);
        when(stepExecution.getWriteCount()).thenReturn(100L);
        when(stepExecution.getSkipCount()).thenReturn(0L);
        when(stepExecution.getCommitCount()).thenReturn(10L);
        when(stepExecution.getFailureExceptions()).thenReturn(null);
        when(stepExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);

        ExitStatus result = listener.afterStep(stepExecution);

        assertEquals(ExitStatus.COMPLETED, result);
    }

    @Test
    void testAfterStepReturnsOriginalExitStatus() {
        ExitStatus originalStatus = new ExitStatus("CUSTOM_STATUS");
        when(stepExecution.getStepName()).thenReturn("testStep");
        when(stepExecution.getReadCount()).thenReturn(100L);
        when(stepExecution.getFilterCount()).thenReturn(0L);
        when(stepExecution.getWriteCount()).thenReturn(100L);
        when(stepExecution.getSkipCount()).thenReturn(0L);
        when(stepExecution.getCommitCount()).thenReturn(10L);
        when(stepExecution.getFailureExceptions()).thenReturn(Collections.emptyList());
        when(stepExecution.getExitStatus()).thenReturn(originalStatus);

        ExitStatus result = listener.afterStep(stepExecution);

        assertEquals(originalStatus, result);
    }

    @Test
    void testAfterStepWithLargeCounts() {
        when(stepExecution.getStepName()).thenReturn("largeStep");
        when(stepExecution.getReadCount()).thenReturn(1_000_000L);
        when(stepExecution.getFilterCount()).thenReturn(100_000L);
        when(stepExecution.getWriteCount()).thenReturn(900_000L);
        when(stepExecution.getSkipCount()).thenReturn(50L);
        when(stepExecution.getCommitCount()).thenReturn(1000L);
        when(stepExecution.getFailureExceptions()).thenReturn(Collections.emptyList());
        when(stepExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);

        ExitStatus result = listener.afterStep(stepExecution);

        assertEquals(ExitStatus.COMPLETED, result);
        verify(stepExecution).getReadCount();
        verify(stepExecution).getWriteCount();
    }
}
