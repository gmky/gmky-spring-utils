package dev.gmky.utils.execution.aop;

import dev.gmky.utils.execution.annotation.ExecutionTime;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;

/**
 * Aspect for monitoring method execution time.
 * <p>
 * This aspect intercepts methods annotated with {@link ExecutionTime} and
 * measures their execution duration. The execution time is logged at DEBUG
 * level for successful executions and INFO level for failed executions.
 * </p>
 * <p>
 * The aspect uses Spring's {@link StopWatch} for accurate time measurement
 * and only performs timing when DEBUG logging is enabled to minimize
 * performance impact.
 * </p>
 *
 * <h3>Configuration:</h3>
 * <p>
 * This aspect is automatically registered as a Spring bean via {@code @Component}.
 * Ensure that AspectJ auto-proxying is enabled in your Spring configuration:
 * </p>
 * <pre>{@code
 * @EnableAspectJAutoProxy
 * @Configuration
 * public class AopConfig {
 *     // configuration
 * }
 * }</pre>
 *
 * <h3>Log Format:</h3>
 * <ul>
 *   <li>Success: {@code Method [methodName] - [key] executed in X ms}</li>
 *   <li>Failure: {@code Method [methodName] - [key] failed in X ms}</li>
 * </ul>
 *
 * @author HiepVH
 * @since 1.0.0
 * @see ExecutionTime
 */
@Slf4j
@Aspect
@ConditionalOnMissingBean(ExecutionTimeAspect.class)
public class ExecutionTimeAspectImpl implements ExecutionTimeAspect {
    
    /**
     * Around advice that measures and logs method execution time.
     * <p>
     * This method intercepts all methods annotated with {@link ExecutionTime}.
     * It measures the execution time using a {@link StopWatch} and logs the
     * duration. If the method throws an exception, the execution time is still
     * logged before re-throwing the exception.
     * </p>
     * <p>
     * The timing logic only executes when DEBUG logging is enabled to avoid
     * unnecessary overhead in production environments.
     * </p>
     *
     * @param joinPoint the proceeding join point representing the intercepted method
     * @return the result of the intercepted method execution
     * @throws Throwable if the intercepted method throws an exception
     */
    @Override
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        if (log.isDebugEnabled()) {
            var annotation = getAnnotation(joinPoint);
            var name = getName(joinPoint, annotation);
            var key = getKey(annotation);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            try {
                var result = joinPoint.proceed();
                stopWatch.stop();
                log.debug("Method [{}] - [{}] executed in {} ms", name, key, stopWatch.getTotalTimeMillis());
                return result;
            } catch (Throwable throwable) {
                stopWatch.stop();
                log.info("Method [{}] - [{}] failed in {} ms", name, key, stopWatch.getTotalTimeMillis());
                throw throwable;
            }
        }
        return joinPoint.proceed();
    }

    /**
     * Extracts the key from the {@link ExecutionTime} annotation.
     * <p>
     * The key provides additional context for the log message, such as
     * transaction IDs or request identifiers.
     * </p>
     *
     * @param annotation the ExecutionTime annotation instance
     * @return the key value from the annotation, or empty string if not specified
     */
    private static String getKey(ExecutionTime annotation) {
        return annotation.key();
    }

    /**
     * Determines the name to use in the log message.
     * <p>
     * If a custom name is specified in the annotation (via {@code value} or
     * {@code name} attribute), it is used. Otherwise, the actual method name
     * is used as a fallback.
     * </p>
     *
     * @param joinPoint  the proceeding join point
     * @param annotation the ExecutionTime annotation instance
     * @return the name to use in log messages
     */
    private static String getName(ProceedingJoinPoint joinPoint, ExecutionTime annotation) {
        return StringUtils.isNotBlank(annotation.value()) ? annotation.value() : joinPoint.getSignature().getName();
    }

    /**
     * Retrieves the {@link ExecutionTime} annotation from the intercepted method.
     * <p>
     * This method extracts the annotation metadata from the method signature
     * of the join point.
     * </p>
     *
     * @param joinPoint the proceeding join point
     * @return the ExecutionTime annotation instance, never null
     * @throws NullPointerException if the annotation is not present (should not happen
     *                              as this method is only called when the annotation is present)
     */
    @NonNull
    private static ExecutionTime getAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getAnnotation(ExecutionTime.class);
    }
}
