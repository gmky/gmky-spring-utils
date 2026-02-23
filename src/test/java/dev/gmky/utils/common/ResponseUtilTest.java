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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

    @Test
    void generatePaginationHeader_ShouldHandleMiddlePage() {
        // Setup mock request
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resource");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Middle page: total 3 pages (0, 1, 2), current 1
        List<String> content = Arrays.asList("item");
        Page<String> page = new PageImpl<>(content, PageRequest.of(1, 10), 30);

        HttpHeaders headers = ResponseUtil.generatePaginationHeader(ServletUriComponentsBuilder.fromCurrentRequest(), page);

        String link = headers.getFirst("Link");
        assertNotNull(link);
        assertTrue(link.contains("rel=\"first\""));
        assertTrue(link.contains("rel=\"last\""));
        assertTrue(link.contains("rel=\"next\""));
        assertTrue(link.contains("rel=\"prev\""));

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void generatePaginationHeader_ShouldReturnEmptyHeaders_WhenPageIsNull() {
        // Setup mock request because fromCurrentRequest() needs it
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resource");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        HttpHeaders headers = ResponseUtil.generatePaginationHeader(ServletUriComponentsBuilder.fromCurrentRequest(), null);
        assertTrue(headers.isEmpty());
        
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testGeneratePaginationHeaderFirstPage() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resource");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Page<String> page = new PageImpl<>(Arrays.asList("item"), PageRequest.of(0, 10), 30);
        HttpHeaders headers = ResponseUtil.generatePaginationHeader(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        String link = headers.getFirst("Link");
        assertTrue(link.contains("rel=\"next\""));
        assertTrue(link.contains("rel=\"last\""));
        assertTrue(link.contains("rel=\"first\""));
        assertFalse(link.contains("rel=\"prev\""));
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testGeneratePaginationHeaderLastPage() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resource");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Page<String> page = new PageImpl<>(Arrays.asList("item"), PageRequest.of(2, 10), 30);
        HttpHeaders headers = ResponseUtil.generatePaginationHeader(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        String link = headers.getFirst("Link");
        assertFalse(link.contains("rel=\"next\""));
        assertTrue(link.contains("rel=\"prev\""));
        assertTrue(link.contains("rel=\"last\""));
        assertTrue(link.contains("rel=\"first\""));
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testGeneratePaginationHeaderSinglePage() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resource");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Page<String> page = new PageImpl<>(Arrays.asList("item"), PageRequest.of(0, 10), 5);
        HttpHeaders headers = ResponseUtil.generatePaginationHeader(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        String link = headers.getFirst("Link");
        assertFalse(link.contains("rel=\"next\""));
        assertFalse(link.contains("rel=\"prev\""));
        assertTrue(link.contains("rel=\"last\""));
        assertTrue(link.contains("rel=\"first\""));
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testPreparePageUriEscapesCommaAndSemicolon() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resource,test;case");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Page<String> page = new PageImpl<>(Arrays.asList("item"), PageRequest.of(0, 10), 5);
        HttpHeaders headers = ResponseUtil.generatePaginationHeader(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        String link = headers.getFirst("Link");
        assertTrue(link.contains("%2C"));
        assertTrue(link.contains("%3B"));
        RequestContextHolder.resetRequestAttributes();
    }
}
