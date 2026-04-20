// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.api;

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.test.GrammarParseTestUtils;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.query.sql.providers.core.SQLContext;
import org.finos.legend.engine.query.sql.providers.core.SQLSource;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceArgument;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceProvider;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceResolvedContext;
import org.finos.legend.engine.query.sql.providers.core.TableSource;
import org.finos.legend.engine.query.sql.providers.core.TableSourceArgument;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public class TestSQLSourceProvider implements SQLSourceProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TestSQLSourceProvider.class);
    private static final PureModelContextData pureModelContextData = GrammarParseTestUtils.loadPureModelContextFromResource("proj-1.pure", TestSQLSourceProvider.class);
    private static final String SERVICE_TYPE = "service";

    private final boolean enablePreGeneratedPlans;
    private final Map<String, ExecutionPlan> preGeneratedPlans;
    private PureModel compiledModel;
    private RichIterable<? extends Root_meta_pure_extension_Extension> extensions;
    private Iterable<? extends PlanTransformer> transformers;

    public TestSQLSourceProvider()
    {
        this(false);
    }

    public TestSQLSourceProvider(boolean enablePreGeneratedPlans)
    {
        this.enablePreGeneratedPlans = enablePreGeneratedPlans;
        this.preGeneratedPlans = Maps.mutable.empty();

        if (enablePreGeneratedPlans)
        {
            initializePlans();
        }
    }

    /**
     * Initialize pre-generated execution plans for all services.
     * This simulates what AlloyServiceState does when services are registered.
     */
    private void initializePlans()
    {
        try
        {
            ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
            this.compiledModel = modelManager.loadModel(pureModelContextData, PureClientVersions.production, Identity.getAnonymousIdentity(), "");

            this.extensions = PureCoreExtensionLoader.extensions().flatCollect(g -> g.extraPureCoreExtensions(compiledModel.getExecutionSupport()));
            MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
            this.transformers = generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);

            LazyIterable<Service> services = LazyIterate.select(pureModelContextData.getElements(), e -> e instanceof Service)
                    .collect(e -> (Service) e);

            for (Service service : services)
            {
                if (service.execution instanceof PureSingleExecution)
                {
                    try
                    {
                        ExecutionPlan plan = generatePlanForService(service, (PureSingleExecution) service.execution);
                        if (plan != null)
                        {
                            preGeneratedPlans.put(service.pattern, plan);
                            LOGGER.debug("Pre-generated plan for service: {}", service.pattern);
                        }
                    }
                    catch (Exception e)
                    {
                        LOGGER.warn("Failed to generate plan for service {}: {}", service.pattern, e.getMessage());
                    }
                }
            }

            LOGGER.info("Initialized {} pre-generated execution plans", preGeneratedPlans.size());
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to initialize pre-generated plans: {}", e.getMessage(), e);
        }
    }

    private ExecutionPlan generatePlanForService(Service service, PureSingleExecution execution)
    {
        LambdaFunction<?> pureLambda = HelperValueSpecificationBuilder.buildLambda(execution.func.body, execution.func.parameters, compiledModel.getContext());

        Mapping mapping = compiledModel.getMapping(execution.mapping);
        Root_meta_core_runtime_Runtime runtime;

        if (execution.runtime instanceof RuntimePointer)
        {
            runtime = compiledModel.getRuntime(((RuntimePointer) execution.runtime).runtime);
        }
        else
        {
            LOGGER.warn("Unsupported runtime type for service {}: {}", service.pattern, execution.runtime.getClass().getSimpleName());
            return null;
        }

        SingleExecutionPlan plan = PlanGenerator.generateExecutionPlan(pureLambda, mapping, runtime,null, compiledModel, PureClientVersions.production, PlanPlatform.JAVA,null, extensions, transformers);

        return plan;
    }

    @Override
    public String getType()
    {
        return SERVICE_TYPE;
    }

    @Override
    public SQLSourceResolvedContext resolve(List<TableSource> sources, SQLContext context, Identity identity)
    {
        List<SQLSource> sqlSources = FastList.newList();
        ListIterate.forEach(sources, source ->
        {
            String PATTERN = "pattern";
            String pattern = (String) source.getArgument(PATTERN, 0).getValue();
            String PROJECT_ID = "projectId";
            Optional<TableSourceArgument> projectId = source.getNamedArgument(PROJECT_ID);

            if (pureModelContextData == null)
            {
                throw new IllegalArgumentException("No Service found for pattern '" + pattern + "'");
            }

            LazyIterable<Service> services = LazyIterate.select(pureModelContextData.getElements(), e -> e instanceof Service)
                    .collect(e -> (Service) e)
                    .select(s -> s.pattern.equals(pattern));

            if (services.isEmpty())
            {
                throw new IllegalArgumentException("No Service found for pattern '" + pattern + "'");
            }

            if (services.size() > 1)
            {
                throw new IllegalArgumentException("Multiple Services found for pattern '" + pattern + "'");
            }

            Service service = services.getOnly();

            List<SQLSourceArgument> keys = FastList.newListWith(new SQLSourceArgument(PATTERN, 0, pattern));
            projectId.ifPresent(tsa -> keys.add(new SQLSourceArgument(PROJECT_ID, 1, tsa.getValue())));

            if (service.execution instanceof PureSingleExecution)
            {
                ExecutionPlan preGeneratedPlan = enablePreGeneratedPlans ? preGeneratedPlans.get(pattern) : null;
                sqlSources.add(from((PureSingleExecution) service.execution, keys, preGeneratedPlan));
            }
        });
        return new SQLSourceResolvedContext(pureModelContextData, sqlSources);
    }

    public PureModelContextData getPureModelContextData()
    {
        return pureModelContextData;
    }

    public boolean isPreGeneratedPlansEnabled()
    {
        return enablePreGeneratedPlans;
    }

    public int getPreGeneratedPlanCount()
    {
        return preGeneratedPlans.size();
    }

    private SQLSource from(PureSingleExecution pse, List<SQLSourceArgument> keys, ExecutionPlan preGeneratedPlan)
    {
        return new SQLSource(SERVICE_TYPE, pse.func, pse.mapping, pse.runtime, pse.executionOptions, null, keys, preGeneratedPlan);
    }
}