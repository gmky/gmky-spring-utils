package dev.gmky.utils.csv.converter;

import dev.gmky.utils.csv.mapper.CsvFieldMeta;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for {@link TypeConverter} instances, keyed by target type.
 * <p>
 * Built-in converters are pre-registered. Add custom converters via {@link #register(TypeConverter)}.
 * The registry is thread-safe and converters are singletons.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.3
 */
public class TypeConverterRegistry {

    private final Map<Class<?>, TypeConverter<?>> converters = new ConcurrentHashMap<>();

    /**
     * Registers a new converter, replacing any existing one for the same target type.
     *
     * @param converter the converter to register
     */
    public void register(TypeConverter<?> converter) {
        converters.put(converter.getTargetType(), converter);
    }

    /**
     * Registers a converter under an explicit target type, regardless of what
     * {@link TypeConverter#getTargetType()} returns. Use this to register
     * multi-type converters (e.g. {@code NumberConverter}) under each concrete type.
     *
     * @param type      the target type key
     * @param converter the converter to register
     */
    public void register(Class<?> type, TypeConverter<?> converter) {
        converters.put(type, converter);
    }

    /**
     * Finds a converter for the given type using a three-step fallback:
     * <ol>
     *   <li>Exact match in registry</li>
     *   <li>If {@code type.isEnum()}, returns the registered {@code Enum.class} converter</li>
     *   <li>Returns {@code null} if no converter is found</li>
     * </ol>
     *
     * @param type the target type to find a converter for
     * @return the converter, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> TypeConverter<T> findConverter(Class<T> type) {
        TypeConverter<T> converter = (TypeConverter<T>) converters.get(type);
        if (converter != null) return converter;
        if (type.isEnum()) return (TypeConverter<T>) converters.get(Enum.class);
        return null;
    }

    /**
     * Retrieves the converter for the given target type, or {@code null} if not found.
     *
     * @param targetType the Java type to convert to
     * @return the converter, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> TypeConverter<T> get(Class<T> targetType) {
        return (TypeConverter<T>) converters.get(targetType);
    }

    /**
     * Converts a raw string value to the specified target type using the registered converter.
     *
     * @param value      the raw CSV cell value
     * @param targetType the target type to convert to
     * @param meta       field metadata
     * @return the converted value
     * @throws Exception if no converter is found or conversion fails
     */
    @SuppressWarnings("unchecked")
    public <T> T convert(String value, Class<T> targetType, CsvFieldMeta meta) throws Exception {
        TypeConverter<T> converter = (TypeConverter<T>) converters.get(targetType);
        if (converter == null) {
            throw new IllegalStateException("No TypeConverter registered for type: " + targetType.getName());
        }
        return converter.convert(value, meta);
    }

    /**
     * Returns true if a converter is registered for the given type.
     */
    public boolean hasConverter(Class<?> targetType) {
        return converters.containsKey(targetType);
    }
}
