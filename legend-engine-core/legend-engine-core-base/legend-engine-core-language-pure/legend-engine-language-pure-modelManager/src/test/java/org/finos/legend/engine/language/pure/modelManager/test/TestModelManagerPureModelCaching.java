// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.pure.modelManager.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Span;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelLoader;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextCombination;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureSDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.collections.api.tuple.Pair;

public class TestModelManagerPureModelCaching
{
    private static ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testPureModelCaching()
    {
        ModelManager manager = new ModelManager(DeploymentMode.TEST, new MockModelLoader(Maps.mutable.empty()));
        Protocol protocol = new Protocol("pure", "v1_17_0");
        PackageableElementPointer element0 = new PackageableElementPointer(PackageableElementType.MAPPING, "meta::relational::tests::milestoning::milestoningmap");
        PackageableElementPointer element1 = new PackageableElementPointer(PackageableElementType.STORE, "meta::relational::tests::dbInc");
        PackageableElementPointer element2 = new PackageableElementPointer(PackageableElementType.STORE, "meta::relational::tests::db");
        List<PackageableElementPointer> list = new ArrayList<>();
        list.add(element0);
        list.add(element1);
        list.add(element2);
        PureSDLC pureSDLC = new PureSDLC();
        pureSDLC.packageableElementPointers = list;
        PureModelContextPointer pointer = new PureModelContextPointer();
        pointer.serializer = protocol;
        pointer.sdlcInfo = pureSDLC;
        PureModelContext context = pointer;
        manager.loadModel(context, null, new Identity("X"), null);
        Assert.assertEquals(1, manager.pureModelCache.size());
    }

    @Test
    public void testCompositeContext() throws Exception
    {
        PureModelContextPointer pointerA = mapper.readValue(
                "{\n" +
                        "    \"_type\": \"pointer\",\n" +
                        "    \"sdlcInfo\": {\n" +
                        "      \"_type\": \"alloy\",\n" +
                        "      \"baseVersion\": \"latest\",\n" +
                        "      \"version\": \"4.3.0\",\n" +
                        "      \"groupId\": \"com.alloy.servicestest\",\n" +
                        "      \"artifactId\": \"test\",\n" +
                        "      \"packageableElementPointers\": []\n" +
                        "     }\n" +
                        "}", PureModelContextPointer.class);


        PureModelContextData blockA = PureGrammarParser.newInstance().parseModel(
                "Class x::A\n" +
                        "{\n" +
                        "   name : String[1];" +
                        "}");

        MutableMap<PureModelContextPointer, PureModelContextData> vals = Maps.mutable.with(pointerA, blockA);

        PureModelContextData blockB = PureGrammarParser.newInstance().parseModel(
                "Class x::B\n" +
                        "{\n" +
                        "   name : x::A[1];" +
                        "}");

        PureModelContextCombination combination = new PureModelContextCombination();
        combination.contexts = Lists.mutable.with(pointerA, blockB);

        ModelManager manager = new ModelManager(DeploymentMode.TEST, new MockModelLoader(vals));
        PureModel pureModel = manager.loadModel(combination, null, new Identity("X"), null);
        Assert.assertNotNull(pureModel.getClass("x::A"));
        Assert.assertNotNull(pureModel.getClass("x::B"));
    }

    @Test
    public void testCompositeContextNested() throws Exception
    {
        PureModelContextPointer pointerA = mapper.readValue(
                "{\n" +
                        "    \"_type\": \"pointer\",\n" +
                        "    \"sdlcInfo\": {\n" +
                        "      \"_type\": \"alloy\",\n" +
                        "      \"baseVersion\": \"latest\",\n" +
                        "      \"version\": \"4.3.0\",\n" +
                        "      \"groupId\": \"com.alloy.servicestest\",\n" +
                        "      \"artifactId\": \"test\",\n" +
                        "      \"packageableElementPointers\": []\n" +
                        "     }\n" +
                        "}", PureModelContextPointer.class);


        PureModelContextPointer pointerC = mapper.readValue(
                "{\n" +
                        "    \"_type\": \"pointer\",\n" +
                        "    \"sdlcInfo\": {\n" +
                        "      \"_type\": \"alloy\",\n" +
                        "      \"baseVersion\": \"latest\",\n" +
                        "      \"version\": \"4.3.0\",\n" +
                        "      \"groupId\": \"com.alloy.otherOne\",\n" +
                        "      \"artifactId\": \"test\",\n" +
                        "      \"packageableElementPointers\": []\n" +
                        "     }\n" +
                        "}", PureModelContextPointer.class);

        PureModelContextData blockA = PureGrammarParser.newInstance().parseModel(
                "Class x::A\n" +
                        "{\n" +
                        "   name : String[1];" +
                        "}");

        PureModelContextData blockC = PureGrammarParser.newInstance().parseModel(
                "Class x::C\n" +
                        "{\n" +
                        "   name : x::A[1];" +
                        "   val : x::D[1];" +
                        "}");

        PureModelContextData blockD = PureGrammarParser.newInstance().parseModel(
                "Class x::D\n" +
                        "{\n" +
                        "   name : x::A[1];" +
                        "}");

        MutableMap<PureModelContextPointer, PureModelContextData> vals = Maps.mutable.with(pointerA, blockA, pointerC, blockC);

        PureModelContextCombination subCombination = new PureModelContextCombination();
        subCombination.contexts = Lists.mutable.with(pointerC, blockD);

        PureModelContextCombination combination = new PureModelContextCombination();
        combination.contexts = Lists.mutable.with(pointerA, subCombination);

        ModelManager manager = new ModelManager(DeploymentMode.TEST, new MockModelLoader(vals));
        PureModel pureModel = manager.loadModel(combination, null, new Identity("X"), null);
        Assert.assertNotNull(pureModel.getClass("x::A"));
        Assert.assertNotNull(pureModel.getClass("x::C"));
        Assert.assertNotNull(pureModel.getClass("x::D"));
    }

    @Test
    public void testLoadModelCachesCompiledModel()
    {
        PureModelContextData blockA = PureGrammarParser.newInstance().parseModel(
                "Class x::A\n{\n   name : String[1];\n}");

        PureModelContextPointer pointer = makePointer("com.test", "artifact1", "1.0.0");

        AtomicInteger loadCount = new AtomicInteger(0);
        CountingMockModelLoader loader = new CountingMockModelLoader(Maps.mutable.with(pointer, blockA), loadCount);
        ModelManager manager = new ModelManager(DeploymentMode.TEST, loader);
        Identity identity = new Identity("X");

        PureModel model1 = manager.loadModel(pointer, null, identity, null);
        Assert.assertNotNull(model1);
        Assert.assertNotNull(model1.getClass("x::A"));
        Assert.assertEquals(1, manager.pureModelCache.size());
        Assert.assertEquals(1, loadCount.get());

        // Second call should hit cache — loader not called again
        PureModel model2 = manager.loadModel(pointer, null, identity, null);
        Assert.assertSame(model1, model2);
        Assert.assertEquals(1, loadCount.get());
    }

    @Test
    public void testLoadDataCachesResolvedData()
    {
        PureModelContextData blockA = PureGrammarParser.newInstance().parseModel(
                "Class x::A\n{\n   name : String[1];\n}");

        PureModelContextPointer pointer = makePointer("com.test", "artifact2", "1.0.0");

        AtomicInteger loadCount = new AtomicInteger(0);
        CountingMockModelLoader loader = new CountingMockModelLoader(Maps.mutable.with(pointer, blockA), loadCount);
        ModelManager manager = new ModelManager(DeploymentMode.TEST, loader);
        Identity identity = new Identity("X");

        PureModelContextData data1 = manager.loadData(pointer, null, identity);
        Assert.assertNotNull(data1);
        Assert.assertEquals(1, manager.pureModelContextCache.size());
        Assert.assertEquals(1, loadCount.get());

        // Second call should hit cache — loader not called again
        PureModelContextData data2 = manager.loadData(pointer, null, identity);
        Assert.assertSame(data1, data2);
        Assert.assertEquals(1, loadCount.get());
    }

    @Test
    public void testLoadModelAndDataCachesBothAndLoadsOnlyOnce()
    {
        PureModelContextData blockA = PureGrammarParser.newInstance().parseModel(
                "Class x::A\n{\n   name : String[1];\n}");

        PureModelContextPointer pointer = makePointer("com.test", "artifact3", "1.0.0");

        AtomicInteger loadCount = new AtomicInteger(0);
        CountingMockModelLoader loader = new CountingMockModelLoader(Maps.mutable.with(pointer, blockA), loadCount);
        ModelManager manager = new ModelManager(DeploymentMode.TEST, loader);
        Identity identity = new Identity("X");

        Pair<PureModelContextData, PureModel> result = manager.loadModelAndData(pointer, null, identity, null);

        // Verify output
        PureModelContextData data = result.getOne();
        PureModel model = result.getTwo();
        Assert.assertNotNull(data);
        Assert.assertNotNull(model);
        Assert.assertNotNull(model.getClass("x::A"));

        // Both caches populated
        Assert.assertEquals(1, manager.pureModelContextCache.size());
        Assert.assertEquals(1, manager.pureModelCache.size());

        // Loader called only once (preResolvedData used for model compilation)
        Assert.assertEquals(1, loadCount.get());
    }

    @Test
    public void testLoadModelAndDataSubsequentCallsHitCache()
    {
        PureModelContextData blockA = PureGrammarParser.newInstance().parseModel(
                "Class x::A\n{\n   name : String[1];\n}");

        PureModelContextPointer pointer = makePointer("com.test", "artifact4", "1.0.0");

        AtomicInteger loadCount = new AtomicInteger(0);
        CountingMockModelLoader loader = new CountingMockModelLoader(Maps.mutable.with(pointer, blockA), loadCount);
        ModelManager manager = new ModelManager(DeploymentMode.TEST, loader);
        Identity identity = new Identity("X");

        Pair<PureModelContextData, PureModel> result1 = manager.loadModelAndData(pointer, null, identity, null);
        Assert.assertEquals(1, loadCount.get());

        // Second call — everything from cache
        Pair<PureModelContextData, PureModel> result2 = manager.loadModelAndData(pointer, null, identity, null);
        Assert.assertSame(result1.getOne(), result2.getOne());
        Assert.assertSame(result1.getTwo(), result2.getTwo());
        Assert.assertEquals(1, loadCount.get());
    }

    @Test
    public void testLoadModelAndDataThenIndividualCallsHitCache()
    {
        PureModelContextData blockA = PureGrammarParser.newInstance().parseModel(
                "Class x::A\n{\n   name : String[1];\n}");

        PureModelContextPointer pointer = makePointer("com.test", "artifact5", "1.0.0");

        AtomicInteger loadCount = new AtomicInteger(0);
        CountingMockModelLoader loader = new CountingMockModelLoader(Maps.mutable.with(pointer, blockA), loadCount);
        ModelManager manager = new ModelManager(DeploymentMode.TEST, loader);
        Identity identity = new Identity("X");

        Pair<PureModelContextData, PureModel> result = manager.loadModelAndData(pointer, null, identity, null);
        Assert.assertEquals(1, loadCount.get());

        // Subsequent individual calls use the caches populated by loadModelAndData
        PureModelContextData data = manager.loadData(pointer, null, identity);
        Assert.assertSame(result.getOne(), data);

        PureModel model = manager.loadModel(pointer, null, identity, null);
        Assert.assertSame(result.getTwo(), model);

        Assert.assertEquals(1, loadCount.get());
    }

    private static PureModelContextPointer makePointer(String groupId, String artifactId, String version)
    {
        PureModelContextPointer pointer = new PureModelContextPointer();
        pointer.serializer = new Protocol("pure", "v1_17_0");
        AlloySDLC sdlc = new AlloySDLC();
        sdlc.groupId = groupId;
        sdlc.artifactId = artifactId;
        sdlc.version = version;
        sdlc.packageableElementPointers = new ArrayList<>();
        pointer.sdlcInfo = sdlc;

        // Use a simple approach: set fields that affect equality
        pointer.sdlcInfo.packageableElementPointers = new ArrayList<>();
        return pointer;
    }

    private static class CountingMockModelLoader implements ModelLoader
    {
        private final MutableMap<PureModelContextPointer, PureModelContextData> contexts;
        private final AtomicInteger loadCount;

        public CountingMockModelLoader(MutableMap<PureModelContextPointer, PureModelContextData> vals, AtomicInteger loadCount)
        {
            this.contexts = vals;
            this.loadCount = loadCount;
        }

        @Override
        public boolean supports(PureModelContext context)
        {
            return true;
        }

        @Override
        public void setModelManager(ModelManager modelManager)
        {
        }

        @Override
        public PureModelContextData load(Identity identity, PureModelContext context, String clientVersion, Span parentSpan)
        {
            loadCount.incrementAndGet();
            PureModelContextPointer pointer = (PureModelContextPointer) context;
            PureModelContextData res = contexts.get(context);
            return res != null ? res : PureModelContextData.newPureModelContextData(pointer.serializer, pointer, Lists.mutable.empty());
        }

        @Override
        public PureModelContext cacheKey(PureModelContext context, Identity identity)
        {
            return context;
        }

        @Override
        public boolean shouldCache(PureModelContext context)
        {
            return true;
        }
    }

    private static class MockModelLoader implements ModelLoader
    {
        private final MutableMap<PureModelContextPointer, PureModelContextData> contexts;

        public MockModelLoader(MutableMap<PureModelContextPointer, PureModelContextData> vals)
        {
            contexts = vals;
        }

        @Override
        public boolean supports(PureModelContext context)
        {
            return true;
        }

        @Override
        public void setModelManager(ModelManager modelManager)
        {

        }

        @Override
        public PureModelContextData load(Identity identity, PureModelContext context, String clientVersion, Span parentSpan)
        {
            PureModelContextPointer pointer = (PureModelContextPointer) context;
            PureModelContextData res = contexts.get(context);
            return res != null ? res : PureModelContextData.newPureModelContextData(pointer.serializer, pointer, Lists.mutable.empty());
        }

        @Override
        public PureModelContext cacheKey(PureModelContext context, Identity identity)
        {
            return context;
        }

        @Override
        public boolean shouldCache(PureModelContext context)
        {
            return this.supports(context);
        }
    }
}
