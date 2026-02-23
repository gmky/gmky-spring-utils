package dev.gmky.utils.startup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppReadyLoggingTest {

    @Mock
    private Environment env;

    @Test
    void logApplication_ShouldLogCorrectly() {
        // Arrange
        when(env.getProperty("server.ssl.key-store")).thenReturn(null);
        when(env.getProperty("server.port")).thenReturn("8080");
        when(env.getProperty("server.servlet.context-path")).thenReturn("/api");
        when(env.getProperty("spring.application.name")).thenReturn("TestApp");
        when(env.getActiveProfiles()).thenReturn(new String[]{"dev"});

        AppReadyLoggingImpl appReadyLogging = new AppReadyLoggingImpl(env);

        // Act
        appReadyLogging.logApplication();

        // Assert
        verify(env, atLeastOnce()).getProperty("spring.application.name");
        verify(env, atLeastOnce()).getActiveProfiles();
    }

    @Test
    void testLogApplicationWithSslKeyStore() {
        when(env.getProperty("server.ssl.key-store")).thenReturn("classpath:keystore.jks");
        when(env.getProperty("server.port")).thenReturn("8443");
        when(env.getProperty("server.servlet.context-path")).thenReturn("/api");
        when(env.getProperty("spring.application.name")).thenReturn("TestApp");
        when(env.getActiveProfiles()).thenReturn(new String[]{"dev"});

        AppReadyLoggingImpl appReadyLogging = new AppReadyLoggingImpl(env);
        appReadyLogging.logApplication();
        verify(env, atLeastOnce()).getProperty("server.ssl.key-store");
    }

    @Test
    void testLogApplicationWithNullPort() {
        when(env.getProperty("server.ssl.key-store")).thenReturn(null);
        when(env.getProperty("server.port")).thenReturn(null);
        when(env.getProperty("server.servlet.context-path")).thenReturn("");
        when(env.getProperty("spring.application.name")).thenReturn("TestApp");
        when(env.getActiveProfiles()).thenReturn(new String[]{});

        AppReadyLoggingImpl appReadyLogging = new AppReadyLoggingImpl(env);
        appReadyLogging.logApplication();
        verify(env, atLeastOnce()).getProperty("server.port");
    }

    @Test
    void testLogApplicationWithBlankContextPath() {
        when(env.getProperty("server.ssl.key-store")).thenReturn(null);
        when(env.getProperty("server.port")).thenReturn("8080");
        when(env.getProperty("server.servlet.context-path")).thenReturn("   ");
        when(env.getProperty("spring.application.name")).thenReturn("TestApp");
        when(env.getActiveProfiles()).thenReturn(new String[]{});

        AppReadyLoggingImpl appReadyLogging = new AppReadyLoggingImpl(env);
        appReadyLogging.logApplication();
        verify(env, atLeastOnce()).getProperty("server.servlet.context-path");
    }

    @Test
    void testLogApplicationWithNullContextPath() {
        when(env.getProperty("server.ssl.key-store")).thenReturn(null);
        when(env.getProperty("server.port")).thenReturn("8080");
        when(env.getProperty("server.servlet.context-path")).thenReturn(null);
        when(env.getProperty("spring.application.name")).thenReturn("TestApp");
        when(env.getActiveProfiles()).thenReturn(new String[]{});

        AppReadyLoggingImpl appReadyLogging = new AppReadyLoggingImpl(env);
        appReadyLogging.logApplication();
        verify(env, atLeastOnce()).getProperty("server.servlet.context-path");
    }
}
