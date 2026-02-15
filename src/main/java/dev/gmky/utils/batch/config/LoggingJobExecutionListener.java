package dev.gmky.utils.batch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Job execution listener that logs job start, completion, and statistics.
 *
 * @author HiepVH
 * @since 1.0.3
 */
@Slf4j
public class LoggingJobExecutionListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Starting batch job: {} with parameters: {}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        LocalDateTime startTime = jobExecution.getStartTime() != null
            ? jobExecution.getStartTime()
            : LocalDateTime.now();
        LocalDateTime endTime = jobExecution.getEndTime() != null
            ? jobExecution.getEndTime()
            : LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);

        BatchStatus status = jobExecution.getStatus();

        if (status == BatchStatus.COMPLETED) {
            log.info("Batch job completed successfully: {} - Duration: {}s",
                    jobExecution.getJobInstance().getJobName(),
                    duration.getSeconds());
        } else if (status == BatchStatus.FAILED) {
            log.error("Batch job failed: {} - Duration: {}s - Failures: {}",
                    jobExecution.getJobInstance().getJobName(),
                    duration.getSeconds(),
                    jobExecution.getAllFailureExceptions());
        } else {
            log.info("Batch job finished with status {}: {} - Duration: {}s",
                    status,
                    jobExecution.getJobInstance().getJobName(),
                    duration.getSeconds());
        }
    }
}
