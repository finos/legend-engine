// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.pure.repl.core;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.execution.test.TestRunner;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ReplEngine provides methods for evaluating Pure expressions, running tests,
 * and introspecting types.
 */
public class ReplEngine
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplEngine.class);
    // Use scratch repository for REPL expressions - it has visibility to all other repositories
    // (platform repo can only see meta/system/apps::pure packages, which is too restrictive)
    private static final String REPL_SOURCE_ID = "/scratch/pure/repl_expression.pure";
    private static final String EXPRESSION_WRAPPER_TEMPLATE = "function meta::pure::repl::__repl__eval__():Any[*] { %s }";

    private final ReplSession session;
    private final ExecutorService executorService;
    private long timeoutMs = 30000; // Default 30 seconds

    public ReplEngine(ReplSession session)
    {
        this.session = session;
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Sets the execution timeout in milliseconds.
     */
    public void setTimeout(long timeoutMs)
    {
        this.timeoutMs = timeoutMs;
    }

    /**
     * Gets the current timeout in milliseconds.
     */
    public long getTimeout()
    {
        return this.timeoutMs;
    }

    /**
     * Evaluates a Pure expression and returns the result.
     *
     * @param expression the Pure expression to evaluate
     * @return the evaluation result
     * @throws ReplException if evaluation fails
     */
    public EvaluationResult evaluate(String expression) throws ReplException
    {
        if (expression == null || expression.trim().isEmpty())
        {
            throw new ReplException("Expression cannot be empty");
        }

        EvaluationResult.Builder resultBuilder = new EvaluationResult.Builder()
                .expression(expression);

        long startTime = System.currentTimeMillis();

        try
        {
            // Parse and compile the expression
            long parseStart = System.currentTimeMillis();
            String wrappedExpression = String.format(EXPRESSION_WRAPPER_TEMPLATE, expression);

            PureRuntime runtime = session.getPureRuntime();

            // Remove previous REPL expression if exists
            if (runtime.getSourceById(REPL_SOURCE_ID) != null)
            {
                runtime.delete(REPL_SOURCE_ID);
            }

            // Create and compile the new expression
            runtime.createInMemorySource(REPL_SOURCE_ID, wrappedExpression);
            long parseMs = System.currentTimeMillis() - parseStart;
            resultBuilder.parseMs(parseMs);

            long compileStart = System.currentTimeMillis();
            runtime.compile();
            long compileMs = System.currentTimeMillis() - compileStart;
            resultBuilder.compileMs(compileMs);

            // Get the function and execute it
            CoreInstance function = runtime.getFunction("meta::pure::repl::__repl__eval__():Any[*]");
            if (function == null)
            {
                throw new ReplException("Failed to compile expression");
            }

            // Get the return type
            ProcessorSupport processorSupport = runtime.getProcessorSupport();
            CoreInstance functionType = function.getValueForMetaPropertyToOne(M3Properties.classifierGenericType)
                    .getValueForMetaPropertyToOne(M3Properties.typeArguments)
                    .getValueForMetaPropertyToOne(M3Properties.rawType);
            CoreInstance returnType = functionType.getValueForMetaPropertyToOne(M3Properties.returnType);
            CoreInstance returnMultiplicity = functionType.getValueForMetaPropertyToOne(M3Properties.returnMultiplicity);

            String typeString = buildTypeString(returnType, returnMultiplicity, processorSupport);
            resultBuilder.type(typeString);

            // Execute the function
            long executeStart = System.currentTimeMillis();
            FunctionExecution functionExecution = session.getFunctionExecution();

            // Capture console output
            ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
            PrintStream consolePrintStream = new PrintStream(consoleOutput);
            Console console = functionExecution.getConsole();

            try
            {
                console.setPrintStream(consolePrintStream);
                console.setConsole(true);

                CoreInstance result = functionExecution.start(function, FastList.newList());

                long executeMs = System.currentTimeMillis() - executeStart;
                resultBuilder.executeMs(executeMs);

                // Format the result
                String resultValue = formatResult(result, processorSupport);
                resultBuilder.result(resultValue);

                // Add console output
                String consoleOutputStr = consoleOutput.toString();
                if (!consoleOutputStr.isEmpty())
                {
                    resultBuilder.consoleOutput(consoleOutputStr);
                }

                return resultBuilder.success().build();
            }
            finally
            {
                // Reset console to a new stream (we don't have access to the original)
                console.setPrintStream(new PrintStream(new ByteArrayOutputStream()));
                console.setConsole(false);
            }
        }
        catch (PureException e)
        {
            return resultBuilder
                    .error()
                    .errorMessage(e.getMessage())
                    .errorType(e.getClass().getSimpleName())
                    .sourceInfo(e.getSourceInformation())
                    .build();
        }
        catch (Exception e)
        {
            return resultBuilder
                    .error()
                    .errorMessage(e.getMessage())
                    .errorType(e.getClass().getSimpleName())
                    .build();
        }
    }

    /**
     * Gets the type of an expression without executing it.
     *
     * @param expression the Pure expression
     * @return the type string
     * @throws ReplException if type inference fails
     */
    public String getExpressionType(String expression) throws ReplException
    {
        if (expression == null || expression.trim().isEmpty())
        {
            throw new ReplException("Expression cannot be empty");
        }

        try
        {
            String wrappedExpression = String.format(EXPRESSION_WRAPPER_TEMPLATE, expression);
            PureRuntime runtime = session.getPureRuntime();

            // Remove previous REPL expression if exists
            if (runtime.getSourceById(REPL_SOURCE_ID) != null)
            {
                runtime.delete(REPL_SOURCE_ID);
            }

            // Create and compile the expression
            runtime.createInMemorySource(REPL_SOURCE_ID, wrappedExpression);
            runtime.compile();

            // Get the function and extract its return type
            CoreInstance function = runtime.getFunction("meta::pure::repl::__repl__eval__():Any[*]");
            if (function == null)
            {
                throw new ReplException("Failed to compile expression");
            }

            ProcessorSupport processorSupport = runtime.getProcessorSupport();
            CoreInstance functionType = function.getValueForMetaPropertyToOne(M3Properties.classifierGenericType)
                    .getValueForMetaPropertyToOne(M3Properties.typeArguments)
                    .getValueForMetaPropertyToOne(M3Properties.rawType);
            CoreInstance returnType = functionType.getValueForMetaPropertyToOne(M3Properties.returnType);
            CoreInstance returnMultiplicity = functionType.getValueForMetaPropertyToOne(M3Properties.returnMultiplicity);

            return buildTypeString(returnType, returnMultiplicity, processorSupport);
        }
        catch (PureException e)
        {
            throw new ReplException("Type inference failed: " + e.getMessage(), e);
        }
    }

    /**
     * Executes the go():Any[*] function if it exists.
     *
     * @return the evaluation result
     * @throws ReplException if the function doesn't exist or execution fails
     */
    public EvaluationResult executeGo() throws ReplException
    {
        EvaluationResult.Builder resultBuilder = new EvaluationResult.Builder()
                .expression("go()");

        try
        {
            PureRuntime runtime = session.getPureRuntime();
            runtime.compile();

            CoreInstance function = runtime.getFunction("go():Any[*]");
            if (function == null)
            {
                throw new ReplException("No go():Any[*] function found. " +
                        "Please define: function go():Any[*] { ... }");
            }

            long executeStart = System.currentTimeMillis();
            FunctionExecution functionExecution = session.getFunctionExecution();

            // Capture console output
            ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
            PrintStream consolePrintStream = new PrintStream(consoleOutput);
            Console console = functionExecution.getConsole();

            try
            {
                console.setPrintStream(consolePrintStream);
                console.setConsole(true);

                CoreInstance result = functionExecution.start(function, FastList.newList());

                long executeMs = System.currentTimeMillis() - executeStart;
                resultBuilder.executeMs(executeMs);

                ProcessorSupport processorSupport = runtime.getProcessorSupport();
                String resultValue = formatResult(result, processorSupport);
                resultBuilder.result(resultValue);

                String consoleOutputStr = consoleOutput.toString();
                if (!consoleOutputStr.isEmpty())
                {
                    resultBuilder.consoleOutput(consoleOutputStr);
                }

                return resultBuilder.success().build();
            }
            finally
            {
                // Reset console to a new stream (we don't have access to the original)
                console.setPrintStream(new PrintStream(new ByteArrayOutputStream()));
                console.setConsole(false);
            }
        }
        catch (PureException e)
        {
            return resultBuilder
                    .error()
                    .errorMessage(e.getMessage())
                    .errorType(e.getClass().getSimpleName())
                    .sourceInfo(e.getSourceInformation())
                    .build();
        }
        catch (Exception e)
        {
            return resultBuilder
                    .error()
                    .errorMessage(e.getMessage())
                    .errorType(e.getClass().getSimpleName())
                    .build();
        }
    }

    /**
     * Runs tests at the specified path.
     *
     * @param path the package path or test function path
     * @param pctAdapter optional PCT adapter
     * @return the test results
     */
    public TestResults runTests(String path, String pctAdapter)
    {
        return runTests(path, pctAdapter, null);
    }

    /**
     * Runs tests at the specified path with an optional filter.
     *
     * @param path the package path or test function path
     * @param pctAdapter optional PCT adapter
     * @param filterRegex optional regex filter for test names
     * @return the test results
     */
    public TestResults runTests(String path, String pctAdapter, String filterRegex)
    {
        TestResults.Builder resultsBuilder = new TestResults.Builder();
        long startTime = System.currentTimeMillis();

        try
        {
            PureRuntime runtime = session.getPureRuntime();
            runtime.compile();

            FunctionExecution functionExecution = session.getFunctionExecution();
            ProcessorSupport processorSupport = runtime.getProcessorSupport();

            // Resolve the path to a package or function
            String effectivePath = path == null || path.isEmpty() ? "::" : path;
            CoreInstance coreInstance = runtime.getCoreInstance(effectivePath);

            if (coreInstance == null)
            {
                return resultsBuilder
                        .status(TestResults.Status.ERROR)
                        .errorMessage("Path not found: " + effectivePath)
                        .build();
            }

            // Determine if it's a function or package
            CoreInstance pkg;
            Predicate<? super CoreInstance> singleTestFilter;

            if (Instance.instanceOf(coreInstance, M3Paths.ConcreteFunctionDefinition, processorSupport))
            {
                pkg = Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties._package, processorSupport);
                singleTestFilter = Predicates.sameAs(coreInstance);
            }
            else
            {
                pkg = coreInstance;
                singleTestFilter = Predicates.alwaysTrue();
            }

            // Build predicates for test collection
            Predicate<CoreInstance> funcExecPredicate = TestCollection.getFilterPredicateForExecutionPlatformClass(
                    functionExecution.getClass(), processorSupport);
            Predicate<? super CoreInstance> predicate = Predicates.and(funcExecPredicate, singleTestFilter);

            // Collect tests
            TestCollection collection = TestCollection.collectTests(pkg, processorSupport,
                    fn -> TestCollection.collectTestsFromPure(fn, functionExecution), predicate);

            MutableList<Pair<CoreInstance, String>> tests = collection.getAllTestFunctionsWithParameterizations(false);
            resultsBuilder.totalTests(tests.size());

            if (tests.isEmpty())
            {
                return resultsBuilder
                        .status(TestResults.Status.COMPLETED)
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            // Create test runner
            TestResultCallback callback = new TestResultCallback(resultsBuilder);
            TestRunner runner = new TestRunner(collection, false, functionExecution, callback, pctAdapter);

            // Run tests
            runner.run();

            return resultsBuilder
                    .status(TestResults.Status.COMPLETED)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
        catch (Exception e)
        {
            return resultsBuilder
                    .status(TestResults.Status.ERROR)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * Builds a type string from a return type and multiplicity.
     */
    private String buildTypeString(CoreInstance returnType, CoreInstance returnMultiplicity,
                                   ProcessorSupport processorSupport)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(GenericType.print(returnType, processorSupport));
        sb.append("[");
        sb.append(Multiplicity.print(returnMultiplicity));
        sb.append("]");
        return sb.toString();
    }

    /**
     * Formats a result CoreInstance as a string.
     */
    private String formatResult(CoreInstance result, ProcessorSupport processorSupport)
    {
        if (result == null)
        {
            return "[]";
        }

        // Handle collections
        if (result.getValueForMetaPropertyToMany(M3Properties.values) != null)
        {
            MutableList<String> values = Lists.mutable.empty();
            for (CoreInstance value : result.getValueForMetaPropertyToMany(M3Properties.values))
            {
                values.add(formatSingleValue(value, processorSupport));
            }
            if (values.size() == 1)
            {
                return values.get(0);
            }
            return "[" + String.join(", ", values) + "]";
        }

        return formatSingleValue(result, processorSupport);
    }

    /**
     * Formats a single value as a string.
     */
    private String formatSingleValue(CoreInstance value, ProcessorSupport processorSupport)
    {
        if (value == null)
        {
            return "null";
        }

        // Check for primitive types
        String classifier = value.getClassifier().getName();
        switch (classifier)
        {
            case "Integer":
            case "Float":
            case "Boolean":
                return value.getName();
            case "String":
                return "'" + value.getName() + "'";
            case "Date":
            case "StrictDate":
            case "DateTime":
                return "%" + value.getName();
            default:
                // For complex types, return the path
                if (Instance.instanceOf(value, M3Paths.PackageableElement, processorSupport))
                {
                    return PackageableElement.getUserPathForPackageableElement(value);
                }
                return value.toString();
        }
    }

    /**
     * Shuts down the engine's executor service.
     */
    public void shutdown()
    {
        executorService.shutdown();
        try
        {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS))
            {
                executorService.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Inner callback class for test results.
     */
    private static class TestResultCallback implements org.finos.legend.pure.m3.execution.test.TestCallBack
    {
        private final TestResults.Builder resultsBuilder;
        private long testStartTime;

        TestResultCallback(TestResults.Builder resultsBuilder)
        {
            this.resultsBuilder = resultsBuilder;
            this.testStartTime = System.currentTimeMillis();
        }

        @Override
        public void foundTests(Iterable<? extends CoreInstance> tests)
        {
            // Reset timer when tests are found
            this.testStartTime = System.currentTimeMillis();
        }

        @Override
        public void executedTest(CoreInstance test, String parameterizationId,
                                 String console,
                                 org.finos.legend.pure.m3.execution.test.TestStatus status)
        {
            long durationMs = System.currentTimeMillis() - testStartTime;
            testStartTime = System.currentTimeMillis(); // Reset for next test

            String testName = PackageableElement.getUserPathForPackageableElement(test);
            if (parameterizationId != null)
            {
                testName += "[" + parameterizationId + "]";
            }

            String errorMessage = null;
            if (status instanceof org.finos.legend.pure.m3.execution.test.TestExceptionStatus)
            {
                Throwable t = ((org.finos.legend.pure.m3.execution.test.TestExceptionStatus) status).getException();
                errorMessage = t != null ? t.getMessage() : null;
            }

            TestResults.TestResult result = new TestResults.TestResult(
                    testName,
                    convertStatus(status),
                    durationMs,
                    console,
                    errorMessage
            );

            resultsBuilder.addTestResult(result);
        }

        private TestResults.TestStatus convertStatus(org.finos.legend.pure.m3.execution.test.TestStatus status)
        {
            if (status == org.finos.legend.pure.m3.execution.test.TestStatus.SUCCESS)
            {
                return TestResults.TestStatus.PASS;
            }
            else if (status instanceof org.finos.legend.pure.m3.execution.test.TestExceptionStatus)
            {
                return TestResults.TestStatus.FAIL;
            }
            return TestResults.TestStatus.ERROR;
        }
    }
}
