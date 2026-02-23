package dev.gmky.utils.core;

import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.junit.jupiter.api.Assertions.*;

class NullSafePropertyAccessorTest {

    private final NullSafePropertyAccessor accessor = new NullSafePropertyAccessor();
    private final EvaluationContext context = new StandardEvaluationContext();

    @Test
    void testGetSpecificTargetClassesReturnsNull() {
        assertNull(accessor.getSpecificTargetClasses());
    }

    @Test
    void testCanReadReturnsTrueForNullTarget() {
        assertTrue(accessor.canRead(context, null, "anyProperty"));
    }

    @Test
    void testCanReadReturnsFalseForNonNullTarget() {
        assertFalse(accessor.canRead(context, new Object(), "anyProperty"));
    }

    @Test
    void testReadReturnsTypedValueNull() {
        TypedValue value = accessor.read(context, null, "anyProperty");
        assertEquals(TypedValue.NULL, value);
    }

    @Test
    void testCanWriteReturnsFalse() {
        assertFalse(accessor.canWrite(context, null, "anyProperty"));
        assertFalse(accessor.canWrite(context, new Object(), "anyProperty"));
    }

    @Test
    void testWriteIsNoOp() {
        assertDoesNotThrow(() -> accessor.write(context, null, "anyProperty", "newValue"));
        assertDoesNotThrow(() -> accessor.write(context, new Object(), "anyProperty", "newValue"));
    }
}
