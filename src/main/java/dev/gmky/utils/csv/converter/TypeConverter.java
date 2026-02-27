package dev.gmky.utils.csv.converter;

import dev.gmky.utils.csv.mapper.CsvFieldMeta;

/**
 * SPI interface for converting a raw CSV string value to a target Java type.
 * <p>
 * Implement this interface and register via {@link TypeConverterRegistry} to support
 * custom types globally, or specify per-field via {@code @CsvColumn(converter = ...)}.
 * </p>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * public class MoneyConverter implements TypeConverter<BigDecimal> {
 *     public BigDecimal convert(String value, CsvFieldMeta meta) {
 *         return new BigDecimal(value.replace("$", "").replace(",", ""));
 *     }
 *     public Class<BigDecimal> getTargetType() { return BigDecimal.class; }
 * }
 * }</pre>
 *
 * @param <T> the target Java type this converter produces
 * @author HiepVH
 * @since 1.0.3
 */
public interface TypeConverter<T> {

    /**
     * Converts a raw CSV string to the target type.
     *
     * @param value the raw string from the CSV cell (may be blank if default is applied)
     * @param meta  metadata about the target field (type, annotations, etc.)
     * @return the converted value, or {@code null} if value is blank and not required
     * @throws Exception if conversion fails
     */
    T convert(String value, CsvFieldMeta meta) throws Exception;

    /**
     * The Java type this converter produces.
     *
     * @return the target type class
     */
    Class<T> getTargetType();
}
