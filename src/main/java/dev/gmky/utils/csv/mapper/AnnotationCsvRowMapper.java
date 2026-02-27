package dev.gmky.utils.csv.mapper;

import dev.gmky.utils.csv.annotation.CsvColumn;
import dev.gmky.utils.csv.annotation.CsvDateFormat;
import dev.gmky.utils.csv.annotation.CsvIgnore;
import dev.gmky.utils.csv.converter.TypeConverter;
import dev.gmky.utils.csv.converter.TypeConverterRegistry;
import dev.gmky.utils.csv.converter.impl.*;
import dev.gmky.utils.csv.exception.CsvMappingException;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link CsvRowMapper} implementation that uses reflection metadata
 * cached in {@link CsvFieldMeta} and the {@link TypeConverterRegistry} to map
 * CSV rows to DTOs via annotation-driven binding.
 * <p>
 * Metadata is computed once per DTO class and stored in a static
 * {@link ConcurrentHashMap} for the lifetime of the JVM.
 * </p>
 *
 * @param <T> the target DTO type
 * @author HiepVH
 * @since 1.0.3
 */
@SuppressWarnings("unchecked")
public class AnnotationCsvRowMapper<T> implements CsvRowMapper<T> {

    private static final ConcurrentHashMap<Class<?>, List<CsvFieldMeta>> METADATA_CACHE =
            new ConcurrentHashMap<>();

    private static final TypeConverterRegistry DEFAULT_REGISTRY = buildDefaultRegistry();

    private final Class<T> targetType;
    private final List<CsvFieldMeta> fieldMetas;
    private final Map<String, Integer> headerIndexMap; // header name -> column index
    private final TypeConverterRegistry registry;

    /**
     * Creates a mapper for the given DTO type with header-based binding.
     *
     * @param targetType the DTO class
     * @param headers    the CSV header row (may be null for index-based binding)
     */
    public AnnotationCsvRowMapper(Class<T> targetType, String[] headers) {
        this(targetType, headers, DEFAULT_REGISTRY);
    }

    /**
     * Creates a mapper with a custom converter registry.
     */
    public AnnotationCsvRowMapper(Class<T> targetType, String[] headers, TypeConverterRegistry registry) {
        this.targetType = targetType;
        this.registry = registry;
        this.fieldMetas = METADATA_CACHE.computeIfAbsent(targetType,
                AnnotationCsvRowMapper::introspect);
        this.headerIndexMap = buildHeaderIndexMap(headers);
    }

    @Override
    public T map(String[] columns, String[] headers, long lineNumber) throws Exception {
        T instance;
        try {
            var constructor = targetType.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        } catch (Exception e) {
            throw new CsvMappingException(lineNumber, targetType.getSimpleName(), "",
                    "Cannot instantiate DTO. Ensure a no-arg constructor exists.", e);
        }

        for (CsvFieldMeta meta : fieldMetas) {
            int colIdx = resolveColumnIndex(meta);
            if (colIdx < 0 || colIdx >= columns.length) {
                if (meta.isRequired()) {
                    throw new CsvMappingException(lineNumber, meta.getFieldName(), "",
                            "Required column '" + meta.getColumnName() + "' not found");
                }
                continue;
            }

            String rawValue = columns[colIdx];
            if (StringUtils.isBlank(rawValue)) {
                rawValue = meta.getDefaultValue();
            }
            if (StringUtils.isBlank(rawValue)) {
                if (meta.isRequired()) {
                    throw new CsvMappingException(lineNumber, meta.getFieldName(), rawValue,
                            "Required field is blank");
                }
                continue;
            }

            try {
                Object converted = convertValue(rawValue, meta);
                meta.setValue(instance, converted);
            } catch (CsvMappingException e) {
                throw e;
            } catch (Exception e) {
                throw new CsvMappingException(lineNumber, meta.getFieldName(), rawValue,
                        "Conversion failed: " + e.getMessage(), e);
            }
        }

        return instance;
    }

    private int resolveColumnIndex(CsvFieldMeta meta) {
        if (meta.isIndexBased()) {
            return meta.getColumnIndex();
        }
        return headerIndexMap.getOrDefault(meta.getColumnName().trim().toLowerCase(), -1);
    }

    private Object convertValue(String value, CsvFieldMeta meta) throws Exception {
        // 1. Custom per-field converter takes highest priority
        TypeConverter<?> custom = meta.getCustomConverter();
        if (custom != null) {
            return custom.convert(value, meta);
        }

        Class<?> type = meta.getFieldType();

        // 2. String shortcut
        if (type == String.class) {
            return value;
        }

        // 3. BigDecimal (must be before generic Number check since BigDecimal extends Number)
        if (type == BigDecimal.class) {
            return new BigDecimalConverter().convert(value, meta);
        }

        // 4. Temporal types (multi-type converter)
        if (TemporalConverter.supports(type)) {
            return new TemporalConverter().convert(value, meta);
        }

        // 5. Enum types
        if (type.isEnum()) {
            return new EnumConverter().convert(value, meta);
        }

        // 6. Primitive numeric and boxed Integer/Long/Double/Float/Short/Byte
        if (type == Integer.class || type == int.class
                || type == Long.class || type == long.class
                || type == Double.class || type == double.class
                || type == Float.class || type == float.class
                || type == Short.class || type == short.class
                || type == Byte.class || type == byte.class) {
            return new NumberConverter().convert(value, meta);
        }

        // 7. Boolean
        if (type == Boolean.class || type == boolean.class) {
            return new BooleanConverter().convert(value, meta);
        }

        // 8. Registry lookup for custom types
        if (registry.hasConverter(type)) {
            return registry.convert(value, (Class<Object>) type, meta);
        }

        throw new IllegalStateException("No converter found for type: " + type.getName());
    }

    private static Map<String, Integer> buildHeaderIndexMap(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        if (headers != null) {
            for (int i = 0; i < headers.length; i++) {
                if (headers[i] != null) {
                    map.put(headers[i].trim().toLowerCase(), i);
                }
            }
        }
        return map;
    }

    private static List<CsvFieldMeta> introspect(Class<?> clazz) {
        List<CsvFieldMeta> metas = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(CsvIgnore.class)) continue;
            CsvColumn column = field.getAnnotation(CsvColumn.class);
            if (column == null) continue;
            CsvDateFormat dateFormat = field.getAnnotation(CsvDateFormat.class);
            metas.add(new CsvFieldMeta(field, column, dateFormat));
        }
        return metas;
    }

    private static TypeConverterRegistry buildDefaultRegistry() {
        TypeConverterRegistry reg = new TypeConverterRegistry();
        reg.register(new StringConverter());
        reg.register(new BooleanConverter());
        reg.register(new BigDecimalConverter());
        return reg;
    }
}
