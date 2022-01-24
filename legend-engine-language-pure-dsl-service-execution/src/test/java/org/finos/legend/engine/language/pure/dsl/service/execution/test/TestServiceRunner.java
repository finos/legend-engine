// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.service.execution.test;

import com.google.common.cache.CacheBuilder;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.service.execution.AbstractServicePlanExecutor;
import org.finos.legend.engine.language.pure.dsl.service.execution.OperationalContext;
import org.finos.legend.engine.language.pure.dsl.service.execution.ServiceRunnerInput;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.plan.execution.cache.ExecutionCacheBuilder;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCrossAssociationKeys;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.finos.legend.pure.generated.core_relational_relational_router_router_extension;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TestServiceRunner
{
    @Test
    public void testSimpleM2MServiceExecution()
    {
        SimpleM2MServiceRunner simpleM2MServiceRunner = new SimpleM2MServiceRunner();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList("{\"fullName\": \"Peter Smith\"}"));

        String result = simpleM2MServiceRunner.run(serviceRunnerInput);
        Assert.assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}}", result);
    }

    @Test
    public void testSimpleM2MServiceExecutionWithOutputStream()
    {
        SimpleM2MServiceRunner simpleM2MServiceRunner = new SimpleM2MServiceRunner();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList("{\"fullName\": \"Peter Smith\"}"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        simpleM2MServiceRunner.run(serviceRunnerInput, outputStream);
        String result = outputStream.toString();
        Assert.assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}}", result);
    }

    @Test
    public void testSimpleM2MServiceExecutionWithSerializationFormat()
    {
        SimpleM2MServiceRunner simpleM2MServiceRunner = new SimpleM2MServiceRunner();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList("[{\"fullName\": \"Peter Smith\"},{\"fullName\": \"John Johnson\"}]"))
                .withSerializationFormat(SerializationFormat.PURE);

        String result = simpleM2MServiceRunner.run(serviceRunnerInput);
        Assert.assertEquals("[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"}]", result);
    }

    @Test
    public void testMultiParameterM2MServiceExecution()
    {
        MultiParameterM2MServiceRunner multiParameterM2MServiceRunner = new MultiParameterM2MServiceRunner();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Arrays.asList("{\"fullName\": \"Peter Smith\"}", "{\"fullName\": \"John Johnson\"}"))
                .withSerializationFormat(SerializationFormat.PURE);

        String result = multiParameterM2MServiceRunner.run(serviceRunnerInput);
        Assert.assertEquals("[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"}]", result);
    }

    @Test
    public void testSimpleRelationalServiceExecution()
    {
        SimpleRelationalServiceRunner simpleRelationalServiceRunner = new SimpleRelationalServiceRunner();

        ServiceRunnerInput serviceRunnerInput1 = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList(Collections.singletonList("John")))
                .withSerializationFormat(SerializationFormat.PURE);
        String result1 = simpleRelationalServiceRunner.run(serviceRunnerInput1);
        Assert.assertEquals("{\"firstName\":\"John\",\"lastName\":\"Johnson\"}", result1);

        ServiceRunnerInput serviceRunnerInput2 = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList(Arrays.asList("John", "Peter")))
                .withSerializationFormat(SerializationFormat.PURE);
        String result2 = simpleRelationalServiceRunner.run(serviceRunnerInput2);
        Assert.assertEquals("[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"}]", result2);
    }

    @Test
    public void testXStoreServiceExecutionWithNoCrossPropertyAccess() throws JavaCompileException
    {
        XStoreServiceRunnerWithNoCrossPropertyAccess xStoreServiceRunner = new XStoreServiceRunnerWithNoCrossPropertyAccess();
        Assert.assertEquals(Collections.emptyList(), xStoreServiceRunner.getGraphFetchCrossAssociationKeys());
        String result = xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withSerializationFormat(SerializationFormat.PURE));
        Assert.assertEquals("[{\"fullName\":\"P1\"},{\"fullName\":\"P2\"},{\"fullName\":\"P3\"},{\"fullName\":\"P4\"},{\"fullName\":\"P5\"}]", result);
    }

    @Test
    public void testXStoreServiceExecutionWithSingleCrossPropertyAccessCache() throws JavaCompileException
    {
        XStoreServiceRunnerWithSingleCrossPropertyAccess xStoreServiceRunner = new XStoreServiceRunnerWithSingleCrossPropertyAccess();
        Assert.assertEquals(1, xStoreServiceRunner.getGraphFetchCrossAssociationKeys().size());
        Assert.assertEquals("<default, root.firm>", xStoreServiceRunner.getGraphFetchCrossAssociationKeys().get(0).getName());

        ExecutionCache<GraphFetchCacheKey, List<Object>> firmCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        OperationalContext operationalContext = OperationalContext
                .newInstance()
                .withGraphFetchCrossAssociationKeysCacheConfig(Maps.mutable.of(xStoreServiceRunner.getGraphFetchCrossAssociationKeys().get(0), firmCache));

        String expectedRes = "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\"}}" +
                "]";

        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(firmCache, 3, 5, 2, 3);
        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(firmCache, 3, 10, 7, 3);
    }

    @Test
    public void testXStoreServiceExecutionWithToManyCrossPropertyAccessCache() throws JavaCompileException
    {
        XStoreServiceRunnerWithToManyCrossPropertyAccess xStoreServiceRunner = new XStoreServiceRunnerWithToManyCrossPropertyAccess();
        Assert.assertEquals(1, xStoreServiceRunner.getGraphFetchCrossAssociationKeys().size());
        Assert.assertEquals("<default, root.persons>", xStoreServiceRunner.getGraphFetchCrossAssociationKeys().get(0).getName());

        ExecutionCache<GraphFetchCacheKey, List<Object>> personCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        OperationalContext operationalContext = OperationalContext
                .newInstance()
                .withGraphFetchCrossAssociationKeysCacheConfig(Maps.mutable.of(xStoreServiceRunner.getGraphFetchCrossAssociationKeys().get(0), personCache));

        String expectedRes = "[" +
                "{\"name\":\"A1\",\"persons\":[{\"fullName\":\"P1\"},{\"fullName\":\"P5\"}]}," +
                "{\"name\":\"A2\",\"persons\":[{\"fullName\":\"P2\"}]}," +
                "{\"name\":\"A3\",\"persons\":[{\"fullName\":\"P4\"}]}," +
                "{\"name\":\"A4\",\"persons\":[]}," +
                "{\"name\":\"A5\",\"persons\":[]}" +
                "]";

        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(personCache, 5, 5, 0, 5);
        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(personCache, 5, 10, 5, 5);
    }

    @Test
    public void testXStoreServiceExecutionWithMultiCrossPropertyAccessCaches() throws JavaCompileException
    {
        XStoreServiceRunnerWithMultiCrossPropertyAccess xStoreServiceRunner = new XStoreServiceRunnerWithMultiCrossPropertyAccess();
        Assert.assertEquals(2, xStoreServiceRunner.getGraphFetchCrossAssociationKeys().size());
        Assert.assertEquals(Sets.mutable.of("<default, root.firm>", "<default, root.address>"), xStoreServiceRunner.getGraphFetchCrossAssociationKeys().stream().map(GraphFetchCrossAssociationKeys::getName).collect(Collectors.toSet()));

        ExecutionCache<GraphFetchCacheKey, List<Object>> firmCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        ExecutionCache<GraphFetchCacheKey, List<Object>> addressCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        OperationalContext operationalContext = OperationalContext
                .newInstance()
                .withGraphFetchCrossAssociationKeysCacheConfig(Maps.mutable.of(
                        xStoreServiceRunner.getGraphFetchCrossAssociationKeys().stream().filter(x -> "<default, root.firm>".equals(x.getName())).findFirst().orElse(null), firmCache,
                        xStoreServiceRunner.getGraphFetchCrossAssociationKeys().stream().filter(x -> "<default, root.address>".equals(x.getName())).findFirst().orElse(null), addressCache
                ));

        String expectedRes = "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\"},\"address\":{\"name\":\"A1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\"},\"address\":{\"name\":\"A2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null,\"address\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null,\"address\":{\"name\":\"A3\"}}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\"},\"address\":{\"name\":\"A1\"}}" +
                "]";

        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(firmCache, 3, 5, 2, 3);
        assertCacheStats(addressCache, 4, 5, 1, 4);
        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(firmCache, 3, 10, 7, 3);
        assertCacheStats(addressCache, 4, 10, 6, 4);
    }

    @Test
    public void testXStoreServiceExecutionWithDeepCrossPropertyAccessSharedCaches() throws JavaCompileException
    {
        XStoreServiceRunnerWithDeepCrossPropertyAccess xStoreServiceRunner = new XStoreServiceRunnerWithDeepCrossPropertyAccess();
        Assert.assertEquals(3, xStoreServiceRunner.getGraphFetchCrossAssociationKeys().size());
        Assert.assertEquals(Sets.mutable.of("<default, root.firm>", "<default, root.address>", "<default, root.firm.address>"), xStoreServiceRunner.getGraphFetchCrossAssociationKeys().stream().map(GraphFetchCrossAssociationKeys::getName).collect(Collectors.toSet()));

        ExecutionCache<GraphFetchCacheKey, List<Object>> firmCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        ExecutionCache<GraphFetchCacheKey, List<Object>> addressCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        OperationalContext operationalContext = OperationalContext
                .newInstance()
                .withGraphFetchCrossAssociationKeysCacheConfig(Maps.mutable.of(
                        xStoreServiceRunner.getGraphFetchCrossAssociationKeys().stream().filter(x -> "<default, root.firm>".equals(x.getName())).findFirst().orElse(null), firmCache,
                        xStoreServiceRunner.getGraphFetchCrossAssociationKeys().stream().filter(x -> "<default, root.address>".equals(x.getName())).findFirst().orElse(null), addressCache,
                        xStoreServiceRunner.getGraphFetchCrossAssociationKeys().stream().filter(x -> "<default, root.firm.address>".equals(x.getName())).findFirst().orElse(null), addressCache
                ));

        String expectedRes = "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}},\"address\":{\"name\":\"A1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\",\"address\":{\"name\":\"A3\"}},\"address\":{\"name\":\"A2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null,\"address\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null,\"address\":{\"name\":\"A3\"}}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}},\"address\":{\"name\":\"A1\"}}" +
                "]";

        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(firmCache, 3, 5, 2, 3);
        assertCacheStats(addressCache, 5, 7, 2, 5);
        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(firmCache, 3, 10, 7, 3);
        assertCacheStats(addressCache, 5, 12, 7, 5);
    }

    @Test
    public void testServiceRunnerGraphFetchBatchMemoryLimit()
    {
        ServiceRunnerInput serviceRunnerInput1 = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList("{\"fullName\": \"Peter Smith\"}"))
                .withSerializationFormat(SerializationFormat.PURE);

        ServiceRunnerInput serviceRunnerInput2 = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList("[{\"fullName\": \"Peter Smith\"}, {\"fullName\": \"John Hill\"}]"))
                .withSerializationFormat(SerializationFormat.PURE);


        // Service with default batch size (1 in case of M2M)
        SimpleM2MServiceRunner simpleM2MServiceRunner = new SimpleM2MServiceRunner();

        simpleM2MServiceRunner.setGraphFetchBatchMemoryLimit(1);
        Exception e1 = Assert.assertThrows(RuntimeException.class, () -> simpleM2MServiceRunner.run(serviceRunnerInput1));
        Assert.assertEquals("Maximum memory reached when processing the graphFetch. Try reducing batch size of graphFetch fetch operation.", e1.getMessage());

        simpleM2MServiceRunner.setGraphFetchBatchMemoryLimit(200);
        Assert.assertEquals("{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}", simpleM2MServiceRunner.run(serviceRunnerInput1));
        Assert.assertEquals("[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"}]", simpleM2MServiceRunner.run(serviceRunnerInput2));


        // Service which has graph fetch batch size set
        SimpleM2MServiceRunnerWthGraphFetchBatchSize simpleM2MServiceRunnerWthGraphFetchBatchSize = new SimpleM2MServiceRunnerWthGraphFetchBatchSize();

        simpleM2MServiceRunnerWthGraphFetchBatchSize.setGraphFetchBatchMemoryLimit(1);
        Exception e2 = Assert.assertThrows(RuntimeException.class, () -> simpleM2MServiceRunnerWthGraphFetchBatchSize.run(serviceRunnerInput1));
        Assert.assertEquals("Maximum memory reached when processing the graphFetch. Try reducing batch size of graphFetch fetch operation.", e2.getMessage());

        simpleM2MServiceRunnerWthGraphFetchBatchSize.setGraphFetchBatchMemoryLimit(200);
        Assert.assertEquals("{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}", simpleM2MServiceRunnerWthGraphFetchBatchSize.run(serviceRunnerInput1));

        Exception e3 = Assert.assertThrows(RuntimeException.class, () -> simpleM2MServiceRunnerWthGraphFetchBatchSize.run(serviceRunnerInput2));
        Assert.assertEquals("Maximum memory reached when processing the graphFetch. Try reducing batch size of graphFetch fetch operation.", e3.getMessage());

        simpleM2MServiceRunnerWthGraphFetchBatchSize.setGraphFetchBatchMemoryLimit(400);
        Assert.assertEquals("[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"}]", simpleM2MServiceRunnerWthGraphFetchBatchSize.run(serviceRunnerInput2));
    }

    private static class SimpleM2MServiceRunner extends AbstractServicePlanExecutor
    {
        SimpleM2MServiceRunner()
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/simpleM2MService.pure", "test::function"), true);
        }

        @Override
        public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
        {
            newSingleParameterExecutionBuilder()
                    .withParameter("input", serviceRunnerInput.getArgs().get(0))
                    .withServiceRunnerInput(serviceRunnerInput)
                    .executeToStream(outputStream);
        }
    }

    private static class MultiParameterM2MServiceRunner extends AbstractServicePlanExecutor
    {
        MultiParameterM2MServiceRunner()
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/multiParamM2MService.pure", "test::function"), true);
        }

        @Override
        public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
        {
            newExecutionBuilder()
                    .withParameter("input1", serviceRunnerInput.getArgs().get(0))
                    .withParameter("input2", serviceRunnerInput.getArgs().get(1))
                    .withServiceRunnerInput(serviceRunnerInput)
                    .executeToStream(outputStream);
        }
    }

    private static class SimpleM2MServiceRunnerWthGraphFetchBatchSize extends AbstractServicePlanExecutor
    {
        SimpleM2MServiceRunnerWthGraphFetchBatchSize()
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/simpleM2MService.pure", "test::functionWithBatchSize"), true);
        }

        @Override
        public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
        {
            newSingleParameterExecutionBuilder()
                    .withParameter("input", serviceRunnerInput.getArgs().get(0))
                    .withServiceRunnerInput(serviceRunnerInput)
                    .executeToStream(outputStream);
        }
    }

    private static class SimpleRelationalServiceRunner extends AbstractServicePlanExecutor
    {
        SimpleRelationalServiceRunner()
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/simpleRelationalService.pure", "test::fetch"), true);
        }

        @Override
        public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
        {
            newExecutionBuilder()
                    .withParameter("ip", serviceRunnerInput.getArgs().get(0))
                    .withServiceRunnerInput(serviceRunnerInput)
                    .executeToStream(outputStream);
        }
    }

    private static abstract class AbstractXStoreServiceRunner extends AbstractServicePlanExecutor
    {
        private final EngineJavaCompiler compiler;

        AbstractXStoreServiceRunner(String servicePath, SingleExecutionPlan plan) throws JavaCompileException
        {
            super(servicePath, plan, false);
            compiler = JavaHelper.compilePlan(plan, null);
        }

        @Override
        public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
        {
            try (JavaHelper.ThreadContextClassLoaderScope ignored = JavaHelper.withCurrentThreadContextClassLoader(compiler.getClassLoader()))
            {
                newExecutionBuilder().withServiceRunnerInput(serviceRunnerInput).executeToStream(outputStream);
            }
        }
    }

    private static class XStoreServiceRunnerWithNoCrossPropertyAccess extends AbstractXStoreServiceRunner
    {
        private static final SingleExecutionPlan plan = buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/xStorePropertyAccessServices.pure", "test::fetch1");

        XStoreServiceRunnerWithNoCrossPropertyAccess() throws JavaCompileException
        {
            super("test::Service", plan);
        }
    }

    private static class XStoreServiceRunnerWithSingleCrossPropertyAccess extends AbstractXStoreServiceRunner
    {
        private static final SingleExecutionPlan plan = buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/xStorePropertyAccessServices.pure", "test::fetch2");

        XStoreServiceRunnerWithSingleCrossPropertyAccess() throws JavaCompileException
        {
            super("test::Service", plan);
        }
    }

    private static class XStoreServiceRunnerWithToManyCrossPropertyAccess extends AbstractXStoreServiceRunner
    {
        private static final SingleExecutionPlan plan = buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/xStorePropertyAccessServices.pure", "test::fetch3");

        XStoreServiceRunnerWithToManyCrossPropertyAccess() throws JavaCompileException
        {
            super("test::Service", plan);
        }
    }

    private static class XStoreServiceRunnerWithMultiCrossPropertyAccess extends AbstractXStoreServiceRunner
    {
        private static final SingleExecutionPlan plan = buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/xStorePropertyAccessServices.pure", "test::fetch4");

        XStoreServiceRunnerWithMultiCrossPropertyAccess() throws JavaCompileException
        {
            super("test::Service", plan);
        }
    }

    private static class XStoreServiceRunnerWithDeepCrossPropertyAccess extends AbstractXStoreServiceRunner
    {
        private static final SingleExecutionPlan plan = buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/xStorePropertyAccessServices.pure", "test::fetch5");

        XStoreServiceRunnerWithDeepCrossPropertyAccess() throws JavaCompileException
        {
            super("test::Service", plan);
        }
    }

    private static SingleExecutionPlan buildPlanForFetchFunction(String modelCodeResource, String fetchFunctionName)
    {
        InputStreamReader inputStreamReader = new InputStreamReader(Objects.requireNonNull(TestServiceRunner.class.getResourceAsStream(modelCodeResource)));
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(bufferedReader.lines().collect(Collectors.joining("\n")));
        PureModel pureModel = Compiler.compile(contextData, null, null);

        Function fetchFunction = contextData.getElementsOfType(Function.class).stream().filter(x -> fetchFunctionName.equals(x._package + "::" + x.name)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown function"));

        return PlanGenerator.generateExecutionPlan(
                HelperValueSpecificationBuilder.buildLambda(fetchFunction.body, fetchFunction.parameters, new CompileContext.Builder(pureModel).build()),
                pureModel.getMapping("test::Map"),
                pureModel.getRuntime("test::Runtime"),
                null,
                pureModel,
                "vX_X_X",
                PlanPlatform.JAVA,
                null,
                core_relational_relational_router_router_extension.Root_meta_pure_router_extension_defaultRelationalExtensions__RouterExtension_MANY_(pureModel.getExecutionSupport()),
                LegendPlanTransformers.transformers
        );
    }

    private static void assertCacheStats(ExecutionCache<?,?> cache, int estimatedSize, int requestCount, int hitCount, int missCount)
    {
        Assert.assertEquals(estimatedSize, cache.estimatedSize());
        Assert.assertEquals(requestCount, cache.stats().requestCount());
        Assert.assertEquals(hitCount, cache.stats().hitCount());
        Assert.assertEquals(missCount, cache.stats().missCount());
    }
}
