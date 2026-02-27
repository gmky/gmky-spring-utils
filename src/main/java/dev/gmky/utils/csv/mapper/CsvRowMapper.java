package dev.gmky.utils.csv.mapper;

/**
 * Functional interface for mapping a single parsed CSV row (as a {@code String[]} of columns)
 * to a target DTO instance.
 *
 * <p>Implement this to bypass annotation scanning entirely and provide custom mapping logic.</p>
 *
 * @param <T> the target DTO type
 * @author HiepVH
 * @since 1.0.3
 */
@FunctionalInterface
public interface CsvRowMapper<T> {

    /**
     * Maps a parsed CSV row to a DTO instance.
     *
     * @param columns    the raw column values from the CSV row
     * @param headers    the header row (may be null if no header)
     * @param lineNumber the 1-based line number in the file (for error reporting)
     * @return the mapped DTO instance
     * @throws Exception if mapping fails
     */
    T map(String[] columns, String[] headers, long lineNumber) throws Exception;
}
