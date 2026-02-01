package dev.gmky.utils.startup;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Interface for application startup logging.
 * <p>
 * Responsible for logging application details (URLs, profile, etc.) when the Spring context is ready.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.0
 */
public interface AppReadyLogging {
    
    /**
     * Logs application information upon startup.
     * <p>
     * Triggered by {@link ApplicationReadyEvent}.
     * </p>
     */
    @EventListener(ApplicationReadyEvent.class)
    void logApplication();
}
