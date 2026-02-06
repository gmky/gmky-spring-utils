package dev.gmky.utils.core;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

/**
 * A property accessor that returns null instead of throwing an exception
 * when a property is accessed on a null target.
 * <p>
 * This is useful for SpEL evaluation where we want to handle null targets
 * gracefully by returning null instead of stopping evaluation.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.0
 */
public class NullSafePropertyAccessor implements PropertyAccessor {
    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return null;
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) {
        return target == null;
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) {
        return TypedValue.NULL;
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) {
        return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) {
        // No-op
    }
}
