package dev.gmky.utils.batch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Step execution listener that logs step execution statistics.
 *
 * @author HiepVH
 * @since 1.0.3
 */
@Slf4j
public class LoggingStepExecutionListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Starting step: {}", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("Step completed: {} - Read: {}, Filtered: {}, Written: {}, Skipped: {}, Commit: {}",
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                stepExecution.getFilterCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                stepExecution.getCommitCount());

        if (stepExecution.getFailureExceptions() != null && !stepExecution.getFailureExceptions().isEmpty()) {
            log.error("Step failed with {} exceptions", stepExecution.getFailureExceptions().size());
        }

        return stepExecution.getExitStatus();
    }
}
