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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.fromPureGraph.PureModelContextDataGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.to.HelperValueSpecificationGrammarComposer;
import org.finos.legend.engine.protocol.analytics.model.MappingModelCoverageAnalysisResult;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MappingModelCoverageAnalysis
{
    public static MappingModelCoverageAnalysisResult analyze(Mapping mapping,
                                                             PureModel pureModel,
                                                             PureModelContextData pureModelContextData,
                                                             String clientVersion, ObjectMapper objectMapper,
                                                             boolean returnMappedEntityInfo,
                                                             boolean returnMappedPropertyInfo,
                                                             boolean returnLightGraph) throws JsonProcessingException
    {
        Root_meta_analytics_mapping_modelCoverage_MappingModelCoverageAnalysisResult analysisResult =
                core_analytics_mapping_modelCoverage_analytics.Root_meta_analytics_mapping_modelCoverage_analyze_Mapping_1__Boolean_1__Boolean_1__Boolean_1__MappingModelCoverageAnalysisResult_1_(
                mapping,
                returnMappedEntityInfo,
                returnMappedPropertyInfo,
                returnLightGraph,
                pureModel.getExecutionSupport()
        );
        MappingModelCoverageAnalysisResult result = objectMapper.readValue(
                core_analytics_mapping_modelCoverage_serializer.Root_meta_analytics_mapping_modelCoverage_serialization_json_getSerializedMappingModelCoverageAnalysisResult_MappingModelCoverageAnalysisResult_1__String_1_(
                        analysisResult,
                        pureModel.getExecutionSupport()
                ),
                MappingModelCoverageAnalysisResult.class
        );

        if (returnLightGraph)
        {
            PureModelContextData.Builder builder = PureModelContextData.newBuilder();

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
            PureModelContextData classes = PureModelContextDataGenerator.generatePureModelContextDataFromClasses(coveredClasses, clientVersion, pureModel.getExecutionSupport());
            PureModelContextData enums = PureModelContextDataGenerator.generatePureModelContextDataFromEnumerations(coveredEnumerations, clientVersion, pureModel.getExecutionSupport());
            PureModelContextData _profiles = PureModelContextDataGenerator.generatePureModelContextDataFromProfile((RichIterable<Profile>) analysisResult._profiles(), clientVersion, pureModel.getExecutionSupport());
            PureModelContextData associations = PureModelContextDataGenerator.generatePureModelContextDataFromAssociations(analysisResult._associations(), clientVersion, pureModel.getExecutionSupport());

            result.model = builder.build().combine(classes).combine(enums).combine(_profiles).combine(associations);
        }
        return result;
    }
}
