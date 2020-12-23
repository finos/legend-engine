// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.server.test.shared;

import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.server.Server;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CoreCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.VersionControlledClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.Revision;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.FunctionCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;
import org.junit.Ignore;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Objects;

public class PureTestHelper
{
    private static final ThreadLocal<ServersState> state = new ThreadLocal<>();

    public static boolean initClientVersionIfNotAlreadySet(String defaultClientVersion)
    {
        boolean isNotSet = System.getProperty("alloy.test.clientVersion") == null;
        if (isNotSet)
        {
            System.setProperty("alloy.test.clientVersion", defaultClientVersion);
            System.setProperty("alloy.test.serverVersion", "v1");
        }
        return isNotSet;
    }

    public static void cleanUp()
    {
        System.clearProperty("alloy.test.clientVersion");
    }

    @Ignore
    public static TestSetup wrapSuite(TestSuite suite, boolean shouldClean)
    {
        return new TestSetup(suite)
        {
            @Override
            protected void setUp() throws Exception
            {
                super.setUp();
                state.set(initEnvironment());
            }

            @Override
            protected void tearDown() throws Exception
            {
                super.tearDown();
                state.get().shutDown();
                state.remove();
                if (shouldClean)
                {
                    cleanUp();
                }
            }
        };
    }

    public static ServersState initEnvironment() throws Exception
    {
        int engineServerPort = 1100 + (int) (Math.random() * 30000);
        int metadataServerPort = 1100 + (int) (Math.random() * 30000);

        // Start metadata server
        TestMetaDataServer metadataServer = new TestMetaDataServer(metadataServerPort, true);
        System.out.println("Metadata server started on port:" + metadataServerPort);

        // Start engine server
        System.setProperty("dw.server.connector.port", String.valueOf(engineServerPort));
        System.setProperty("dw.metadataserver.pure.port", String.valueOf(metadataServerPort));
        System.out.println("Found Config file: " + Objects.requireNonNull(PureTestHelper.class.getClassLoader().getResource("org/finos/legend/engine/server/test/userTestConfig.json")).getFile());

        Server server = new Server();
        server.run("server", Objects.requireNonNull(PureTestHelper.class.getClassLoader().getResource("org/finos/legend/engine/server/test/userTestConfig.json")).getFile());
        System.out.println("Alloy server started on port:" + engineServerPort);

        // Pure client configuration (to call the engine server)
        System.setProperty("test.metadataserver.pure.port", String.valueOf(metadataServerPort));
        System.setProperty("alloy.test.server.host", "127.0.0.1");
        System.setProperty("alloy.test.server.port", String.valueOf(engineServerPort));
        System.out.println("Pure client configured to reach engine server");

        return new ServersState(server, metadataServer);
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
        return !hasTestStereotypeWithValue(node, "ExcludeAlloy", processorSupport) &&
                !shouldExcludeOnClientVersion(node, System.getProperty("alloy.test.clientVersion"), processorSupport) &&
                shouldExecuteOnClientVersionOnwards(node, System.getProperty("alloy.test.clientVersion"), processorSupport);
    }

    public static CompiledExecutionSupport getExecutionSupport()
    {
        return new CompiledExecutionSupport(
                new JavaCompilerState(null, PureTestHelper.class.getClassLoader()),
                new CompiledProcessorSupport(PureTestHelper.class.getClassLoader(), new MetadataLazy(PureTestHelper.class.getClassLoader()), Sets.mutable.empty()),
                null,
                new CodeStorage()
                {
                    @Override
                    public String getRepoName(String s)
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<String> getAllRepoNames()
                    {
                        return null;
                    }

                    @Override
                    public boolean isRepoName(String s)
                    {
                        return false;
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

                    @Override
                    public boolean isVersioned(String s)
                    {
                        return false;
                    }

                    @Override
                    public long getCurrentRevision(String s)
                    {
                        return 0;
                    }

                    @Override
                    public LongList getAllRevisions(String s)
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<Revision> getAllRevisionLogs(RichIterable<String> richIterable)
                    {
                        return null;
                    }
                },
                null,
                null,
                new ConsoleCompiled(),
                new FunctionCache(),
                new ClassCache(),
                null,
                Sets.mutable.empty()
        );
    }

    public static CompiledExecutionSupport getClassLoaderExecutionSupport()
    {
//        ClassLoaderCodeStorage platformCodeStorage = new VersionControlledClassLoaderCodeStorage(PureTestHelper.class.getClassLoader(), PureRepositories.getRepositoryByName("platform"), new PureRepositoryRevisionCache(SVNWCUtil.createDefaultAuthenticationManager("puresvntest", "Pure#Test#Access".toCharArray())));
        MutableList<CodeRepository> codeRepos = Lists.mutable.of(CoreCodeRepository.newPlatformCodeRepository(), CodeRepository.newCoreCodeRepository());
        return new CompiledExecutionSupport(
                new JavaCompilerState(null, PureTestHelper.class.getClassLoader()),
                new CompiledProcessorSupport(PureTestHelper.class.getClassLoader(), new MetadataLazy(PureTestHelper.class.getClassLoader()), Sets.mutable.empty()),
                null,
                new PureCodeStorage(null, new VersionControlledClassLoaderCodeStorage(PureTestHelper.class.getClassLoader(), codeRepos, null)),
                null,
                null,
                new ConsoleCompiled(),
                new FunctionCache(),
                new ClassCache(),
                null,
                Sets.mutable.empty()
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
            try
            {
                method.invoke(null, this.executionSupport);
            }
	    catch(InvocationTargetException e)
            {
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
}
