package org.finos.legend.engine.query.graphQL.api.execute;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.relational.result.RealizedRelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.graphQL.metamodel.Directive;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Field;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationDefinition;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.query.graphQL.api.GraphQL;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLCacheKey;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLPlanCache;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_transformation_queryToPure_NamedExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_Runtime;
import org.finos.legend.pure.generated.core_external_query_graphql_transformation_transformation_graphFetch;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GraphQLExecuteExtension extends GraphQL implements IGraphQLExecuteExtension
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GraphQLExecuteExtension.class);
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensionsFunc;
    private final GraphQLPlanCache graphQLPlanCache;
    private final Iterable<? extends PlanTransformer> transformers;
    private final PlanExecutor planExecutor;

    public GraphQLExecuteExtension(GraphQLPlanCache graphQLPlanCache, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensionsFunc, Iterable<? extends PlanTransformer> transformers, PlanExecutor planExecutor)
    {
        super(null, null);
        this.graphQLPlanCache = graphQLPlanCache;
        this.extensionsFunc = extensionsFunc;
        this.transformers = transformers;
        this.planExecutor = planExecutor;
    }

    @Override
    public Map<String, ?> computeExtensionsField(String queryClassPath, String mappingPath, String runtimePath, Document document, OperationDefinition query, PureModel pureModel, GraphQLCacheKey graphQLCacheKey, MutableList<CommonProfile> profiles)
    {
        String fieldName = ((Field)(query.selectionSet.get(0))).name;
        Map<String, Map<String,Object>> m = new HashMap<>();
        List<Directive> directives = ((Field)(query.selectionSet.get(0))).directives.stream().distinct().collect(Collectors.toList());
        if (directives.isEmpty())
        {
            return new HashMap<>();
        }
        m.put(fieldName, new HashMap<>());
        directives.forEach(directive ->
        {
            m.get(fieldName).put(
                directive.name,
                this.processDirective(directive, fieldName, queryClassPath, mappingPath, runtimePath, document, query, pureModel, graphQLCacheKey, profiles)
            );
        });
        return m;
    }

    private Object processDirective(Directive directive, String fieldName, String queryClassPath, String mappingPath, String runtimePath, Document document, OperationDefinition query, PureModel pureModel, GraphQLCacheKey graphQLCacheKey, MutableList<CommonProfile> profiles)
    {
        LOGGER.info("Processing Directive: " + directive);
        switch (directive.name)
        {
            case "echo":
                return true;
            case "totalCount":
                try
                {
                    List<SerializedNamedPlans> plans = null;
                    if (graphQLPlanCache != null)
                    {
                        plans = graphQLPlanCache.getIfPresent(graphQLCacheKey);
                        if (plans == null)
                        {
                            RichIterable<? extends Root_meta_pure_extension_Extension> extensions = this.extensionsFunc.apply(pureModel);
                            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class = pureModel.getClass(queryClassPath);
                            Mapping mapping = pureModel.getMapping(mappingPath);
                            Root_meta_pure_runtime_Runtime runtime = pureModel.getRuntime(runtimePath);
                            org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document queryDoc = GraphQLExecute.toPureModel(document, pureModel);
                            RichIterable<? extends Root_meta_external_query_graphQL_transformation_queryToPure_NamedExecutionPlan> purePlans = core_external_query_graphql_transformation_transformation_graphFetch.Root_meta_external_query_graphQL_transformation_queryToPure_getPlanForTotalCountDirective_Class_1__Mapping_1__Runtime_1__Document_1__Extension_MANY__NamedExecutionPlan_MANY_(_class, mapping, runtime, queryDoc, extensions, pureModel.getExecutionSupport());
                            plans = purePlans.toList().stream().map(p ->
                            {
                                Root_meta_pure_executionPlan_ExecutionPlan nPlan = PlanPlatform.JAVA.bindPlan(p._plan(), "ID", pureModel, extensions);
                                SerializedNamedPlans serializedPlans = new SerializedNamedPlans();
                                serializedPlans.propertyName = p._name();
                                serializedPlans.serializedPlan = PlanGenerator.stringToPlan(PlanGenerator.serializeToJSON(nPlan, PureClientVersions.production, pureModel, extensions, this.transformers));
                                return serializedPlans;
                            }).collect(Collectors.toList());
                            graphQLPlanCache.put(graphQLCacheKey, plans);
                        }
                    }
                    if (plans == null || plans.size() != 1)
                    {
                        throw new RuntimeException("Error computing plans for directive " + directive.name);
                    }
                    Map<String, Result> parameterMap = getParameterMap(query, fieldName);
                    SingleExecutionPlan executionPlan = plans.get(0).serializedPlan;
                    RelationalResult result = (RelationalResult) planExecutor.execute(executionPlan, parameterMap, null, profiles);
                    RealizedRelationalResult realizedResult = ((RealizedRelationalResult) ((result).realizeInMemory()));
                    List<List<Object>> resultSetRows = (realizedResult).resultSetRows;
                    Long totalCount = (Long)((resultSetRows.get(0)).get(0));
                    return totalCount;
                }
                catch (Exception e)
                {
                    LOGGER.error(Arrays.toString(e.getStackTrace()));
                    throw new RuntimeException(e.getMessage());
                }
            default:
                throw new RuntimeException(directive.name + " is not supported");
        }
    }
}
