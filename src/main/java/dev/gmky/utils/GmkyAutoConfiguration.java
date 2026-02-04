package dev.gmky.utils;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Main auto-configuration class for GMKY Spring Utils library.
 * <p>
 * This configuration class enables automatic component scanning for all utility
 * classes and configurations provided by the GMKY Spring Utils library. When this
 * library is included as a dependency in a Spring Boot application, this configuration
 * will be automatically loaded through Spring Boot's auto-configuration mechanism.
 * </p>
 * <p>
 * The following components are automatically registered:
 * <ul>
 *   <li>Execution time monitoring aspect ({@code ExecutionTimeAspectImpl})</li>
 *   <li>Application startup logging ({@code AppReadyLoggingImpl})</li>
 *   <li>Common utility classes ({@code AppContextUtil}, {@code DateUtil}, {@code RequestUtil}, {@code ResponseUtil})</li>
 *   <li>Entity mapper interface ({@code EntityMapper})</li>
 * </ul>
 * </p>
 *
 * @author HiepVH
 * @see org.springframework.boot.autoconfigure.AutoConfiguration
 * @since 1.0.0
 */
@Configuration
@AutoConfiguration
@ComponentScan(basePackages = "dev.gmky.utils")
public class GmkyAutoConfiguration {
}
