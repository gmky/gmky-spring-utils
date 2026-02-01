package dev.gmky.utils.common;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.MessageFormat;
import java.util.List;

/**
 * Utility class for creating standardized {@link ResponseEntity} objects.
 * <p>
 * Provides helper methods for success, no-content, created responses, and
 * pagination support with "Link" and "X-TOTAL-COUNT" headers.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.0
 */
@UtilityClass
public class ResponseUtil {
    private static final String X_TOTAL_COUNT_HEADER = "X-TOTAL-COUNT";

    /**
     * Creates a 200 OK response with the given data body.
     *
     * @param data the body content
     * @param <T>  the type of the body
     * @return a {@link ResponseEntity} with status 200 and the given body
     */
    public static <T> ResponseEntity<T> data(T data) {
        return ResponseEntity.ok(data);
    }

    /**
     * Creates a 200 OK response for a paginated result.
     * <p>
     * Adds "X-TOTAL-COUNT" and "Link" headers for pagination navigation.
     * </p>
     *
     * @param page the Spring Data {@link Page} object
     * @param <T>  the type of elements in the page
     * @return a {@link ResponseEntity} with status 200, the page content as body, and pagination headers
     */
    public static <T> ResponseEntity<List<T>> data(Page<T> page) {
        var headers = generatePaginationHeader(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * Creates a 200 OK response with no body.
     *
     * @return a {@link ResponseEntity} with status 200
     */
    public static ResponseEntity<Void> ok() {
        return ResponseEntity.ok().build();
    }

    /**
     * Creates a 204 No Content response.
     *
     * @return a {@link ResponseEntity} with status 204
     */
    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a 201 Created response.
     *
     * @return a {@link ResponseEntity} with status 201
     */
    public static ResponseEntity<Void> created() {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Generates pagination headers (Link and X-TOTAL-COUNT).
     *
     * @param uriBuilder the URI builder for generating links
     * @param page       the results page
     * @param <T>        the type of elements
     * @return {@link HttpHeaders} containing pagination info
     */
    public static <T> HttpHeaders generatePaginationHeader(ServletUriComponentsBuilder uriBuilder, Page<T> page) {
        HttpHeaders headers = new HttpHeaders();
        if (page == null) return headers;
        headers.add(X_TOTAL_COUNT_HEADER, Long.toString(page.getTotalElements()));
        int pageNumber = page.getNumber();
        int pageSize = page.getSize();
        StringBuilder link = new StringBuilder();
        if (pageNumber < page.getTotalPages() - 1) {
            link.append(prepareLink(uriBuilder, pageNumber + 1, pageSize, "next")).append(",");
        }

        if (pageNumber > 0) {
            link.append(prepareLink(uriBuilder, pageNumber - 1, pageSize, "prev")).append(",");
        }

        link.append(prepareLink(uriBuilder, page.getTotalPages() - 1, pageSize, "last")).append(",").append(prepareLink(uriBuilder, 0, pageSize, "first"));
        headers.add("Link", link.toString());
        return headers;
    }

    private static String prepareLink(UriComponentsBuilder uriBuilder, int pageNumber, int pageSize, String relType) {
        return MessageFormat.format("<{0}>; rel=\"{1}\"", preparePageUri(uriBuilder, pageNumber, pageSize), relType);
    }

    private static String preparePageUri(UriComponentsBuilder uriBuilder, int pageNumber, int pageSize) {
        return uriBuilder.replaceQueryParam("page", Integer.toString(pageNumber)).replaceQueryParam("size", Integer.toString(pageSize)).toUriString().replace(",", "%2C").replace(";", "%3B");
    }
}
