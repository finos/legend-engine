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

package org.finos.legend.engine.pure.runtime.execution;

import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.pure.runtime.compiler.interpreted.natives.LegendCompileMixedProcessorSupport;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.VoidPureRuntimeStatus;
import org.finos.legend.pure.m3.serialization.runtime.cache.ClassLoaderPureGraphCache;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RunWith(Parameterized.class)
public class LegendExecuteTest
{
    private static PureRuntime setUpRuntime(CoreInstanceFactoryRegistry registryOverride) throws Exception
    {
        System.out.println("starting to setup runtime");

        RichIterable<CodeRepository> repositories = getRepositories().select(p -> !p.getName().startsWith("other_") && !p.getName().startsWith("test_"));
        System.out.println(repositories.collect(CodeRepository::getName).makeString("Code Repos: ", ", ", ""));
        PureCodeStorage codeStorage = new PureCodeStorage(null, new ClassLoaderCodeStorage(repositories));

        ClassLoaderPureGraphCache graphCache = new ClassLoaderPureGraphCache()
        {
            @Override
            public void deleteCache()
            {
                // ignore
            }
        };

        PureRuntime runtime = new PureRuntimeBuilder(codeStorage)
                .withRuntimeStatus(VoidPureRuntimeStatus.VOID_PURE_RUNTIME_STATUS)
                .setTransactionalByDefault(false)
                .withFactoryRegistryOverride(registryOverride)
                .withCache(graphCache)
                .buildAndTryToInitializeFromCache();

        Assert.assertTrue("Fail to init cache: " + graphCache.getCacheState().getLastErrorMessage(), runtime.isInitialized());

        return runtime;
    }

    private static FunctionExecutionInterpreted interpretedExecution() throws Exception
    {
        System.out.println("starting preparing interpreted execution");
        FunctionExecutionInterpreted interpretedFunctionExecution = new FunctionExecutionInterpreted();
        interpretedFunctionExecution.init(setUpRuntime(null), new Message(""));
        interpretedFunctionExecution.setProcessorSupport(new LegendCompileMixedProcessorSupport(interpretedFunctionExecution.getRuntime().getContext(), interpretedFunctionExecution.getRuntime().getModelRepository(), interpretedFunctionExecution.getProcessorSupport()));
        interpretedFunctionExecution.getConsole().disable();
        System.out.println("finish preparing interpreted execution");
        return interpretedFunctionExecution;
    }

    private static FunctionExecutionCompiled compileExecution() throws Exception
    {
        PureRuntime runtime = setUpRuntime(JavaModelFactoryRegistryLoader.loader());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        // The core_* java code is on the class path from the original jar.  That code is generated using distributed mode
        // We need to respect the java code generated in memory class loader as part of the FunctionExecutionCompiled init
        // Otherwise, we get metadata problems (monolithic vs distributed)
        ClassLoader classLoader = new ClassLoader(contextClassLoader == null ? LegendExecuteTest.class.getClassLoader() : contextClassLoader)
        {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
            {
                if (name.startsWith("org.finos.legend.pure.generated.core_"))
                {
                    throw new ClassNotFoundException("Generated should come from memory class loader: " + name);
                }
                return super.loadClass(name, resolve);
            }
        };

        Thread.currentThread().setContextClassLoader(classLoader);

        try
        {
            System.out.println("starting preparing compiled execution");
            FunctionExecutionCompiled compileFunctionExecution = new FunctionExecutionCompiledBuilder().build();
            compileFunctionExecution.getConsole().disable();
            compileFunctionExecution.init(runtime, new Message(""));
            System.out.println("finish preparing compiled execution");
            return compileFunctionExecution;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> modes() throws Exception
    {
        return Arrays.asList(new Object[][]
            {
                { "compiled",  compileExecution() },
                { "interpreted", interpretedExecution() }
            }
        );
    }

    private final FunctionExecution functionExecution;

    public LegendExecuteTest(String ignored, FunctionExecution functionExecution)
    {
        this.functionExecution = functionExecution;
    }

    @Test
    public void testPlanExecution()
    {
        test(
                "|test::Types.all()->graphFetch(#{test::Types{string}}#)->from(test::simpleModelMapping, $runtime)->serialize(#{test::Types{string}}#)",
                "[]",
                "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"string\":\"string1\"},{\"string\":\"string2\"}]}"
        );
    }

    @Test
    public void tesStringVariable()
    {
        String toExecute = "{pref: String[1], suf:String[1]|\n" +
                "  let tree = #{test::Types{string, 'prefixed': string($pref, $suf)}}#;\n" +
                "  test::Types.all()->graphFetch($tree)->from(test::simpleModelMapping, $runtime)->serialize($tree);}";

        test(toExecute,
                "[pair('pref', 'hello_'), pair('suf', '_bye')]",
                "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"string\":\"string1\",\"prefixed\":\"hello_string1_bye\"},{\"string\":\"string2\",\"prefixed\":\"hello_string2_bye\"}]}"
        );
    }

    @Test
    public void testStrictDateVariable()
    {
        String toExecute = "{asOf: StrictDate[1]|\n" +
                "      let tree = #{test::Types{date, 'years': years($asOf)}}#;\n" +
                "      test::Types.all()->graphFetch($tree)->from(test::simpleModelMapping, $runtime)->serialize($tree);}";

        test(toExecute,
                "pair('asOf', %2020-01-01)",
                " {\"builder\":{\"_type\":\"json\"},\"values\":[{\"date\":\"2020-01-01\",\"years\":0},{\"date\":\"2019-01-01\",\"years\":1}]}"
        );
    }

    @Test
    public void testDateTimeVariable()
    {
        String toExecute = "{asOf: DateTime[1]|\n" +
                "      let tree = #{test::Types{dateTime, 'seconds': seconds($asOf)}}#;\n" +
                "      test::Types.all()->graphFetch($tree)->from(test::simpleModelMapping, $runtime)->serialize($tree);}";

        test(toExecute,
                "pair('asOf', %2020-01-01T01:01:01.123)",
                "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"dateTime\":\"2021-01-02T01:02:11.1223\",\"seconds\":-31708869},{\"dateTime\":\"2023-01-02T05:02:11.1223\",\"seconds\":-94795269}]}"
        );
    }

    @Test
    public void testBooleanVariable()
    {
        String toExecute = "{bool: Boolean[1]|\n" +
                "  let tree = #{test::Types{bool, 'and': and($bool)}}#;\n" +
                "  test::Types.all()->graphFetch($tree)->from(test::simpleModelMapping, $runtime)->serialize($tree);}";

        test(toExecute,
                "pair('bool', true)",
                "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"bool\":true,\"and\":true},{\"bool\":false,\"and\":false}]}"
        );
    }

    @Test
    public void testFloatVariable()
    {
        String toExecute = "{num: Float[1]|\n" +
                "  let tree = #{test::Types{float, 'mult': mult($num)}}#;\n" +
                "  test::Types.all()->graphFetch($tree)->from(test::simpleModelMapping, $runtime)->serialize($tree);}";

        test(toExecute,
                "pair('num', 1.1234)",
                "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"float\":123.123,\"mult\":138.3163782},{\"float\":125.125,\"mult\":140.565425}]}"
        );
    }

    @Test
    public void testIntegerVariable()
    {
        String toExecute = "{num: Integer[1]|\n" +
                "  let tree = #{test::Types{int, 'mult': mult($num)}}#;\n" +
                "  test::Types.all()->graphFetch($tree)->from(test::simpleModelMapping, $runtime)->serialize($tree);}";

        test(toExecute,
                "pair('num', 123)",
                "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"int\":123456,\"mult\":15185088},{\"int\":654321,\"mult\":80481483}]}"
        );
    }

    @Test
    public void testDecimalVariable()
    {
        String toExecute = "{num: Decimal[1]|\n" +
                "  let tree = #{test::Types{decimal, 'mult': mult($num)}}#;\n" +
                "  test::Types.all()->graphFetch($tree)->from(test::simpleModelMapping, $runtime)->serialize($tree);}";

        test(toExecute,
                "pair('num', 1.1234->meta::pure::functions::math::toDecimal())",
                "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"decimal\":789.123,\"mult\":886.5007782},{\"decimal\":345.219,\"mult\":387.8190246}]}"
        );
    }

    @Test
    public void testToManyVariable()
    {
        String toExecute = "{nums: Integer[*]|\n" +
                "  let tree = #{test::Types{int, 'sum': sum($nums)}}#;\n" +
                "  test::Types.all()->graphFetch($tree)->from(test::simpleModelMapping, $runtime)->serialize($tree);}";

        test(toExecute,
                "pair('nums', meta::pure::functions::collection::list([1, 2, 3, 4, 5]))",
                "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"int\":123456,\"sum\":123471},{\"int\":654321,\"sum\":654336}]}"
        );
    }

    private void test(String toExecute, String vars, String expectedPlanResult)
    {
        try (InputStream is = Objects.requireNonNull(LegendExecuteTest.class.getResourceAsStream("/testModels.txt")))
        {
            List<String> lines = IOUtils.readLines(is, StandardCharsets.UTF_8);
            lines.add("function test():Any[*]{");
            lines.add("  let extensions = meta::external::shared::format::executionPlan::platformBinding::legendJava::bindingExtensionsWithLegendJavaPlatformBinding([]);");
            lines.add("  let runtime = ^Runtime(");
            lines.add("                      connections = [");
            lines.add("                        ^JsonModelConnection(");
            lines.add("                          element = ^ModelStore(),");
            lines.add("                          class = test::_S_Types,");
            lines.add("                          url = 'data:application/json,\\n{\"string\": \"string1\", \"float\": 123.123, \"int\": 123456, \"bool\": true, \"decimal\": \"789.123\", \"date\": \"2020-01-01\", \"dateTime\": \"2021-01-02T01:02:11.1223Z\"}\\n{\"string\": \"string2\", \"float\": 125.125, \"int\": 654321, \"bool\": false, \"decimal\": \"345.219\", \"date\": \"2019-01-01\", \"dateTime\": \"2023-01-02T05:02:11.1223Z\"}'");
            lines.add("                        )");
            lines.add("                      ]");
            lines.add("                    );");
            lines.add("  let toExecute = " + toExecute + ";");
            lines.add("  let vars = " + vars + ";");
            lines.add("  let result = meta::legend::execute($toExecute, $vars, ^ExecutionContext(), $extensions);");
            lines.add("  let expectedPlanResult = '" + expectedPlanResult + "';");
            lines.add("  assertJsonStringsEqual($result, $expectedPlanResult);");
            lines.add("}");

            String code = String.join("\n", lines);
            functionExecution.getRuntime().createInMemoryAndCompile(
                    Collections.singletonList(Tuples.pair("testSource.pure", code)));
            CoreInstance func = functionExecution.getRuntime().getFunction("test():Any[*]");
            functionExecution.start(func, Lists.immutable.empty());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        finally
        {
            functionExecution.getRuntime().delete("testSource.pure");
        }
    }

    private static RichIterable<CodeRepository> getRepositories()
    {
        return CodeRepositoryProviderHelper.findCodeRepositories(true).toList()
                .with(PlatformCodeRepository.newPlatformCodeRepository());
    }
}
