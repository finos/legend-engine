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

package org.finos.legend.engine.plan.execution.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PackageableElementFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PackageableElementSecondPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.service.test.JavaCode;
import org.finos.legend.engine.plan.execution.service.test.SingleTestRun;
import org.finos.legend.engine.plan.execution.service.test.TestResult;
import org.finos.legend.engine.plan.execution.service.test.TestRun;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemory;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.plan.execution.stores.service.plugin.ServiceStore;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.service.post.validation.runner.LegendServicePostValidationRunner;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.engine.shared.core.operational.prometheus.Prometheus;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.finos.legend.engine.test.runner.service.RichServiceTestResult;
import org.finos.legend.engine.test.runner.service.ServiceTestRunner;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.pac4j.core.profile.CommonProfile;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static org.finos.legend.pure.generated.core_relational_relational_extensions_extension.Root_meta_relational_extension_relationalExtensions__Extension_MANY_;

public class ServiceModeling
{
    public static ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final PlanExecutor planExecutor = PlanExecutor.newPlanExecutor(Relational.build(), ServiceStore.build(), InMemory.build()); //dont add the serviceStore

    private final ModelManager modelManager;
    private final DeploymentMode deploymentMode;

    public ServiceModeling(ModelManager modelManager, DeploymentMode deploymentMode)
    {
        this.modelManager = modelManager;
        this.deploymentMode = deploymentMode;
        MetricsHandler.createMetrics(this.getClass());
    }

    public Root_meta_legend_service_metamodel_Service compileService(Service service, CompileContext compileContext)
    {
        // If we're recompiling an existing service remove the original first
        Package pkg = compileContext.pureModel.getOrCreatePackage(service._package);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement existing = pkg._children().detect(c -> c._name().equals(service.name));
        if (existing != null)
        {
            pkg._childrenRemove(existing);
        }

        Root_meta_legend_service_metamodel_Service compiledService = (Root_meta_legend_service_metamodel_Service) service.accept(new PackageableElementFirstPassBuilder(compileContext));
        service.accept(new PackageableElementSecondPassBuilder(compileContext));
        return compiledService;
    }

    @Prometheus(name = "service test model resolve", doc = "Model resolution duration summary within service test execution")
    public List<TestResult> testService(MutableList<CommonProfile> profiles, PureModelContext context, String metricsContext)
    {
        Service invokedService = null;
        try
        {
            long start = System.currentTimeMillis();
            PureModelContextData data = ((PureModelContextData) context).shallowCopy();
            Service service = (Service) Iterate.detect(data.getElements(), e -> e instanceof Service);
            invokedService = service;
            PureModelContextData dataWithoutService = PureModelContextData.newBuilder().withOrigin(data.getOrigin()).withSerializer(data.getSerializer()).withElements(LazyIterate.select(data.getElements(), e -> e != service)).build();
            PureModel pureModel  = new PureModel(dataWithoutService, profiles, deploymentMode);
            Pair<PureModelContextData, PureModel> pureModelAndData  = Tuples.pair(dataWithoutService, pureModel);
            long end = System.currentTimeMillis();
            MetricsHandler.observe("service test model resolve", start, end);
            MetricsHandler.observeServerOperation("model_resolve", metricsContext, start, end);

            if (service.execution instanceof PureMultiExecution)
            {
                throw new UnsupportedOperationException("MultiExecutions not yet supported");
            }
            Root_meta_legend_service_metamodel_Service pureService = compileService(service, pureModel.getContext(service));

            TestRun testRun = executeTests(service, pureService, pureModelAndData, PureClientVersions.production, metricsContext);
            if (!(testRun instanceof SingleTestRun))
            {
                throw new UnsupportedOperationException("Expected a single test run result");
            }

            return ((SingleTestRun) testRun).results.entrySet().stream().map(x -> new TestResult(x.getKey(), x.getValue())).collect(Collectors.toList());
        }
        catch (IOException | JavaCompileException e)
        {
            MetricsHandler.observeError(LoggingEventType.MODEL_RESOLVE_ERROR, e, invokedService == null ? null : invokedService.pattern);
            throw new RuntimeException(e);
        }
    }

    private static TestRun executeTests(Service service, Root_meta_legend_service_metamodel_Service pureService, Pair<PureModelContextData, PureModel> pureModelPairs, String pureVersion, String metricsContext) throws IOException, JavaCompileException
    {
        MutableList<PlanGeneratorExtension> extensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = extensions.flatCollect(e -> e.getExtraExtensions(pureModelPairs.getTwo()));
        MutableList<PlanTransformer> planTransformers = extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);

        ServiceTestRunner runner = new ServiceTestRunner(service, pureService, pureModelPairs.getOne(), pureModelPairs.getTwo(), objectMapper, planExecutor, routerExtensions, planTransformers, pureVersion, metricsContext);
        RichServiceTestResult richServiceTestResult = runner.executeTests().get(0);

        Map<String, Boolean> results = Maps.mutable.empty();
        for (Map.Entry<String, org.finos.legend.engine.test.runner.shared.TestResult> entry : richServiceTestResult.getResults().entrySet())
        {
            Boolean testResult = entry.getValue().equals(org.finos.legend.engine.test.runner.shared.TestResult.SUCCESS);
            results.put(entry.getKey(), testResult);
        }
        return new SingleTestRun(richServiceTestResult.getExecutionPlan(), new JavaCode(richServiceTestResult.getJavaCodeString()), results);
    }

    @Prometheus(name = "service validation model resolve", doc = "Model resolution duration summary within service validation execution")
    public Response validateService(MutableList<CommonProfile> profiles, PureModelContext context, String metricsContext, String assertionId, SerializationFormat format)
    {
        long start = System.currentTimeMillis();
        PureModelContextData data = ((PureModelContextData) context).shallowCopy();
        Service service = (Service) Iterate.detect(data.getElements(), e -> e instanceof Service);
        PureModelContextData dataWithoutService = PureModelContextData.newBuilder().withOrigin(data.getOrigin()).withSerializer(data.getSerializer()).withElements(LazyIterate.select(data.getElements(), e -> e != service)).build();
        PureModel pureModel = new PureModel(dataWithoutService, profiles, deploymentMode);
        long end = System.currentTimeMillis();
        MetricsHandler.observe("service validation model resolve", start, end);
        MetricsHandler.observeServerOperation("model_resolve", metricsContext, start, end);

        Root_meta_legend_service_metamodel_Service pureService = compileService(service, pureModel.getContext(service));
        List<Variable> rawParams = ((PureExecution) service.execution).func.parameters;

        LegendServicePostValidationRunner postValidationRunner = new LegendServicePostValidationRunner(pureModel, pureService, rawParams, Root_meta_relational_extension_relationalExtensions__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers, PureClientVersions.production, profiles, format);
        return postValidationRunner.runValidationAssertion(assertionId);
    }
}
