package dev.gmky.utils.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResponseUtilTest {

    @Test
    void data_ShouldReturnOkWithContent() {
        String content = "test data";
        ResponseEntity<String> response = ResponseUtil.data(content);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(content, response.getBody());
    }

    @Test
    void ok_ShouldReturnOkStatus() {
        ResponseEntity<Void> response = ResponseUtil.ok();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void noContent_ShouldReturnNoContentStatus() {
        ResponseEntity<Void> response = ResponseUtil.noContent();
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void created_ShouldReturnCreatedStatus() {
        ResponseEntity<Void> response = ResponseUtil.created();
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void data_Page_ShouldReturnOkWithPaginationHeaders() {
        // Setup mock request for ServletUriComponentsBuilder
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setRequestURI("/api/resource");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        List<String> content = Arrays.asList("item1", "item2");
        Page<String> page = new PageImpl<>(content, PageRequest.of(0, 10), 20);

        ResponseEntity<List<String>> response = ResponseUtil.data(page);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(content, response.getBody());

        HttpHeaders headers = response.getHeaders();
        assertTrue(headers.containsKey("X-TOTAL-COUNT"));
        assertEquals("20", headers.getFirst("X-TOTAL-COUNT"));
        assertTrue(headers.containsKey("Link"));
        String link = headers.getFirst("Link");
        assertNotNull(link);
        assertTrue(link.contains("rel=\"last\""));
        assertTrue(link.contains("rel=\"first\""));

        RequestContextHolder.resetRequestAttributes();
    }
}
