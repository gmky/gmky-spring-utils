package dev.gmky.utils.execution.annotation;

import dev.gmky.utils.execution.aop.ExecutionTimeAspectImpl;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Annotation to mark methods for execution time monitoring.
 * <p>
 * When applied to a method, this annotation triggers an aspect that measures
 * and logs the execution time of the annotated method. This is useful for
 * performance monitoring and identifying slow methods.
 * </p>
 * <p>
 * The execution time is logged at DEBUG level. If the method fails with an
 * exception, it logs at INFO level.
 * </p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * @ExecutionTime("User Service")
 * public User findUser(Long id) {
 *     // method implementation
 * }
 *
 * @ExecutionTime(name = "Payment Processing", key = "order-123")
 * public void processPayment(Order order) {
 *     // method implementation
 * }
 * }</pre>
 *
 * @author HiepVH
 * @since 1.0.0
 * @see ExecutionTimeAspectImpl
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecutionTime {
    
    /**
     * The name to use when logging execution time.
     * <p>
     * This is an alias for {@link #name()}. If not specified, the actual
     * method name will be used.
     * </p>
     *
     * @return the custom name for logging
     */
    @AliasFor("name")
    String value() default "";

    /**
     * An optional key to provide additional context in the log message.
     * <p>
     * This can be used to identify specific executions, such as transaction IDs,
     * request IDs, or any other contextual information.
     * </p>
     *
     * @return the contextual key for logging
     */
    String key() default "";

    /**
     * The name to use when logging execution time.
     * <p>
     * This is an alias for {@link #value()}. If not specified, the actual
     * method name will be used.
     * </p>
     *
     * @return the custom name for logging
     */
    @AliasFor("value")
    String name() default "";
}
