package dev.gmky.utils.csv.converter.impl;

import dev.gmky.utils.csv.converter.TypeConverter;
import dev.gmky.utils.csv.mapper.CsvFieldMeta;

/**
 * Built-in converter for {@link String} fields. Returns the value as-is (after optional trimming).
 *
 * @author HiepVH
 * @since 1.0.3
 */
public class StringConverter implements TypeConverter<String> {
    @Override
    public String convert(String value, CsvFieldMeta meta) {
        return value;
    }

    @Override
    public Class<String> getTargetType() {
        return String.class;
    }
}
