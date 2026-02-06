package dev.gmky.utils.logging.aop;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Aspect interface for log prefix functionality.
 * <p>
 * Defines the contract for the aspect that handles the {@link dev.gmky.utils.logging.annotation.LogPrefix} annotation.
 * </p>
 *
 * @author HiepVH
 * @see dev.gmky.utils.logging.annotation.LogPrefix
 * @since 1.0.0
 */
public interface LogPrefixAspect {
    /**
     * Around advice to add log prefix to MDC.
     *
     * @param joinPoint the proceeding join point
     * @return the result of the method execution
     * @throws Throwable if the intercepted method throws an exception
     */
    Object around(ProceedingJoinPoint joinPoint) throws Throwable;
}
