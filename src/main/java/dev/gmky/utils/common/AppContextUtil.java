package dev.gmky.utils.common;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Utility class for accessing Spring's {@link ApplicationContext} statically.
 * <p>
 * This class implements {@link ApplicationContextAware} to hold a static reference
 * to the Spring application context, allowing static access to beans and properties
 * from non-managed objects.
 * </p>
 *
 * @author HiepVH
 * @since 1.0.0
 */
@Component
@SuppressWarnings("all")
public class AppContextUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    /**
     * Retrieves a bean from the application context by its class.
     *
     * @param beanClass the class of the bean to retrieve
     * @param <T>       the type of the bean
     * @return the bean instance
     * @throws BeansException if the bean could not be created or found
     */
    public static <T> T getBean(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }

    /**
     * Retrieves a bean from the application context by its name and class.
     *
     * @param beanName  the name of the bean to retrieve
     * @param beanClass the class of the bean to retrieve
     * @param <T>       the type of the bean
     * @return the bean instance
     * @throws BeansException if the bean could not be created or found
     */
    public static <T> T getBean(String beanName, Class<T> beanClass) {
        return applicationContext.getBean(beanName, beanClass);
    }

    /**
     * Retrieves a property value from the application environment.
     *
     * @param key the property key
     * @return the property value, or null if not found
     */
    public static String getProperty(String key) {
        Environment env = applicationContext.getEnvironment();
        return env.getProperty(key);
    }

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        this.applicationContext = appContext;
    }
}
