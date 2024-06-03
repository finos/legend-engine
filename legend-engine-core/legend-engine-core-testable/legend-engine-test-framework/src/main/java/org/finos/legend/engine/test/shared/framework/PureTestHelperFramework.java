// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.test.shared.framework;

import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.function.checked.ThrowingFunction0;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.VersionControlledClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.ParserService;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.FunctionCache;
import org.junit.Ignore;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;

public class PureTestHelperFramework
{
    private static final ThreadLocal<ServersState> state = new ThreadLocal<>();

    @Ignore
    public static TestSetup wrapSuite(ThrowingFunction0<Boolean> init,
                                      Function0<TestSuite> suiteBuilder,
                                      ThrowingFunction0<Boolean> shutdown,
                                      MutableList<TestServerResource> testServerResources)
    {
        try
        {
            boolean shouldCleanUp = init.safeValue();
            TestSuite suite = suiteBuilder.value();
            if (shouldCleanUp)
            {
                shutdown.safeValue();
            }

            return new TestSetup(suite)
            {
                boolean shouldCleanUp;

                @Override
                protected void setUp() throws Exception
                {
                    super.setUp();
                    shouldCleanUp = init.safeValue();
                    ServersState serversState = new ServersState(testServerResources);
                    serversState.start();
                    state.set(serversState);
                }

                @Override
                protected void tearDown() throws Exception
                {
                    super.tearDown();
                    state.get().shutDown();
                    state.remove();
                    if (this.shouldCleanUp)
                    {
                        shutdown.safeValue();
                    }
                }
            };
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static boolean hasTestStereotypeWithValue(CoreInstance node, String value, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(node, M3Properties.stereotypes, processorSupport);
        if (stereotypes.notEmpty())
        {
            CoreInstance testProfile = processorSupport.package_getByUserPath("meta::pure::profiles::test");
            for (CoreInstance stereotype : stereotypes)
            {
                if ((testProfile == Instance.getValueForMetaPropertyToOneResolved(stereotype, M3Properties.profile, processorSupport)) &&
                        value.equals(Instance.getValueForMetaPropertyToOneResolved(stereotype, M3Properties.value, processorSupport).getName()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean shouldExcludeOnClientVersion(CoreInstance node, String serverVersion, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> taggedValues = Instance.getValueForMetaPropertyToManyResolved(node, M3Properties.taggedValues, processorSupport);
        if (taggedValues.notEmpty())
        {
            CoreInstance serverVersionProfile = processorSupport.package_getByUserPath("meta::pure::executionPlan::profiles::serverVersion");
            for (CoreInstance taggedValue : taggedValues)
            {
                if ((taggedValue.getValueForMetaPropertyToOne("tag").getValueForMetaPropertyToOne("profile") == serverVersionProfile)
                        && (taggedValue.getValueForMetaPropertyToOne("tag").getName().equals("exclude")) &&
                        serverVersion.toLowerCase().equals(Instance.getValueForMetaPropertyToOneResolved(taggedValue, M3Properties.value, processorSupport).getName().toLowerCase()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean shouldExecuteOnClientVersionOnwards(CoreInstance node, String serverVersion, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> taggedValues = Instance.getValueForMetaPropertyToManyResolved(node, M3Properties.taggedValues, processorSupport);
        if (taggedValues.notEmpty())
        {
            CoreInstance serverVersionProfile = processorSupport.package_getByUserPath("meta::pure::executionPlan::profiles::serverVersion");
            for (CoreInstance taggedValue : taggedValues)
            {
                if ((taggedValue.getValueForMetaPropertyToOne("tag").getValueForMetaPropertyToOne("profile") == serverVersionProfile)
                        && (taggedValue.getValueForMetaPropertyToOne("tag").getName().equals("start")))
                {
                    return PureClientVersions.versionAGreaterThanOrEqualsVersionB(serverVersion.toLowerCase(), Instance.getValueForMetaPropertyToOneResolved(taggedValue, M3Properties.value, processorSupport).getName().toLowerCase());
                }

            }
        }
        return true;
    }

    public static boolean satisfiesConditions(CoreInstance node, ProcessorSupport processorSupport)
    {
        String ver = System.getProperty("alloy.test.clientVersion") == null ? System.getProperty("legend.test.clientVersion") : System.getProperty("alloy.test.clientVersion");
        return !hasTestStereotypeWithValue(node, "ExcludeAlloy", processorSupport) &&
                !shouldExcludeOnClientVersion(node, ver, processorSupport) &&
                shouldExecuteOnClientVersionOnwards(node, ver, processorSupport);
    }

    public static CompiledExecutionSupport getExecutionSupport()
    {
        return new CompiledExecutionSupport(
                new JavaCompilerState(null, PureTestHelperFramework.class.getClassLoader()),
                new CompiledProcessorSupport(PureTestHelperFramework.class.getClassLoader(), PureModel.METADATA_LAZY, Sets.mutable.empty()),
                null,
                new RepositoryCodeStorage()
                {
                    @Override
                    public void initialize(Message message)
                    {

                    }

                    @Override
                    public CodeRepository getRepositoryForPath(String s)
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<CodeRepository> getAllRepositories()
                    {
                        return null;
                    }

                    @Override
                    public CodeRepository getRepository(String s)
                    {
                        return null;
                    }

                    @Override
                    public CodeStorageNode getNode(String s)
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<CodeStorageNode> getFiles(String s)
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<String> getUserFiles()
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<String> getFileOrFiles(String s)
                    {
                        return null;
                    }

                    @Override
                    public InputStream getContent(String s)
                    {
                        return null;
                    }

                    @Override
                    public byte[] getContentAsBytes(String s)
                    {
                        return new byte[0];
                    }

                    @Override
                    public String getContentAsText(String s)
                    {
                        return null;
                    }

                    @Override
                    public boolean exists(String s)
                    {
                        return false;
                    }

                    @Override
                    public boolean isFile(String s)
                    {
                        return false;
                    }

                    @Override
                    public boolean isFolder(String s)
                    {
                        return false;
                    }

                    @Override
                    public boolean isEmptyFolder(String s)
                    {
                        return false;
                    }
                },
                null,
                null,
                new ConsoleCompiled(),
                new FunctionCache(),
                new ClassCache(),
                null,
                Sets.mutable.empty(),
                CompiledExtensionLoader.extensions()
        );
    }

    public static CompiledExecutionSupport getClassLoaderExecutionSupport()
    {
        return getClassLoaderExecutionSupport(false);
    }

    public static CompiledExecutionSupport getClassLoaderExecutionSupport(boolean enableConsole)
    {
        ParserService loader = new ParserService();
        ListIterable<Parser> parsers = loader.parsers();
        ListIterable<InlineDSL> inlineDSLs = loader.inlineDSLs();

        ConsoleCompiled console = new ConsoleCompiled();
        if (enableConsole == true)
        {
            console.enable();
        }
        else
        {
            console.disable();
        }

        return new CompiledExecutionSupport(
                new JavaCompilerState(null, PureTestHelperFramework.class.getClassLoader()),
                new CompiledProcessorSupport(PureTestHelperFramework.class.getClassLoader(), PureModel.METADATA_LAZY, Sets.mutable.empty()),
                null,
                new CompositeCodeStorage(new VersionControlledClassLoaderCodeStorage(PureTestHelperFramework.class.getClassLoader(), CodeRepositoryProviderHelper.findCodeRepositories(true), null)),
                null,
                null,
                console,
                new FunctionCache(),
                new ClassCache(),
                null,
                Sets.mutable.empty(),
                CompiledExtensionLoader.extensions()
        );
    }

    @Ignore
    public static class PureTestCase extends TestCase
    {
        CoreInstance coreInstance;
        ExecutionSupport executionSupport;

        public PureTestCase()
        {
        }

        PureTestCase(CoreInstance coreInstance, ExecutionSupport executionSupport)
        {
            super(coreInstance.getValueForMetaPropertyToOne("functionName").getName());
            this.coreInstance = coreInstance;
            this.executionSupport = executionSupport;
        }

        @Override
        protected void runTest() throws Throwable
        {
            Class<?> _class = Class.forName("org.finos.legend.pure.generated." + IdBuilder.sourceToId(coreInstance.getSourceInformation()));
            Method method = _class.getMethod(FunctionProcessor.functionNameToJava(coreInstance), ExecutionSupport.class);
            // NOTE: mock out the global tracer for test
            // See https://github.com/opentracing/opentracing-java/issues/170
            // See https://github.com/opentracing/opentracing-java/issues/364
            GlobalTracer.registerIfAbsent(NoopTracerFactory.create());
            String testName = PackageableElement.getUserPathForPackageableElement(this.coreInstance);
            System.out.print("EXECUTING " + testName + " ... ");
            long start = System.nanoTime();
            try
            {
                method.invoke(null, this.executionSupport);
                System.out.format("DONE (%.6fs)\n", (System.nanoTime() - start) / 1_000_000_000.0);
            }
            catch (InvocationTargetException e)
            {
                System.out.format("ERROR (%.6fs)\n", (System.nanoTime() - start) / 1_000_000_000.0);
                throw e.getTargetException();
            }
        }
    }

    @Ignore
    public static class JavaPureTestCase extends PureTestCase
    {
        CoreInstance runnerInstance;

        public JavaPureTestCase()
        {
        }

        JavaPureTestCase(CoreInstance runnerInstance, CoreInstance coreInstance, ExecutionSupport executionSupport)
        {
            super(coreInstance, executionSupport);
            this.runnerInstance = runnerInstance;
        }

        @Override
        protected void runTest() throws Throwable
        {
            Class<?> _class = Class.forName("org.finos.legend.pure.generated." + IdBuilder.sourceToId(runnerInstance.getSourceInformation()));
            Object[] params = Lists.mutable.empty().with(coreInstance).with(executionSupport).toArray();

            String methodName = FunctionProcessor.functionNameToJava(runnerInstance);
            Method method = params.length == 1 ? _class.getMethod(methodName, ExecutionSupport.class)
                    : ArrayIterate.detect(_class.getMethods(), m -> methodName.equals(m.getName()));

            // NOTE: mock out the global tracer for test
            // See https://github.com/opentracing/opentracing-java/issues/170
            // See https://github.com/opentracing/opentracing-java/issues/364
            GlobalTracer.registerIfAbsent(NoopTracerFactory.create());
            String testName = PackageableElement.getUserPathForPackageableElement(this.coreInstance);
            System.out.print("EXECUTING " + testName + " ... ");
            long start = System.nanoTime();
            try
            {
                method.invoke(null, params);
                System.out.format("DONE (%.6fs)\n", (System.nanoTime() - start) / 1_000_000_000.0);
            }
            catch (InvocationTargetException e)
            {
                System.out.format("ERROR (%.6fs)\n", (System.nanoTime() - start) / 1_000_000_000.0);
                throw e.getTargetException();
            }
        }
    }

    public static TestSuite buildSuite(TestCollection testCollection, ExecutionSupport executionSupport)
    {
        MutableList<TestSuite> subSuites = new FastList<>();
        for (TestCollection collection : testCollection.getSubCollections().toSortedList(Comparator.comparing(a -> a.getPackage().getName())))
        {
            subSuites.add(buildSuite(collection, executionSupport));
        }
        return buildSuite(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.GET_USER_PATH.valueOf(testCollection.getPackage()),
                testCollection.getBeforeFunctions(),
                testCollection.getAfterFunctions(),
                testCollection.getPureAndAlloyOnlyFunctions(),
                subSuites,
                executionSupport
        );
    }

    private static TestSuite buildSuite(String packageName, RichIterable<CoreInstance> beforeFunctions, RichIterable<CoreInstance> afterFunctions,
                                        RichIterable<CoreInstance> testFunctions, org.eclipse.collections.api.list.ListIterable<TestSuite> subSuites, ExecutionSupport executionSupport)
    {
        TestSuite suite = new TestSuite();
        suite.setName(packageName);
        beforeFunctions.collect(fn -> new PureTestCase(fn, executionSupport)).each(suite::addTest);
        for (Test subSuite : subSuites.toSortedList(Comparator.comparing(TestSuite::getName)))
        {
            suite.addTest(subSuite);
        }
        for (CoreInstance testFunc : testFunctions.toSortedList(Comparator.comparing(CoreInstance::getName)))
        {
            Test theTest = new PureTestCase(testFunc, executionSupport);
            suite.addTest(theTest);
        }
        afterFunctions.collect(fn -> new PureTestCase(fn, executionSupport)).each(suite::addTest);
        return suite;
    }

    public static TestSuite buildJavaPureTestSuite(TestCollection testCollection, ExecutionSupport executionSupport, CoreInstance runner)
    {
        MutableList<TestSuite> subSuites = new FastList<>();
        for (TestCollection collection : testCollection.getSubCollections().toSortedList(Comparator.comparing(a -> a.getPackage().getName())))
        {
            subSuites.add(buildJavaPureTestSuite(collection, executionSupport, runner));
        }
        return buildJavaPureTestSuite(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.GET_USER_PATH.valueOf(testCollection.getPackage()),
                testCollection.getBeforeFunctions(),
                testCollection.getAfterFunctions(),
                testCollection.getPureAndAlloyOnlyFunctions(),
                subSuites,
                executionSupport,
                runner
        );
    }

    private static TestSuite buildJavaPureTestSuite(String packageName, RichIterable<CoreInstance> beforeFunctions, RichIterable<CoreInstance> afterFunctions,
                                                    RichIterable<CoreInstance> testFunctions, org.eclipse.collections.api.list.ListIterable<TestSuite> subSuites, ExecutionSupport executionSupport, CoreInstance runner)
    {
        TestSuite suite = new TestSuite();
        suite.setName(packageName);
        beforeFunctions.collect(fn -> new JavaPureTestCase(runner, fn, executionSupport)).each(suite::addTest);
        for (Test subSuite : subSuites.toSortedList(Comparator.comparing(TestSuite::getName)))
        {
            suite.addTest(subSuite);
        }
        for (CoreInstance testFunc : testFunctions.toSortedList(Comparator.comparing(CoreInstance::getName)))
        {
            Test theTest = new JavaPureTestCase(runner, testFunc, executionSupport);
            suite.addTest(theTest);
        }
        afterFunctions.collect(fn -> new JavaPureTestCase(runner, fn, executionSupport)).each(suite::addTest);
        return suite;
    }

}
