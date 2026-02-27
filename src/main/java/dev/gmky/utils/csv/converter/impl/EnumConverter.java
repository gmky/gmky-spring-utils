package dev.gmky.utils.csv.converter.impl;

import dev.gmky.utils.csv.converter.TypeConverter;
import dev.gmky.utils.csv.mapper.CsvFieldMeta;
import org.apache.commons.lang3.StringUtils;

/**
 * Built-in converter for Java {@link Enum} types.
 * Performs case-insensitive matching against enum constant names.
 *
 * @author HiepVH
 * @since 1.0.3
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class EnumConverter implements TypeConverter<Enum> {

    @Override
    public Enum convert(String value, CsvFieldMeta meta) {
        if (StringUtils.isBlank(value)) return null;
        Class<? extends Enum> enumType = (Class<? extends Enum>) meta.getFieldType();
        String trimmed = value.trim();
        for (Enum constant : enumType.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(trimmed)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(
                String.format("Unknown enum value '%s' for type %s", trimmed, enumType.getSimpleName())
        );
    }

    @Override
    public Class<Enum> getTargetType() {
        return Enum.class;
    }
}
