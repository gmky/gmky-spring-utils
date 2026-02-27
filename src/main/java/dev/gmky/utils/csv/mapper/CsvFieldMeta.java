package dev.gmky.utils.csv.mapper;

import dev.gmky.utils.csv.annotation.CsvColumn;
import dev.gmky.utils.csv.annotation.CsvDateFormat;
import dev.gmky.utils.csv.converter.TypeConverter;
import lombok.Getter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * Immutable metadata about a DTO field that is mapped from a CSV column.
 * <p>
 * Built once per field during DTO class introspection and cached for reuse.
 * Uses a {@link MethodHandle} for writing field values instead of {@link Field#set(Object, Object)}
 * to benefit from JIT optimization in hot mapping loops.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 */
@Getter
public class CsvFieldMeta {

    private final String fieldName;
    private final Class<?> fieldType;
    private final String columnName;  // header name (may be empty if index-based)
    private final int columnIndex;    // -1 if header-based
    private final boolean required;
    private final String defaultValue;
    private final TypeConverter<?> customConverter;  // may be null (use registry)
    private final String dateFormatPattern;
    private final String dateFormatTimezone;
    private final java.time.format.DateTimeFormatter dateTimeFormatter;
    private final MethodHandle setter;

    public CsvFieldMeta(Field field, CsvColumn column, CsvDateFormat dateFormat) {
        this.fieldName = field.getName();
        this.fieldType = field.getType();
        this.columnName = column.value().isBlank() ? field.getName() : column.value();
        this.columnIndex = column.index();
        this.required = column.required();
        this.defaultValue = column.defaultValue();
        this.dateFormatPattern = dateFormat != null ? dateFormat.pattern() : "yyyy-MM-dd";
        this.dateFormatTimezone = dateFormat != null ? dateFormat.timezone() : "";
        
        java.time.format.DateTimeFormatter formatter = org.apache.commons.lang3.StringUtils.isBlank(this.dateFormatPattern)
                ? java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
                : java.time.format.DateTimeFormatter.ofPattern(this.dateFormatPattern);
                
        if (org.apache.commons.lang3.StringUtils.isNotBlank(this.dateFormatTimezone)) {
            formatter = formatter.withZone(java.time.ZoneId.of(this.dateFormatTimezone));
        }
        this.dateTimeFormatter = formatter;

        // Resolve custom converter if specified (not the marker interface itself)
        TypeConverter<?> resolvedConverter = null;
        Class<?> converterClass = column.converter();
        if (converterClass != TypeConverter.class) {
            try {
                resolvedConverter = (TypeConverter<?>) converterClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to instantiate converter: " + converterClass.getName(), e);
            }
        }
        this.customConverter = resolvedConverter;

        // Build MethodHandle for fast field write
        try {
            field.setAccessible(true);
            this.setter = MethodHandles.lookup().unreflectSetter(field);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot create MethodHandle for field: " + field.getName(), e);
        }
    }

    /**
     * Writes the given value to the target object's field using the cached MethodHandle.
     *
     * @param target the DTO instance
     * @param value  the value to write
     */
    public void setValue(Object target, Object value) {
        try {
            setter.invoke(target, value);
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "Failed to set field [" + fieldName + "] on " + target.getClass().getSimpleName(), e);
        }
    }

    /**
     * Returns true if this field uses index-based column binding.
     */
    public boolean isIndexBased() {
        return columnIndex >= 0;
    }
}
