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

    // Singleton converter instances — declared before DEFAULT_REGISTRY to ensure correct init order
    private static final TemporalConverter TEMPORAL_CONVERTER = new TemporalConverter();
    private static final EnumConverter ENUM_CONVERTER = new EnumConverter();
    private static final NumberConverter NUMBER_CONVERTER = new NumberConverter();

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

        // 2. String shortcut — no registry lookup needed
        if (type == String.class) {
            return value;
        }

        // 3. Registry lookup (covers all built-in types registered in CsvAutoConfiguration
        //    or in buildDefaultRegistry(), including numeric, temporal, enum, boolean, BigDecimal)
        TypeConverter<?> converter = registry.findConverter(type);
        if (converter != null) {
            return converter.convert(value, meta);
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
        reg.register(new BigDecimalConverter());
        reg.register(ENUM_CONVERTER); // registered under Enum.class for isEnum() fallback

        // Register Boolean under both boxed and primitive
        BooleanConverter boolConverter = new BooleanConverter();
        reg.register(boolConverter);
        reg.register(boolean.class, boolConverter);

        // Register NumberConverter under each concrete numeric type
        reg.register(Integer.class, NUMBER_CONVERTER);
        reg.register(int.class, NUMBER_CONVERTER);
        reg.register(Long.class, NUMBER_CONVERTER);
        reg.register(long.class, NUMBER_CONVERTER);
        reg.register(Double.class, NUMBER_CONVERTER);
        reg.register(double.class, NUMBER_CONVERTER);
        reg.register(Float.class, NUMBER_CONVERTER);
        reg.register(float.class, NUMBER_CONVERTER);
        reg.register(Short.class, NUMBER_CONVERTER);
        reg.register(short.class, NUMBER_CONVERTER);
        reg.register(Byte.class, NUMBER_CONVERTER);
        reg.register(byte.class, NUMBER_CONVERTER);

        // Register TemporalConverter under each concrete temporal type
        reg.register(java.time.LocalDate.class, TEMPORAL_CONVERTER);
        reg.register(java.time.LocalDateTime.class, TEMPORAL_CONVERTER);
        reg.register(java.time.LocalTime.class, TEMPORAL_CONVERTER);
        reg.register(java.time.ZonedDateTime.class, TEMPORAL_CONVERTER);
        reg.register(java.time.Instant.class, TEMPORAL_CONVERTER);

        return reg;
    }
}
