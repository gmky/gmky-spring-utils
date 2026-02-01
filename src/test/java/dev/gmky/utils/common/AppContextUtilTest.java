package dev.gmky.utils.common;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppContextUtilTest {

    @Test
    void getBean_ShouldReturnBean_WhenContextIsSet() {
        ApplicationContext context = mock(ApplicationContext.class);
        AppContextUtil util = new AppContextUtil();
        util.setApplicationContext(context);

        Object mockBean = new Object();
        when(context.getBean(Object.class)).thenReturn(mockBean);

        Object result = AppContextUtil.getBean(Object.class);

        assertEquals(mockBean, result);
    }

    @Test
    void getBean_ByName_ShouldReturnBean_WhenContextIsSet() {
        ApplicationContext context = mock(ApplicationContext.class);
        AppContextUtil util = new AppContextUtil();
        util.setApplicationContext(context);

        Object mockBean = new Object();
        when(context.getBean("testBean", Object.class)).thenReturn(mockBean);

        Object result = AppContextUtil.getBean("testBean", Object.class);

        assertEquals(mockBean, result);
    }

    @Test
    void getProperty_ShouldReturnProperty_WhenContextIsSet() {
        ApplicationContext context = mock(ApplicationContext.class);
        Environment env = mock(Environment.class);
        when(context.getEnvironment()).thenReturn(env);
        when(env.getProperty("test.prop")).thenReturn("value");

        AppContextUtil util = new AppContextUtil();
        util.setApplicationContext(context);

        String result = AppContextUtil.getProperty("test.prop");

        assertEquals("value", result);
    }
}
