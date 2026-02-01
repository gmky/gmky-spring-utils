package dev.gmky.utils.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * Default implementation of {@link AppReadyLogging}.
 * <p>
 * Logs the following information when the application starts:
 * <ul>
 *   <li>Application Name</li>
 *   <li>Local Access URL of the application</li>
 *   <li>External Access URL (using Host IP)</li>
 *   <li>Swagger UI URL (if available/configured)</li>
 *   <li>Active Spring Profiles</li>
 * </ul>
 * <p>
 * It attempts to determine the local host IP address. If unable, it falls back to 'localhost'.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
@ConditionalOnMissingBean(AppReadyLogging.class)
public class AppReadyLoggingImpl implements AppReadyLogging {
    private final Environment env;

    @Override
    public void logApplication() {
        String protocol = Optional.ofNullable(env.getProperty("server.ssl.key-store")).map(key -> "https").orElse("http");
        String serverPort = StringUtils.defaultIfBlank(env.getProperty("server.port"), "8080");
        String contextPath = Optional
                .ofNullable(env.getProperty("server.servlet.context-path"))
                .filter(StringUtils::isNotBlank)
                .orElse("");
        String hostAddress = "localhost";
        String swaggerUrl = String.format("%s/swagger-ui/index.html", contextPath);
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }
        log.info(
                """
                                       \s
                        ----------------------------------------------------------------
                        Application '{}' is running! Access URLs:
                        Local\t\t: \t{}://localhost:{}{}
                        External\t: \t{}://{}:{}{}
                        SwaggerUI\t: \t{}://localhost:{}{}
                        Profiles\t: \t{}
                        ----------------------------------------------------------------""",
                env.getProperty("spring.application.name"),
                protocol,
                serverPort,
                contextPath,
                protocol,
                hostAddress,
                serverPort,
                contextPath,
                protocol,
                serverPort,
                swaggerUrl,
                env.getActiveProfiles()
        );

    }
}
