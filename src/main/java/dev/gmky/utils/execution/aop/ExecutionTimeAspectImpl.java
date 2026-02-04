package dev.gmky.utils.execution.aop;

import dev.gmky.utils.execution.annotation.ExecutionTime;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
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
@Component
public class ExecutionTimeAspectImpl implements ExecutionTimeAspect {

    // SpEL Parser
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

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
    @Around("@annotation(dev.gmky.utils.execution.annotation.ExecutionTime)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        if (log.isDebugEnabled()) {
            var annotation = getAnnotation(joinPoint);
            var name = getName(joinPoint, annotation);
            var key = getKey(joinPoint, annotation);
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
     * Extracts the key from the {@link ExecutionTime} annotation, supporting SpEL expressions.
     * <p>
     * The key is evaluated against the method arguments.
     * </p>
     *
     * @param joinPoint the proceeding join point
     * @param annotation the ExecutionTime annotation instance
     * @return the evaluated key value
     */
    private String getKey(ProceedingJoinPoint joinPoint, ExecutionTime annotation) {
        String keyExpression = annotation.key();
        if (StringUtils.isBlank(keyExpression)) {
            return "";
        }

        try {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();
            Object[] args = joinPoint.getArgs();

            EvaluationContext context = new StandardEvaluationContext();
            String[] paramNames = parameterNameDiscoverer.getParameterNames(method);

            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }

            // Also support #a0, #p0 style arguments
            for (int i = 0; i < args.length; i++) {
                context.setVariable("a" + i, args[i]);
                context.setVariable("p" + i, args[i]);
            }

            Expression expression = parser.parseExpression(keyExpression);
            Object value = expression.getValue(context);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            // Fallback to raw string if evaluation fails (e.g., it's a literal string not an expression)
            // Ideally we check if it looks like an expression, but SpEL can parse literals too.
            // If it fails, log warning or just return raw.
            log.trace("SpEL evaluation failed for key [{}], using raw value", keyExpression, e);
            return keyExpression;
        }
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
        if (StringUtils.isNotBlank(annotation.value())) {
            return annotation.value();
        }
        if (StringUtils.isNotBlank(annotation.name())) {
            return annotation.name();
        }
        return joinPoint.getSignature().getName();
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
