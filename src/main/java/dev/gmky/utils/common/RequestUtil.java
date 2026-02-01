package dev.gmky.utils.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Utility class for HTTP request operations.
 * <p>
 * Provides static access to the current {@link HttpServletRequest} and its attributes
 * via {@link RequestContextHolder}.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.0
 */
@UtilityClass
public class RequestUtil {
    private static HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        return (attributes != null) ? attributes.getRequest() : null;
    }

    /**
     * Retrieves the value of the specified request header from the current HTTP request.
     *
     * @param name the name of the header
     * @return the header value, or null if the request is not available or the header is missing
     */
    public static String getHeader(String name) {
        HttpServletRequest request = getCurrentHttpRequest();
        return (request != null) ? request.getHeader(name) : null;
    }
}
