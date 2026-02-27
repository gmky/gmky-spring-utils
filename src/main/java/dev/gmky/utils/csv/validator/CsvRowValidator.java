package dev.gmky.utils.csv.validator;

import java.util.List;

/**
 * Interface for validating a mapped DTO row before it is added to the result set.
 * <p>
 * Called after {@link dev.gmky.utils.csv.mapper.CsvRowMapper} successfully maps a row.
 * Return an empty list to indicate the row is valid.
 * </p>
 *
 * @param <T> the target DTO type
 * @author HiepVH
 * @since 1.0.3
 */
@FunctionalInterface
public interface CsvRowValidator<T> {

    /**
     * Validates the given mapped record.
     *
     * @param record the mapped DTO instance
     * @return a list of validation violation messages, or empty list if valid
     */
    List<String> validate(T record);
}
