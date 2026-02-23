package dev.gmky.utils.logging.aop;

import dev.gmky.utils.logging.annotation.LogPrefix;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogPrefixAspectTest {

    private LogPrefixAspectImpl aspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @BeforeEach
    void setUp() {
        aspect = new LogPrefixAspectImpl();
        MDC.clear();
    }

    @Test
    void around_ShouldReturnEmptyStringOnNullPropertyAccess() throws Throwable {
        // Arrange
        Method method = TestClass.class.getMethod("testMethodWithNullArg", Object.class);
        Object[] args = new Object[]{null};

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);

        when(joinPoint.proceed()).thenAnswer(invocation -> MDC.get("logPrefix"));

        // Act
        Object result = aspect.around(joinPoint);

        // Assert
        assertEquals("", result, "Should return empty string when property access fails on null");
    }

    @Test
    void around_ShouldReturnRawStringWhenNotAnExpression() throws Throwable {
        // Arrange
        Method method = TestClass.class.getMethod("testMethodWithLiteral");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);

        when(joinPoint.proceed()).thenAnswer(invocation -> MDC.get("logPrefix"));

        // Act
        Object result = aspect.around(joinPoint);

        // Assert
        assertEquals("STATIC_PREFIX", result, "Should return raw string for literal value");
    }

    @Test
    void around_ShouldHandleConcatenationInSpel() throws Throwable {
        // Arrange
        Method method = TestClass.class.getMethod("testMethodWithConcatenation", String.class);
        Object[] args = new Object[]{"123"};

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);

        when(joinPoint.proceed()).thenAnswer(invocation -> MDC.get("logPrefix"));

        // Act
        Object result = aspect.around(joinPoint);

        // Assert
        assertEquals("REQ-123", result, "Should support string concatenation in SpEL");
    }

    @Test
    void around_ShouldHandleNullInConcatenation() throws Throwable {
        // Arrange
        Method method = TestClass.class.getMethod("testMethodWithConcatenation", String.class);
        Object[] args = new Object[]{null};

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);

        when(joinPoint.proceed()).thenAnswer(invocation -> MDC.get("logPrefix"));

        // Act
        Object result = aspect.around(joinPoint);

        // Assert
        // In SpEL, 'REQ-' + null results in "REQ-null"
        assertEquals("REQ-null", result);
    }

    @Test
    void around_ShouldReturnPartialResultOnComplexSpelNullFailure() throws Throwable {
        // Arrange
        Method method = TestClass.class.getMethod("testMethodWithComplexSpel", Object.class);
        Object[] args = new Object[]{null};

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);

        when(joinPoint.proceed()).thenAnswer(invocation -> MDC.get("logPrefix"));

        // Act
        Object result = aspect.around(joinPoint);

        // Assert
        // User wants "REQ-null" for "'REQ-' + #p0.id" even if #p0 is null
        assertEquals("REQ-null", result, "Should return partial result with 'null' placeholder when property access fails");
    }

    @Test
    void testAroundWithBlankAnnotationValue() throws Throwable {
        Method method = TestClass.class.getMethod("testMethodWithBlankKey");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenAnswer(invocation -> MDC.get("logPrefix"));
        Object result = aspect.around(joinPoint);
        assertEquals("", result);
    }

    @Test
    void testAroundWithDefaultAnnotationValue() throws Throwable {
        Method method = TestClass.class.getMethod("testMethodWithDefault");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenAnswer(invocation -> MDC.get("logPrefix"));
        Object result = aspect.around(joinPoint);
        assertEquals("", result);
    }

    @Test
    void testMdcIsCleanedUpOnException() throws Throwable {
        Method method = TestClass.class.getMethod("testMethodWithLiteral");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Test Exception"));

        assertThrows(RuntimeException.class, () -> aspect.around(joinPoint));
        assertNull(MDC.get("logPrefix"), "MDC should be cleaned up after exception");
    }

    @Test
    void testAroundWithParamNamesNotNull() throws Throwable {
        Method method = TestClass.class.getMethod("testMethodWithParamNames", String.class);
        Object[] args = new Object[]{"named-param-value"};
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenAnswer(invocation -> MDC.get("logPrefix"));

        Object result = aspect.around(joinPoint);
        assertEquals("named-param-value", result);
    }

    @Test
    void testGetKeyWithLiteralStringContainingHash() throws Throwable {
        Method method = TestClass.class.getMethod("testMethodWithMissingVar");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenAnswer(invocation -> MDC.get("logPrefix"));
        Object result = aspect.around(joinPoint);
        assertEquals("", result);
    }

    static class TestClass {
        @LogPrefix("#p0.id")
        public void testMethodWithNullArg(Object input) {
        }

        @LogPrefix("STATIC_PREFIX")
        public void testMethodWithLiteral() {
        }

        @LogPrefix("'REQ-' + #p0")
        public void testMethodWithConcatenation(String arg) {
        }

        @LogPrefix("'REQ-' + #p0.id")
        public void testMethodWithComplexSpel(Object input) {
        }

        @LogPrefix("")
        public void testMethodWithBlankKey() {
        }

        @LogPrefix
        public void testMethodWithDefault() {
        }

        @LogPrefix("#a0")
        public void testMethodWithParamNames(String mySpecialParam) {
        }

        @LogPrefix("#missingVar")
        public void testMethodWithMissingVar() {
        }
    }
}
