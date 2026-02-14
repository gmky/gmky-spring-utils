package dev.gmky.utils.batch.core.processor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AbstractDynamicProcessor.
 */
class AbstractDynamicProcessorTest {

    @Test
    void testBasicTransformation() throws Exception {
        TestProcessor processor = new TestProcessor();

        String result = processor.process("input");

        assertEquals("TRANSFORMED: input", result);
    }

    @Test
    void testPreProcessFilter() throws Exception {
        TestProcessor processor = new TestProcessor();
        processor.setSkipItem(true);

        String result = processor.process("input");

        assertNull(result);
    }

    @Test
    void testValidationFailure() throws Exception {
        TestProcessor processor = new TestProcessor();
        processor.setInvalidItem(true);

        String result = processor.process("input");

        assertNull(result);
        assertTrue(processor.isValidationFailureHandled());
    }

    @Test
    void testPostProcessing() throws Exception {
        TestProcessor processor = new TestProcessor();
        processor.setApplyPostProcess(true);

        String result = processor.process("input");

        assertEquals("POST: TRANSFORMED: input", result);
    }

    @Test
    void testFullPipeline() throws Exception {
        TestProcessor processor = new TestProcessor();
        processor.setApplyPostProcess(true);

        String result = processor.process("test");

        // Should go through: preProcess -> validate -> transform -> postProcess
        assertEquals("POST: TRANSFORMED: test", result);
        assertTrue(processor.isPreProcessCalled());
        assertTrue(processor.isValidateCalled());
    }

    @Test
    void testNullInputHandling() throws Exception {
        TestProcessor processor = new TestProcessor();

        String result = processor.process(null);

        assertEquals("TRANSFORMED: null", result);
    }

    @Test
    void testDefaultHooks() throws Exception {
        MinimalProcessor processor = new MinimalProcessor();
        String result = processor.process("test");

        assertEquals("test", result);
    }

    /**
     * Minimal implementation to test default hooks.
     */
    static class MinimalProcessor extends AbstractDynamicProcessor<String, String> {
        @Override
        protected String transform(String item) {
            return item;
        }
    }

    /**
     * Test implementation of AbstractDynamicProcessor.
     */
    static class TestProcessor extends AbstractDynamicProcessor<String, String> {

        private boolean skipItem = false;
        private boolean invalidItem = false;
        private boolean applyPostProcess = false;
        private boolean validationFailureHandled = false;
        private boolean preProcessCalled = false;
        private boolean validateCalled = false;

        @Override
        protected String transform(String item) throws Exception {
            return "TRANSFORMED: " + item;
        }

        @Override
        protected boolean preProcess(String item) {
            preProcessCalled = true;
            return !skipItem;
        }

        @Override
        protected boolean validate(String item) {
            validateCalled = true;
            return !invalidItem;
        }

        @Override
        protected String postProcess(String item) {
            if (applyPostProcess) {
                return "POST: " + item;
            }
            return item;
        }

        @Override
        protected void handleValidationFailure(String item) {
            validationFailureHandled = true;
        }

        // Test helper methods
        void setSkipItem(boolean skipItem) {
            this.skipItem = skipItem;
        }

        void setInvalidItem(boolean invalidItem) {
            this.invalidItem = invalidItem;
        }

        void setApplyPostProcess(boolean applyPostProcess) {
            this.applyPostProcess = applyPostProcess;
        }

        boolean isValidationFailureHandled() {
            return validationFailureHandled;
        }

        boolean isPreProcessCalled() {
            return preProcessCalled;
        }

        boolean isValidateCalled() {
            return validateCalled;
        }
    }
}
