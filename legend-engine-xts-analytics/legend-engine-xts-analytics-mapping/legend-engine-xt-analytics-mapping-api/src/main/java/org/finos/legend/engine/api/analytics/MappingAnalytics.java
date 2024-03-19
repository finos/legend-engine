// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.api.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.api.analytics.model.MappingModelCoverageAnalysisInput;
import org.finos.legend.engine.api.analytics.model.MappingRuntimeCompatibilityAnalysisInput;
import org.finos.legend.engine.api.analytics.model.MappingRuntimeCompatibilityAnalysisResult;
import org.finos.legend.engine.language.pure.compiler.fromPureGraph.PureModelContextDataGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.to.HelperValueSpecificationGrammarComposer;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.analytics.model.MappingModelCoverageAnalysisResult;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.http.InflateInterceptor;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_analytics_binding_modelCoverage_BindingModelCoverageAnalysisResult;
import org.finos.legend.pure.generated.Root_meta_analytics_function_modelCoverage_FunctionModelCoverageAnalysisResult;
import org.finos.legend.pure.generated.Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult;
import org.finos.legend.pure.generated.Root_meta_external_format_shared_binding_Binding;
import org.finos.legend.pure.generated.core_analytics_binding_modelCoverage_analytics;
import org.finos.legend.pure.generated.core_analytics_function_modelCoverage_analytics;
import org.finos.legend.pure.generated.core_analytics_mapping_modelCoverage_analytics;
import org.finos.legend.pure.generated.core_analytics_mapping_modelCoverage_serializer;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Api(tags = "Analytics - Model")
@Path("pure/v1/analytics/mapping")
public class MappingAnalytics
{
    private final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private final ModelManager modelManager;

    public MappingAnalytics(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("modelCoverage")
    @ApiOperation(value = "Analyze the mapping to generate information about mapped classes and mapped properties of each class")
    @Consumes({MediaType.APPLICATION_JSON, InflateInterceptor.APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response analyzeModelCoverage(MappingModelCoverageAnalysisInput input,
                                         @QueryParam("returnMappedEntityInfo") @DefaultValue("false") boolean returnMappedEntityInfo,
                                         @QueryParam("returnMappedPropertyInfo") @DefaultValue("false") boolean returnMappedPropertyInfo,
                                         @QueryParam("returnLightGraph") @DefaultValue("false") boolean returnLightGraph,
                                         @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(profiles);
        PureModel pureModel = this.modelManager.loadModel(input.model, input.clientVersion, identity, null);
        PureModelContextData pureModelContextData = this.modelManager.loadData(input.model, input.clientVersion, identity);
        Mapping mapping = input.mapping == null ? null : pureModel.getMapping(input.mapping);
        try (Scope scope = GlobalTracer.get().buildSpan("Mapping: analysis").startActive(true))
        {
            try
            {
                Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult analysisResult = core_analytics_mapping_modelCoverage_analytics.Root_meta_analytics_mapping_modelCoverage_analyze_Mapping_1__Boolean_1__Boolean_1__Boolean_1__MappingModelCoverageAnalysisResult_1_(mapping, returnMappedEntityInfo, returnMappedPropertyInfo, returnLightGraph, pureModel.getExecutionSupport());
                MappingModelCoverageAnalysisResult result = this.objectMapper.readValue(core_analytics_mapping_modelCoverage_serializer.Root_meta_analytics_mapping_modelCoverage_serialization_json_getSerializedMappingModelCoverageAnalysisResult_MappingModelCoverageAnalysisResult_1__String_1_(analysisResult, pureModel.getExecutionSupport()), MappingModelCoverageAnalysisResult.class);
                PureModelContextData.Builder builder = PureModelContextData.newBuilder();
               if (returnLightGraph)
               {
                   // Here we prune the bindings to have just packageableIncludes part of ModelUnit
                   // because we only need that as a part of analytics.
                   List<String> bindingPaths = pureModelContextData.getElements().stream().filter(el -> el instanceof Binding).map(b ->
                   {
                       Binding _binding = new Binding();
                       _binding.name = b.name;
                       _binding.contentType = ((Binding) b).contentType;
                       _binding._package = b._package;
                       _binding.modelUnit = ((Binding) b).modelUnit;
                       _binding.modelUnit.packageableElementExcludes = org.eclipse.collections.api.factory.Lists.mutable.empty();
                       builder.addElement(_binding);
                       return b.getPath();
                   }).collect(Collectors.toList());
                   RichIterable<? extends Root_meta_external_format_shared_binding_Binding> bindings = org.eclipse.collections.api.factory.Lists.mutable.ofAll(bindingPaths.stream().map(path ->
                   {
                       Root_meta_external_format_shared_binding_Binding binding;
                       try
                       {
                           binding = (Root_meta_external_format_shared_binding_Binding) pureModel.getPackageableElement(path);
                           return binding;
                       }
                       catch (Exception ignored)
                       {

                       }
                       return null;
                   }).filter(c -> c != null).collect(Collectors.toList()));
                   Root_meta_analytics_binding_modelCoverage_BindingModelCoverageAnalysisResult bindingAnalysisResult = core_analytics_binding_modelCoverage_analytics.Root_meta_analytics_binding_modelCoverage_getBindingModelCoverage_Binding_MANY__BindingModelCoverageAnalysisResult_1_(bindings, pureModel.getExecutionSupport());
                   List<String> functionPaths = pureModelContextData.getElements().stream().filter(el -> el instanceof Function).map(e -> e.getPath()).collect(Collectors.toList());
                   List<String> allExtraElements = functionPaths;
                   pureModelContextData.getElements().stream().filter(el -> allExtraElements.contains(el.getPath())).forEach(builder::addElement);
                   List<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> elements = builder.build().getElements();
                   RichIterable<? extends ConcreteFunctionDefinition<? extends  Object>> functions = org.eclipse.collections.api.factory.Lists.mutable.ofAll(functionPaths.stream().map(path ->
                   {
                       ConcreteFunctionDefinition<? extends Object> function = null;
                       try
                       {
                           function = pureModel.getConcreteFunctionDefinition_safe(path);
                           if (function == null)
                           {
                               Function _function = (Function) elements.stream().filter(e -> e.getPath().equals(path)).findFirst().get();
                               function = pureModel.getConcreteFunctionDefinition_safe(path + HelperValueSpecificationGrammarComposer.getFunctionSignature(_function));

                           }
                           return function;
                       }
                       catch (Exception ignored)
                       {

                       }
                       return null;
                   }).filter(c -> c != null).collect(Collectors.toList()));
                   Root_meta_analytics_function_modelCoverage_FunctionModelCoverageAnalysisResult functionCoverageAnalysisResult = core_analytics_function_modelCoverage_analytics.Root_meta_analytics_function_modelCoverage_getFunctionModelCoverage_ConcreteFunctionDefinition_MANY__FunctionModelCoverageAnalysisResult_1_(org.eclipse.collections.impl.factory.Lists.mutable.ofAll(functions), pureModel.getExecutionSupport());
                   MutableList<? extends Class<? extends Object>> coveredClasses = analysisResult._classes().toList();
                   List<String> coveredClassesPaths = coveredClasses.stream().map(c -> HelperModelBuilder.getElementFullPath(c, pureModel.getExecutionSupport())).collect(Collectors.toList());
                   coveredClasses = org.eclipse.collections.impl.factory.Lists.mutable.ofAll(Stream.concat(Stream.concat(functionCoverageAnalysisResult._classes().toList().stream().filter(c -> !coveredClassesPaths.contains(HelperModelBuilder.getElementFullPath(c, pureModel.getExecutionSupport()))),
                           bindingAnalysisResult._classes().toList().stream().filter(c -> !coveredClassesPaths.contains(HelperModelBuilder.getElementFullPath(c, pureModel.getExecutionSupport())))).distinct(),
                           analysisResult._classes().toList().stream()).collect(Collectors.toList()));
                   MutableList<Enumeration<? extends Enum>> coveredEnumerations = org.eclipse.collections.impl.factory.Lists.mutable.ofAll(Stream.concat(analysisResult._enumerations().toList().stream(), functionCoverageAnalysisResult._enumerations().toList().stream()).distinct().collect(Collectors.toList()));
                   PureModelContextData classes = PureModelContextDataGenerator.generatePureModelContextDataFromClasses(coveredClasses, input.clientVersion, pureModel.getExecutionSupport());
                   PureModelContextData enums = PureModelContextDataGenerator.generatePureModelContextDataFromEnumerations(coveredEnumerations, input.clientVersion, pureModel.getExecutionSupport());
                   PureModelContextData _profiles = PureModelContextDataGenerator.generatePureModelContextDataFromProfile((RichIterable<Profile>) analysisResult._profiles(), input.clientVersion, pureModel.getExecutionSupport());
                   PureModelContextData associations = PureModelContextDataGenerator.generatePureModelContextDataFromAssociations(analysisResult._associations(), input.clientVersion, pureModel.getExecutionSupport());

                   result.model = builder.build().combine(classes).combine(enums).combine(_profiles).combine(associations);
               }
                return ManageConstantResult.manageResult(identity.getName(), result);
            }
            catch (Exception e)
            {
                return ExceptionTool.exceptionManager(e, LoggingEventType.ANALYTICS_ERROR, Response.Status.BAD_REQUEST, identity.getName());
            }
        }
    }

    @POST
    @Path("runtimeCompatibility")
    @ApiOperation(value = "Analyze the mapping to identify compatible runtimes")
    @Consumes({MediaType.APPLICATION_JSON, InflateInterceptor.APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response analyzeMappingRuntimeCompatibility(MappingRuntimeCompatibilityAnalysisInput input,
                                                       @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(profiles);
        PureModelContextData pureModelContextData = this.modelManager.loadData(input.model, input.clientVersion, identity);
        PureModel pureModel = this.modelManager.loadModel(pureModelContextData, input.clientVersion, identity, null);
        Mapping mapping = input.mapping == null ? null : pureModel.getMapping(input.mapping);
        try (Scope scope = GlobalTracer.get().buildSpan("Mapping: analysis").startActive(true))
        {
            try
            {
                return ManageConstantResult.manageResult(identity.getName(), new MappingRuntimeCompatibilityAnalysisResult(
                        ListIterate.collect(HelperRuntimeBuilder.getMappingCompatibleRuntimes(
                                mapping,
                                ListIterate.selectInstancesOf(pureModelContextData.getElements(), PackageableRuntime.class),
                                pureModel), runtime -> HelperModelBuilder.getElementFullPath(runtime, pureModel.getExecutionSupport()))));
            }
            catch (Exception e)
            {
                return ExceptionTool.exceptionManager(e, LoggingEventType.ANALYTICS_ERROR, Response.Status.BAD_REQUEST, identity.getName());
            }
        }
    }
}
