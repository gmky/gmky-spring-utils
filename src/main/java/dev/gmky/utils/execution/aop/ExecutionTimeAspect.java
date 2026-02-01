package dev.gmky.utils.execution.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;

/**
 * Aspect interface for execution time monitoring.
 * <p>
 * Defines the contract for the aspect that handles the {@link dev.gmky.utils.execution.annotation.ExecutionTime} annotation.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.0
 * @see dev.gmky.utils.execution.annotation.ExecutionTime
 */
public interface ExecutionTimeAspect {
    
    /**
     * Around advice to intercept and measure method execution time.
     *
     * @param joinPoint the proceeding join point
     * @return the result of the method execution
     * @throws Throwable if the intercepted method throws an exception
     */
    @Around("@annotation(dev.gmky.utils.execution.annotation.ExecutionTime)")
    Object around(ProceedingJoinPoint joinPoint) throws Throwable;
}
