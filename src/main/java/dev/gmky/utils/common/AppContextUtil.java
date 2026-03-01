package dev.gmky.utils.common;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;

/**
 * Utility class for accessing Spring's {@link ApplicationContext} statically.
 * <p>
 * This class implements {@link ApplicationContextAware} to hold a static reference
 * to the Spring application context, allowing static access to beans and properties
 * from non-managed objects.
 * </p>
 * <p>
 * Registered as a bean by {@code GmkyAutoConfiguration}. Do not annotate with
 * {@code @Component} — bean registration is handled explicitly to avoid
 * unintentional component scanning in library consumers.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.0
 */
@SuppressWarnings("squid:S2696") // static field write in non-static method is intentional for ApplicationContextAware
public class AppContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * Retrieves a bean from the application context by its class.
     *
     * @param beanClass the class of the bean to retrieve
     * @param <T>       the type of the bean
     * @return the bean instance
     * @throws IllegalStateException if the application context has not been set yet
     */
    public static <T> T getBean(Class<T> beanClass) {
        assertContextAvailable();
        return applicationContext.getBean(beanClass);
    }

    /**
     * Retrieves a bean from the application context by its name and class.
     *
     * @param beanName  the name of the bean to retrieve
     * @param beanClass the class of the bean to retrieve
     * @param <T>       the type of the bean
     * @return the bean instance
     * @throws IllegalStateException if the application context has not been set yet
     */
    public static <T> T getBean(String beanName, Class<T> beanClass) {
        assertContextAvailable();
        return applicationContext.getBean(beanName, beanClass);
    }

    /**
     * Retrieves a property value from the application environment.
     *
     * @param key the property key
     * @return the property value, or null if not found
     * @throws IllegalStateException if the application context has not been set yet
     */
    public static String getProperty(String key) {
        assertContextAvailable();
        Environment env = applicationContext.getEnvironment();
        return env.getProperty(key);
    }

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        AppContextUtil.applicationContext = appContext;
    }

    private static void assertContextAvailable() {
        if (applicationContext == null) {
            throw new IllegalStateException(
                    "ApplicationContext has not been set. Ensure AppContextUtil is registered as a Spring bean " +
                    "and the application context is fully initialized before calling this method.");
        }
    }
}
