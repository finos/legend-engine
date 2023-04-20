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


package org.finos.legend.engine.test.runner.mapping.api;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest_Legacy;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.test.runner.mapping.MappingTestRunner;
import org.finos.legend.engine.test.runner.mapping.RichMappingTestResult;
import org.finos.legend.engine.test.runner.mapping.api.LegacyMappingTestRunnerResult.LegacyMappingTestResult;
import org.finos.legend.engine.test.runner.shared.TestResult;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.pac4j.core.profile.CommonProfile;

public class LegacyMappingRunner
{

    private final ModelManager modelManager;
    private final MutableList<PlanGeneratorExtension> extensions;
    private final PlanExecutor planExecutor;

    public LegacyMappingRunner(ModelManager modelManager)
    {
        this.modelManager = modelManager;
        this.extensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        this.planExecutor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
    }

    public LegacyMappingTestRunnerResult runTests(LegacyMappingTestRunnerInput input, MutableList<CommonProfile> profiles)
    {
        Pair<PureModelContextData, PureModel> modelAndData = modelManager.loadModelAndData(input.model,
            input.model instanceof PureModelContextPointer ? ((PureModelContextPointer) input.model).serializer.version : null, profiles, null);
        PureModel pureModel = modelAndData.getTwo();
        PureModelContextData data = modelAndData.getOne();
        Mapping mapping = data.getElementsOfType(Mapping.class).stream().filter(e -> e.getPath().equals(input.mapping)).findFirst()
            .orElseThrow(() -> new EngineException("Cant find mapping " + input.mapping));
        RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = extensions.flatCollect(e -> e.getExtraExtensions(pureModel));
        LegacyMappingTestRunnerResult runnerResult = new LegacyMappingTestRunnerResult();
        MutableList<PlanTransformer> planTransformers = extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);

        List<Integer> mappingIndexes = input.mappingTestIndexes;
        if (mappingIndexes == null || mappingIndexes.isEmpty())
        {
            mappingIndexes = mapping.tests.stream().map((e) -> mapping.tests.indexOf(e)).collect(Collectors.toList());
        }
        try
        {
            mappingIndexes.forEach(e -> mapping.tests.get(e));
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new EngineException("Invalid integer:" + e.getMessage());
        }

        runnerResult.results =  mappingIndexes.stream().map(idx -> runTest(pureModel, input.mapping, mapping.tests.get(idx), idx, routerExtensions, planTransformers))
            .collect(Collectors.toList());
        return runnerResult;

    }

    private LegacyMappingTestResult runTest(PureModel pureModel, String mappingPath, MappingTest_Legacy test, Integer idx,
        RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions, MutableList<PlanTransformer> planTransformers)
    {
        try
        {
            MappingTestRunner runner = new MappingTestRunner(pureModel, mappingPath, test, this.planExecutor, routerExtensions, planTransformers);
            RichMappingTestResult testResult = runner.setupAndRunTest();
            return this.buildTestResult(testResult, idx);
        }
        catch (Exception error)
        {
            return new LegacyMappingTestResult(TestResult.ERROR,idx, mappingPath, test.name, null, null, error.getMessage());
        }

    }

    private LegacyMappingTestResult buildTestResult(RichMappingTestResult testResult, Integer idx)
    {
        String expected = testResult.getResult() == TestResult.FAILURE && testResult.getExpected().isPresent() ? testResult.getExpected().get() : null;
        String actual = testResult.getResult() == TestResult.FAILURE && testResult.getActual().isPresent() ? testResult.getActual().get() : null;
        String error = null;
        if (testResult.getException() != null)
        {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            testResult.getException().printStackTrace(printWriter);
            error = stringWriter.toString();
        }
       return new LegacyMappingTestResult(testResult.getResult(), idx, testResult.getMappingPath(), testResult.getTestName(),
            expected, actual, error);

    }





}
