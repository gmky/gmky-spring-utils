package dev.gmky.utils.logging.aop;

import dev.gmky.utils.core.NullSafePropertyAccessor;
import dev.gmky.utils.logging.annotation.LogPrefix;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * Aspect implementation for adding custom log prefixes via MDC.
 * <p>
 * This aspect intercepts methods annotated with {@link LogPrefix} and automatically
 * sets a prefix in the MDC (Mapped Diagnostic Context) that can be included in log
 * messages. The prefix is evaluated using Spring Expression Language (SpEL) and
 * supports both static strings and dynamic values based on method parameters.
 * </p>
 * <p>
 * The aspect ensures proper cleanup by removing the MDC value in a finally block,
 * preventing MDC pollution across different method calls.
 * </p>
 *
 * <h3>SpEL Support:</h3>
 * <p>
 * The aspect supports the following SpEL syntaxes:
 * </p>
 * <ul>
 *   <li>{@code #parameterName} - Access by parameter name (requires -parameters compiler flag)</li>
 *   <li>{@code #p0}, {@code #a0} - Positional argument access (always works)</li>
 *   <li>Property access: {@code #order.id}, {@code #user.name}, etc. (null-safe, behaves like {@code ?.})</li>
 * </ul>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * @LogPrefix("#order.id")
 * public void processOrder(Order order) {
 *     log.info("Processing order"); // MDC will contain logPrefix=ORDER-123
 * }
 * }</pre>
 *
 * @author HiepVH
 * @see LogPrefix
 * @see org.slf4j.MDC
 * @since 1.0.0
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class LogPrefixAspectImpl implements LogPrefixAspect {
    private static final String MDC_LOG_PREFIX = "logPrefix";
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Override
    @Around("@annotation(dev.gmky.utils.logging.annotation.LogPrefix)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            var annotation = getAnnotation(joinPoint);
            var prefix = getKey(joinPoint, annotation);
            MDC.put(MDC_LOG_PREFIX, prefix);
            return joinPoint.proceed();
        } finally {
            MDC.remove(MDC_LOG_PREFIX);
        }
    }

    private String getKey(ProceedingJoinPoint joinPoint, LogPrefix annotation) {
        String keyExpression = annotation.value();
        if (StringUtils.isBlank(keyExpression)) {
            return "";
        }

        try {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();
            Object[] args = joinPoint.getArgs();

            StandardEvaluationContext context = new StandardEvaluationContext();
            context.addPropertyAccessor(new NullSafePropertyAccessor());
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
            // If it contains # or starts with single quote, it's likely an expression that failed, so return empty.
            log.trace("SpEL evaluation failed for key [{}], using raw value", keyExpression, e);
            boolean isLikelyExpression = keyExpression.contains("#") || keyExpression.contains("'");
            return isLikelyExpression ? "" : keyExpression;
        }
    }

    /**
     * Retrieves the {@link LogPrefix} annotation from the intercepted method.
     *
     * @param joinPoint the proceeding join point
     * @return the LogPrefix annotation instance
     * @throws NullPointerException if the annotation is not found
     */
    @NonNull
    private static LogPrefix getAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getAnnotation(LogPrefix.class);
    }
}
