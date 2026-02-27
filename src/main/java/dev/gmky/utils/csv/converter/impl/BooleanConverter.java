package dev.gmky.utils.csv.converter.impl;

import dev.gmky.utils.csv.converter.TypeConverter;
import dev.gmky.utils.csv.mapper.CsvFieldMeta;
import org.apache.commons.lang3.StringUtils;

/**
 * Built-in converter for {@link Boolean} fields.
 * Recognizes: "true", "yes", "1", "on" as true (case-insensitive).
 * Everything else is false.
 *
 * @author HiepVH
 * @since 1.0.3
 */
public class BooleanConverter implements TypeConverter<Boolean> {

    @Override
    public Boolean convert(String value, CsvFieldMeta meta) {
        if (StringUtils.isBlank(value)) return null;
        String lower = value.trim().toLowerCase();
        return "true".equals(lower) || "yes".equals(lower) || "1".equals(lower) || "on".equals(lower);
    }

    @Override
    public Class<Boolean> getTargetType() {
        return Boolean.class;
    }
}
