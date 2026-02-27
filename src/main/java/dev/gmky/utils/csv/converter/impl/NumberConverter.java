package dev.gmky.utils.csv.converter.impl;

import dev.gmky.utils.csv.converter.TypeConverter;
import dev.gmky.utils.csv.mapper.CsvFieldMeta;
import org.apache.commons.lang3.StringUtils;

/**
 * Built-in converter for numeric types: {@link Integer}, {@link Long}, {@link Double}, {@link Float}, {@link Short}.
 * Handles both primitive and boxed types.
 *
 * @author HiepVH
 * @since 1.0.3
 */
public class NumberConverter implements TypeConverter<Number> {

    @Override
    public Number convert(String value, CsvFieldMeta meta) {
        if (StringUtils.isBlank(value)) return null;
        String trimmed = value.trim();
        Class<?> fieldType = meta.getFieldType();

        if (fieldType == Integer.class || fieldType == int.class) return Integer.parseInt(trimmed);
        if (fieldType == Long.class || fieldType == long.class) return Long.parseLong(trimmed);
        if (fieldType == Double.class || fieldType == double.class) return Double.parseDouble(trimmed);
        if (fieldType == Float.class || fieldType == float.class) return Float.parseFloat(trimmed);
        if (fieldType == Short.class || fieldType == short.class) return Short.parseShort(trimmed);
        if (fieldType == Byte.class || fieldType == byte.class) return Byte.parseByte(trimmed);

        throw new IllegalArgumentException("Unsupported numeric type: " + fieldType.getName());
    }

    @Override
    public Class<Number> getTargetType() {
        return Number.class;
    }
}
