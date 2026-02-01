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

    static class TestClass {
        @ExecutionTime(value = "Test Method", key = "test-key")
        public void testMethod() {
        }
    }
}
