package dev.gmky.utils.csv.converter.impl;

import dev.gmky.utils.csv.converter.TypeConverter;
import dev.gmky.utils.csv.mapper.CsvFieldMeta;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

/**
 * Built-in converter for {@link BigDecimal} fields.
 * Strips common currency symbols and group separators before parsing.
 *
 * @author HiepVH
 * @since 1.0.3
 */
public class BigDecimalConverter implements TypeConverter<BigDecimal> {

    @Override
    public BigDecimal convert(String value, CsvFieldMeta meta) {
        if (StringUtils.isBlank(value)) return null;
        // Strip common currency symbols and thousand separators
        String cleaned = value.trim()
                .replace("$", "")
                .replace("€", "")
                .replace("£", "")
                .replace("¥", "")
                .replace(",", "");
        return new BigDecimal(cleaned);
    }

    @Override
    public Class<BigDecimal> getTargetType() {
        return BigDecimal.class;
    }
}
