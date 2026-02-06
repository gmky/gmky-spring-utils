package dev.gmky.utils.logging.annotation;

import java.lang.annotation.*;

/**
 * Annotation to add custom prefix to log messages via MDC (Mapped Diagnostic Context).
 * <p>
 * This annotation can be used on methods to automatically set a log prefix that will
 * be included in all log messages generated within that method's execution context.
 * The prefix is stored in MDC with the key "logPrefix" and automatically cleaned up
 * after method execution.
 * </p>
 * <p>
 * The annotation supports both static strings and Spring Expression Language (SpEL)
 * for dynamic prefix generation based on method parameters.
 * </p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Static prefix
 * @LogPrefix("[ORDER-SERVICE]")
 * public void processOrder(Order order) {
 *     log.info("Processing"); // Logs: ... [ORDER-SERVICE] : Processing
 * }
 *
 * // Dynamic prefix with SpEL
 * @LogPrefix("#order.id")
 * public void processOrder(Order order) {
 *     log.info("Processing"); // Logs: ... [ORDER-123] : Processing
 * }
 * }</pre>
 *
 * <h3>Logging Pattern Configuration:</h3>
 * <p>
 * To use this annotation, configure your logging pattern to include MDC:
 * </p>
 * <pre>
 * logging.pattern.console: "... %X{logPrefix:+ [%X{logPrefix}]} : %m%n"
 * </pre>
 *
 * @author HiepVH
 * @see org.slf4j.MDC
 * @since 1.0.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogPrefix {
    /**
     * The log prefix value.
     * <p>
     * Can be a static string or a Spring Expression Language (SpEL) expression
     * that will be evaluated against method parameters.
     * </p>
     * <p>
     * Supported SpEL syntaxes:
     * </p>
     * <ul>
     *   <li>{@code #paramName} - Access by parameter name</li>
     *   <li>{@code #p0}, {@code #a0} - Positional argument access</li>
     *   <li>{@code #order.id} - Property access (null-safe, behaves like {@code ?.})</li>
     * </ul>
     *
     * @return the log prefix value or SpEL expression
     */
    String value() default "";
}
