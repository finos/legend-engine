// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.shared.core.operational.prometheus;

import io.prometheus.client.CollectorRegistry;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.junit.After;
import org.junit.Test;
import java.util.UnknownFormatFlagsException;
import static org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler.toCamelCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TestErrorManagement
{
    private final String[] COUNTER_LABEL_NAMES = {"exceptionClass", "category", "source", "serviceName"};
    private final String METRIC_NAME = "legend_engine_error_total";
    private final CollectorRegistry METRIC_REGISTRY = MetricsHandler.getMetricsRegistry();
    private final double DELTA = 0d;
    private final String TEST_SERVICE_PATH = "service/remote/getRegistry";

    @After()
    public void clearCounterData()
    {
        MetricsHandler.EXCEPTION_ERROR_COUNTER.clear();
    }

    @Test
    public void testServiceErrorSourceLabel()
    {
        MetricsHandler.observeError(LoggingEventType.SERVICE_TEST_EXECUTE_ERROR, new Exception(), TEST_SERVICE_PATH);
        String[] labels = {"Exception", "UnknownError", "ServiceTestExecute", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorSourceLabel()
    {
        MetricsHandler.observeError(LoggingEventType.SERVICE_TEST_EXECUTE_ERROR, new Exception(), null);
        String[] labels = {"Exception", "UnknownError", "ServiceTestExecute", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testErrorWithoutSource()
    {
        Exception exception = assertThrows(EngineException.class, () ->
                MetricsHandler.observeError(null, new Exception(), TEST_SERVICE_PATH));
        assertEquals(exception.getMessage(), "Exception origin must not be null!");
    }

    @Test
    public void testInteractiveExceptionClassExtractionWithNonGenericException()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new ArithmeticException(), null);
        String[] labels = {"ArithmeticException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionClassExtractionWithEngineExceptionWithType()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException(null, null, EngineErrorType.COMPILATION), null);
        String[] labels = {"CompilationEngineException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionClassExtractionWithEngineExceptionWithoutType()
    {
        MetricsHandler.observeError(LoggingEventType.COMPILE_MODEL_ERROR, new EngineException(null), null);
        String[] labels = {"EngineException", "UnknownError", "CompileModel", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionClassExtractionWithEngineExceptionWithoutTypeWithoutSource()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException(null), null);
        String[] labels = {"EngineException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceExceptionClassExtractionWithRuntimeExceptionWithCauseThatMatches()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new RuntimeException(new ArithmeticException("tests")), TEST_SERVICE_PATH);
        String[] labels = {"RuntimeException", "InternalServerError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionClassExtractionWithRuntimeExceptionWithoutCause()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new RuntimeException(), null);
        String[] labels = {"RuntimeException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionClassExtractionWithNestedRuntimeException()
    {
        RuntimeException nestedOtherErrorException = new RuntimeException(new RuntimeException());
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception(nestedOtherErrorException), null);
        String[] labels = {"Exception", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionClassExtractionWithNestedEngineException()
    {
        RuntimeException nestedOtherErrorException = new RuntimeException(new EngineException(""));
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception(nestedOtherErrorException), null);
        String[] labels = {"Exception", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionClassExtractionWithCrossCausingExceptionsOnFiveLoopIterations()
    {
        Exception exceptionOne = new Exception();
        RuntimeException exceptionTwo = new RuntimeException(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, exceptionOne, null);
        String[] labels = {"Exception", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionClassExtractionWithLoopingExceptionCauseOnFiveLoopIterations()
    {
        Exception exceptionOne = new Exception();
        RuntimeException exceptionTwo = new RuntimeException();
        Exception exceptionThree = new Exception(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        exceptionTwo.initCause(exceptionThree);
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, exceptionTwo, null);
        String[] labels = {"RuntimeException", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionCategorizationWithCrossCausingExceptions()
    {
        Exception exceptionOne = new Exception();
        Exception exceptionTwo = new Exception(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, exceptionOne, null);
        String[] labels = {"Exception", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionCategorizationWithLoopingExceptionCause()
    {
        Exception exceptionOne = new Exception();
        Exception exceptionTwo = new Exception();
        Exception exceptionThree = new Exception(exceptionOne);
        exceptionOne.initCause(exceptionTwo);
        exceptionTwo.initCause(exceptionThree);
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, exceptionOne, null);
        String[] labels = {"Exception", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionCategorizationWithNestedUniqueException()
    {
        RuntimeException nestedOtherErrorException = new RuntimeException(new java.net.SocketTimeoutException("socket timeout"));
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception(nestedOtherErrorException), null);
        String[] labels = {"Exception", "InternalServerError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionCategorizationToUserCredentialsErrorWithExceptionOutlineMatching()
    {
        UnsupportedOperationException permissionsError = new UnsupportedOperationException("credentials issues");
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, permissionsError, null);
        String[] labels = {"UnsupportedOperationException", "UserCredentialsError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceExceptionCategorizationToUserCredentialsErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("some text including kerberos keyword"), TEST_SERVICE_PATH);
        String[] labels = {"Exception", "UserCredentialsError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceExceptionCategorizationToUserExecutionErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("some text including kerberos keyword"), TEST_SERVICE_PATH);
        String[] labels = {"Exception", "UserCredentialsError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceExceptionCategorizationToInternalServerErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new RuntimeException("something has not been configured properly"), TEST_SERVICE_PATH);
        String[] labels = {"RuntimeException", "InternalServerError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionCategorizationToInternalServerErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("overloaded servers"), null);
        String[] labels = {"Exception", "InternalServerError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionCategorizationToServerExecutionErrorWithExceptionOutlineMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new RuntimeException("Input stream was not provided"), null);
        String[] labels = {"RuntimeException", "ServerExecutionError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);

        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException("Error in 'some::graph': Can't find the profile 'some::profile'"), null);
        labels = new String[]{"EngineException", "ServerExecutionError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceExceptionCategorizationToServerExecutionErrorWithKeywordsMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("Error in 'some::graph': Couldn't resolve something"), TEST_SERVICE_PATH);
        String[] labels = {"Exception", "ServerExecutionError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceExceptionCategorizationInternalServerErrorWithExceptionOutlineMatchingInTestScenario()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new IllegalArgumentException("Tests Failed! Error running tests for service 'some/service'"), TEST_SERVICE_PATH);
        String[] labels = {"IllegalArgumentException", "InternalServerError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionCategorizationToInternalServerErrorWithKeywordsMatchingInExceptionMessageInTestScenario()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("some tests have failed!"), null);
        String[] labels = {"Exception", "InternalServerError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceExceptionCategorizationToUnknownErrorByAnyMatching()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new UnknownFormatFlagsException("some unknown error"), TEST_SERVICE_PATH);
        String[] labels = {"UnknownFormatFlagsException", "UnknownError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceExceptionCategorizationMatchingMethodPrioritizationOfExceptionOutlineToKeywords()
    {
        MetricsHandler.observeError(LoggingEventType.DSB_EXECUTE_ERROR, new EngineException("Can't resolve the builder for function 'get/Login/Kerberos"), TEST_SERVICE_PATH);
        String[] labels = {"EngineException", "ServerExecutionError", "DsbExecute", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testEnumToUserFriendlyStringConversion()
    {
        assertEquals(toCamelCase(LoggingEventType.PURE_QUERY_EXECUTE_ERROR), "PureQueryExecuteError");
        assertEquals(toCamelCase(LoggingEventType.GENERATE_PLAN_ERROR), "GeneratePlanError");
        assertEquals(toCamelCase(LoggingEventType.CATCH_ALL), "CatchAll");
        assertEquals(toCamelCase(ExceptionCategory.USER_CREDENTIALS_ERROR), "UserCredentialsError");
        assertEquals(toCamelCase(ExceptionCategory.UNKNOWN_ERROR), "UnknownError");
    }

    @Test
    public void testInteractiveCorrectCounterSampleIncrementation()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception(), null);
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception(), null);
        String[] labels = {"Exception", "UnknownError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 2, DELTA);
    }

    @Test
    public void testServiceExceptionCategorizationExtractingValidCategoryFromEngineException()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException("some message", ExceptionCategory.INTERNAL_SERVER_ERROR), TEST_SERVICE_PATH);
        String[] labels = {"EngineException", "InternalServerError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceExceptionCategorizationExtractingUnknownCategoryFromEngineException()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException("Error in 'some::graph': Couldn't resolve test", ExceptionCategory.UNKNOWN_ERROR), TEST_SERVICE_PATH);
        String[] labels = {"EngineException", "ServerExecutionError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceExceptionCategorizationExtractingNullCategoryFromEngineException()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException("Error in 'some::graph': Couldn't resolve test", (ExceptionCategory) null), TEST_SERVICE_PATH);
        String[] labels = {"EngineException", "ServerExecutionError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceExceptionCategorizationExtractingValidCategoryFromNestedEngineException()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception(new EngineException("some message", ExceptionCategory.INTERNAL_SERVER_ERROR)), TEST_SERVICE_PATH);
        String[] labels = {"EngineException", "InternalServerError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceExceptionCategorizationTechniquePrioritizationOfFirstMatchWithExceptionFile()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("kerberos", new EngineException("some message", ExceptionCategory.INTERNAL_SERVER_ERROR)), TEST_SERVICE_PATH);
        String[] labels = {"Exception", "UserCredentialsError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceExceptionCategorizationTechniquePrioritizationOfFirstMatchWithEngineException()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new EngineException("some message", new Exception("kerberos"), ExceptionCategory.INTERNAL_SERVER_ERROR), TEST_SERVICE_PATH);
        String[] labels = {"EngineException", "InternalServerError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testServiceExceptionCategorizationRegexCaseInsensitivity()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("KERBEROS"), TEST_SERVICE_PATH);
        String[] labels = {"Exception", "UserCredentialsError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    private enum ErrorOrigin
    {
        ERROR_MANAGEMENT_TEST_ERROR
    }

    @Test
    public void testErrorOriginExtensibilityBeyondLoggingEventType()
    {
        MetricsHandler.observeError(ErrorOrigin.ERROR_MANAGEMENT_TEST_ERROR, new Exception("KERBEROS"), TEST_SERVICE_PATH);
        String[] labels = {"Exception", "UserCredentialsError", "ErrorManagementTest", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testOnlyFullMatchingForExceptionName()
    {
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, new Exception("Can't find database"), TEST_SERVICE_PATH);
        String[] labels = {"Exception", "UnknownError", "CatchAll", TEST_SERVICE_PATH};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveExceptionCategorizationDownCauseTrace()
    {
        Exception exception = new Exception(new EngineException("", new RuntimeException("Issue processing freemarker function")));
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, exception, null);
        String[] labels = {"RuntimeException", "ServerExecutionError", "CatchAll", "N/A"};
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    @Test
    public void testInteractiveErrorInToggledCategorisationMode()
    {
        MetricsHandler.setCategorisationEnabled(false);
        Exception exception = new Exception(new EngineException("", new RuntimeException("Issue processing freemarker function")));
        MetricsHandler.observeError(LoggingEventType.CATCH_ALL, exception, null);
        String[] labels = {"Exception", "UnknownError", "CatchAll", "N/A"};
        MetricsHandler.setCategorisationEnabled(true);
        assertEquals(METRIC_REGISTRY.getSampleValue(METRIC_NAME, COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

}
