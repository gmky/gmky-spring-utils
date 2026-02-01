package dev.gmky.utils.execution.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.gmky.utils.execution.annotation.ExecutionTime;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionTimeAspectTest {

    private ExecutionTimeAspectImpl aspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        aspect = new ExecutionTimeAspectImpl();

        // Setup Logback for testing logs
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ExecutionTimeAspectImpl.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.DEBUG);
    }

    @Test
    void around_ShouldLogExecutionTime_WhenDebugEnabled() throws Throwable {
        // Arrange
        Method method = TestClass.class.getMethod("testMethod");
        // We don't need to manually create the annotation mock if we use the real method which has the annotation
        // The aspect implementation reads the annotation from the method

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("success");

        // Act
        Object result = aspect.around(joinPoint);

        // Assert
        assertEquals("success", result);
        verify(joinPoint, times(1)).proceed();

        List<ILoggingEvent> logs = listAppender.list;
        // Depending on async logger or not, this might be flaky, but for unit test with list appender it should be fine.
        assertEquals(1, logs.size());
        assertEquals(Level.DEBUG, logs.getFirst().getLevel());
        assertEquals("Method [{}] - [{}] executed in {} ms", logs.getFirst().getMessage());
        String formattedMessage = logs.getFirst().getFormattedMessage();
        assertTrue(formattedMessage.startsWith("Method [Test Method] - [test-key] executed in "));
        assertTrue(formattedMessage.endsWith(" ms"));
    }

    @Test
    void around_ShouldLogFailure_WhenExceptionThrown() throws Throwable {
        // Arrange
        Method method = TestClass.class.getMethod("testMethod");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> aspect.around(joinPoint));

        List<ILoggingEvent> logs = listAppender.list;
        assertEquals(1, logs.size());
        assertEquals(Level.INFO, logs.getFirst().getLevel());
        String formattedMessage = logs.getFirst().getFormattedMessage();
        assertTrue(formattedMessage.startsWith("Method [Test Method] - [test-key] failed in "));
        assertTrue(formattedMessage.endsWith(" ms"));
    }

    @Test
    void around_ShouldNotLog_WhenDebugDisabled() throws Throwable {
        // Setup Logback for testing logs
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ExecutionTimeAspectImpl.class);
        logger.setLevel(Level.INFO);
        // Clear previous logs
        listAppender.list.clear();

        // Act
        // Note: The aspect checks isDebugEnabled() before doing anything. 
        // If the level is INFO, isDebugEnabled() returns false.

        // Make sure we set up the expectations even if logging is skipped, proceed is called
        // But wait, the aspect calls getAnnotation ONLY if debug is enabled? 
        // Let's check the implementation:
        // if (log.isDebugEnabled()) { ... } return joinPoint.proceed();
        // So if NOT debug enabled, it should just proceed.

        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.around(joinPoint);

        // Assert
        verify(joinPoint, times(1)).proceed();
        assertEquals("success", result);
        assertEquals(0, listAppender.list.size());
    }

    @Test
    void around_ShouldEvaluateSpelKey() throws Throwable {
        // Arrange
        // For SpEL test, we need signature to return parameter names
        // But DefaultParameterNameDiscoverer uses debug info or -parameters flag
        // In simple unit test with mocks, we might struggle to get real parameter names unless we use real methods.
        // Let's use #p0 syntax which doesn't require names.

        Method method = TestClass.class.getMethod("testMethodWithArgs", String.class);
        Object[] args = new Object[]{"dynamic-value"};

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("success");

        // Act
        Object result = aspect.around(joinPoint);

        // Assert
        assertEquals("success", result);
        List<ILoggingEvent> logs = listAppender.list;
        assertEquals(1, logs.size());
        assertEquals(Level.DEBUG, logs.getFirst().getLevel());

        String formattedMessage = logs.getFirst().getFormattedMessage();
        // The key in annotation is "#p0", so it should evaluate to "dynamic-value"
        assertTrue(formattedMessage.contains("- [dynamic-value] executed in"));
    }

    static class TestClass {
        @ExecutionTime(value = "Test Method", key = "test-key")
        public void testMethod() {
        }

        @ExecutionTime(value = "SpEL Method", key = "#p0")
        public void testMethodWithArgs(String arg) {
        }

        @ExecutionTime(name = "Alternate Name", value = "Primary Name")
        public void testMethodWithNamePriority() {
        }

        @ExecutionTime(key = "#invalid.expression")
        public void testMethodWithInvalidSpel() {
        }

        @ExecutionTime(key = "")
        public void testMethodWithBlankKey() {
        }
    }

    @Test
    void around_ShouldUseValueOverName_WhenBothPresent() throws Throwable {
        Method method = TestClass.class.getMethod("testMethodWithNamePriority");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("success");

        aspect.around(joinPoint);

        String logMessage = listAppender.list.getLast().getFormattedMessage();
        assertTrue(logMessage.contains("Method [Primary Name]"));
    }

    @Test
    void around_ShouldFallbackToRawKey_WhenSpelFails() throws Throwable {
        Method method = TestClass.class.getMethod("testMethodWithInvalidSpel");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("success");

        aspect.around(joinPoint);

        String logMessage = listAppender.list.getLast().getFormattedMessage();
        assertTrue(logMessage.contains("- [#invalid.expression] executed"));
    }

    @Test
    void around_ShouldHandleBlankKey() throws Throwable {
        Method method = TestClass.class.getMethod("testMethodWithBlankKey");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethodWithBlankKey"); // Stub name for fallback
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("success");

        aspect.around(joinPoint);

        String logMessage = listAppender.list.getLast().getFormattedMessage();
        assertTrue(logMessage.contains("Method [testMethodWithBlankKey] - [] executed"));
    }

    @Test
    void around_ShouldProceedWithoutLogging_WhenDebugDisabled() throws Throwable {
        // Create aspect with logger that has debug disabled
        ExecutionTimeAspectImpl aspect = new ExecutionTimeAspectImpl();
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ExecutionTimeAspectImpl.class);
        logger.setLevel(Level.INFO); // Disable DEBUG

        Method method = TestClass.class.getMethod("testMethod");
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.around(joinPoint);

        assertEquals("success", result);
        // Verify no new logs (listAppender is attached to static logger, so we check if count increased)
        // Reset list appender before this test potentially or check size change
        // For simplicity, just ensure no DEBUG log added
        boolean hasDebugLog = listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.DEBUG && event.getMessage().contains("Test Method"));
        // This might interpret previous logs if we don't clear.
        // Better to clear list in setUp or check size relative to start.
        // Let's rely on checking the last log is NOT from this execution or list size depends on isolation.
        // Actually, since we control the execution order in this single test run...
        // Let's just trust that setUp clears or creates new appender? 
        // SetUp adds appender but doesn't clear static list? 
        // `listAppender = new ListAppender<>()` in setUp. Yes, it's new instance every time.

        assertEquals(0, listAppender.list.size());
    }
}
