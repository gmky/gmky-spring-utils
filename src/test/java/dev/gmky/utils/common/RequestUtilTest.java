package dev.gmky.utils.common;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestUtilTest {

    @Test
    void getHeader_ShouldReturnHeaderValue_WhenRequestIsPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Test-Header", "test-value");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String headerValue = RequestUtil.getHeader("Test-Header");

        assertEquals("test-value", headerValue);

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getHeader_ShouldReturnNull_WhenRequestIsNotPresent() {
        RequestContextHolder.resetRequestAttributes();
        // Explicitly set null to ensure we test the null attribute branch
        RequestContextHolder.setRequestAttributes(null);
        
        String headerValue = RequestUtil.getHeader("Test-Header");

        assertNull(headerValue);
    }
}
