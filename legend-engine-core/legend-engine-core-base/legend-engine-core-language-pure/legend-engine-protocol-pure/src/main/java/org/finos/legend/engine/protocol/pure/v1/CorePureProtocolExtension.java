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

package org.finos.legend.engine.protocol.pure.v1;

import com.fasterxml.jackson.databind.util.StdConverter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolConverter;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelEmbeddedTestData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelInstanceTestData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelStoreData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelTestData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.DataQualityExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatExternalizeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatExternalizeTDSExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatInternalizeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.UrlStreamExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.VariableResolutionExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Profile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchemaSet;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapper.RelationalMapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.LegacyRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.engine.protocol.pure.v1.model.test.Test;
import org.finos.legend.engine.protocol.pure.v1.model.test.TestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualTo;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualToJson;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertPass;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.EqualToJsonAssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestDebug;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionPlanDebug;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.protocol.pure.v1.model.type.GenericType;
import org.finos.legend.engine.protocol.pure.v1.model.type.PackageableType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.packageableElement.GenericTypeInstance;

public class CorePureProtocolExtension implements PureProtocolExtension
{
    public static final String MAPPING_CLASSIFIER_PATH = "meta::pure::mapping::Mapping";
    public static final String FUNCTION_CLASSIFIER_PATH = "meta::pure::metamodel::function::ConcreteFunctionDefinition";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Core");
    }

    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.with(() -> Lists.fixedSize.with(
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(SectionIndex.class, "sectionIndex")
                        // Domain
                        .withSubtype(Profile.class, "profile")
                        .withSubtype(Enumeration.class, "Enumeration")
                        .withSubtype(Class.class, "class")
                        .withSubtype(Association.class, "association")
                        .withSubtype(Function.class, "function")
                        .withSubtype(Measure.class, "measure")
                        .withSubtype(ExternalFormatSchemaSet.class, "externalFormatSchemaSet")
                        .withSubtype(Binding.class, "binding")
                        .withSubtype(RelationalMapper.class, "relationalMapper")
                        .build(),
                // Runtime
                ProtocolSubTypeInfo.newBuilder(Runtime.class)
                        .withDefaultSubType(LegacyRuntime.class)
                        .withSubtype(LegacyRuntime.class, "legacyRuntime")
                        .withSubtype(EngineRuntime.class, "engineRuntime")
                        .withSubtype(RuntimePointer.class, "runtimePointer")
                        .build(),
                // Embedded Data
                ProtocolSubTypeInfo.newBuilder(EmbeddedData.class)
                        .withSubtype(ExternalFormatData.class, "externalFormat")
                        .withSubtype(ModelStoreData.class, "modelStore")
                        .withSubtype(DataElementReference.class, "reference")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(ModelTestData.class)
                        .withSubtype(ModelEmbeddedTestData.class, "modelEmbeddedData")
                        .withSubtype(ModelInstanceTestData.class, "modelInstanceData")
                        .build(),
                // Test Assertion
                ProtocolSubTypeInfo.newBuilder(TestAssertion.class)
                        .withSubtype(EqualTo.class, "equalTo")
                        .withSubtype(EqualToJson.class, "equalToJson")
                        .build(),
                // Test Result
                ProtocolSubTypeInfo.newBuilder(TestResult.class)
                        .withSubtype(TestError.class, "testError")
                        .withSubtype(TestExecuted.class, "testExecuted")
                        .build(),
                // Test Debug
                ProtocolSubTypeInfo.newBuilder(TestDebug.class)
                        .withSubtype(TestExecutionPlanDebug.class, "testExecutionPlanDebug")
                        .build(),
                // Assertion Status
                ProtocolSubTypeInfo.newBuilder(AssertionStatus.class)
                        .withSubtype(AssertPass.class, "assertPass")
                        .withSubtype(AssertFail.class, "assertFail")
                        .withSubtype(EqualToJsonAssertFail.class, "equalToJsonAssertFail")
                        .build(),
                // Execution Node
                ProtocolSubTypeInfo.newBuilder(ExecutionNode.class)
                        .withSubtype(DataQualityExecutionNode.class, "dataQuality")
                        .withSubtype(UrlStreamExecutionNode.class, "urlStream")
                        .withSubtype(VariableResolutionExecutionNode.class, "varResolution")
                        .withSubtype(ExternalFormatInternalizeExecutionNode.class, "externalFormatInternalize")
                        .withSubtype(ExternalFormatExternalizeExecutionNode.class, "externalFormatExternalize")
                        .withSubtype(ExternalFormatExternalizeTDSExecutionNode.class, "externalFormatExternalizeTDS")

                        .build(),
                ProtocolSubTypeInfo.newBuilder(TestSuite.class)
                        .withSubtype(MappingTestSuite.class, "mappingTestSuite")
                        .withSubtype(FunctionTestSuite.class, "functionTestSuite")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(Test.class)
                        .withSubtype(MappingTest.class, "mappingTest")
                        .withSubtype(FunctionTest.class, "functionTest")
                        .build()
        ));
    }

    @Override
    public Map<java.lang.Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Maps.mutable.<java.lang.Class<? extends PackageableElement>, String>ofInitialCapacity(12)
                .withKeyValue(Association.class, "meta::pure::metamodel::relationship::Association")
                .withKeyValue(Class.class, "meta::pure::metamodel::type::Class")
                .withKeyValue(Enumeration.class, "meta::pure::metamodel::type::Enumeration")
                .withKeyValue(Mapping.class, MAPPING_CLASSIFIER_PATH)
                .withKeyValue(Function.class, FUNCTION_CLASSIFIER_PATH)
                .withKeyValue(Measure.class, "meta::pure::metamodel::type::Measure")
                .withKeyValue(PackageableConnection.class, "meta::pure::runtime::PackageableConnection")
                .withKeyValue(PackageableRuntime.class, "meta::pure::runtime::PackageableRuntime")
                .withKeyValue(Profile.class, "meta::pure::metamodel::extension::Profile")
                .withKeyValue(DataElement.class, "meta::pure::data::DataElement")
                .withKeyValue(ExternalFormatSchemaSet.class, "meta::external::format::shared::metamodel::SchemaSet")
                .withKeyValue(Binding.class, "meta::external::format::shared::binding::Binding")
                .withKeyValue(SectionIndex.class, "meta::pure::metamodel::section::SectionIndex")
                .withKeyValue(RelationalMapper.class, "meta::relational::metamodel::RelationalMapper");
    }

    @Override
    public List<ProtocolConverter<?>> getConverterDeserializers()
    {
        return Lists.fixedSize.with(
                new ProtocolConverter<>(new ResultVariableWithMissingTypeConverter()),
                new ProtocolConverter<>(
                        Lists.mutable.with(
                                new BasicColumnSpecificationToColFunctionConverter(),
                                new TdsOlapRankToColFunctionConverter()))
        );
    }

    private static class ResultVariableWithMissingTypeConverter extends StdConverter<Variable, Variable>
    {
        @Override
        public Variable convert(Variable variable)
        {
            if (variable.genericType != null && variable.genericType.rawType instanceof PackageableType)
            {
                String _class = ((PackageableType) variable.genericType.rawType).fullPath;
                if (("meta::pure::mapping::Result".equals(_class) || "Result".equals(_class)) && variable.genericType.typeArguments.size() == 0)
                {
                    variable.genericType.typeArguments = Lists.mutable.of(new GenericType(new PackageableType("meta::pure::metamodel::type::Any")));
                    variable.genericType.multiplicityArguments = Lists.mutable.of(Multiplicity.PURE_MANY);
                }
            }
            return variable;
        }
    }

    private static class BasicColumnSpecificationToColFunctionConverter extends StdConverter<AppliedFunction, AppliedFunction>
    {
        @Override
        public AppliedFunction convert(AppliedFunction appliedFunction)
        {
            if (appliedFunction.function.equals("new"))
            {
                GenericTypeInstance typeInstance = (GenericTypeInstance) appliedFunction.parameters.get(0);
                if (typeInstance.genericType.typeArguments.size() >= 1 && typeInstance.genericType.typeArguments.get(0).rawType instanceof PackageableType)
                {
                    PackageableType type = (PackageableType) typeInstance.genericType.typeArguments.get(0).rawType;

                    Set<String> classesThatNeedTypeFixing = Sets.fixedSize.of(
                            "meta::pure::tds::BasicColumnSpecification",
                            "BasicColumnSpecification"
                    );

                    if (classesThatNeedTypeFixing.contains(type.fullPath))
                    {
                        Collection collection = (Collection) appliedFunction.parameters.get(2);
                        Optional<Lambda> func = ListIterate.detectOptional(collection.values, x -> ((CString) ((KeyExpression) x).key).value.equals("func"))
                                .map(KeyExpression.class::cast)
                                .map(x -> x.expression)
                                .filter(Lambda.class::isInstance)
                                .map(Lambda.class::cast);

                        Optional<CString> name = ListIterate.detectOptional(collection.values, x -> ((CString) ((KeyExpression) x).key).value.equals("name"))
                                .map(KeyExpression.class::cast)
                                .map(x -> x.expression)
                                .filter(CString.class::isInstance)
                                .map(CString.class::cast);

                        Optional<CString> doc = ListIterate.detectOptional(collection.values, x -> ((CString) ((KeyExpression) x).key).value.equals("documentation"))
                                .map(KeyExpression.class::cast)
                                .map(x -> x.expression)
                                .filter(CString.class::isInstance)
                                .map(CString.class::cast);

                        if (func.isPresent() && name.isPresent())
                        {
                            appliedFunction.function = "meta::pure::tds::col";
                            appliedFunction.parameters = Lists.mutable.with(func.get(), name.get());

                            if (doc.isPresent())
                            {
                                appliedFunction.fControl = "meta::pure::tds::col_Function_1__String_1__String_1__BasicColumnSpecification_1_";
                                appliedFunction.parameters.add(doc.get());
                            }
                            else
                            {
                                appliedFunction.fControl = "meta::pure::tds::col_Function_1__String_1__BasicColumnSpecification_1_";
                            }
                        }
                    }
                }
            }
            return appliedFunction;
        }
    }

    private static class TdsOlapRankToColFunctionConverter extends StdConverter<AppliedFunction, AppliedFunction>
    {
        @Override
        public AppliedFunction convert(AppliedFunction appliedFunction)
        {
            if (appliedFunction.function.equals("new"))
            {
                GenericTypeInstance typeInstance = (GenericTypeInstance) appliedFunction.parameters.get(0);
                if (typeInstance.genericType.typeArguments.size() >= 1 && typeInstance.genericType.typeArguments.get(0).rawType instanceof PackageableType)
                {
                    PackageableType type = (PackageableType) typeInstance.genericType.typeArguments.get(0).rawType;

                    Set<String> classesThatNeedTypeFixing = Sets.fixedSize.of(
                            "meta::pure::tds::TdsOlapRank",
                            "TdsOlapRank"
                    );

                    if (classesThatNeedTypeFixing.contains(type.fullPath))
                    {
                        Collection collection = (Collection) appliedFunction.parameters.get(2);
                        Optional<Lambda> func = ListIterate.detectOptional(collection.values, x -> ((CString) ((KeyExpression) x).key).value.equals("func"))
                                .map(KeyExpression.class::cast)
                                .map(x -> x.expression)
                                .filter(Lambda.class::isInstance)
                                .map(Lambda.class::cast);

                        if (func.isPresent())
                        {
                            appliedFunction.function = "meta::pure::tds::func";
                            appliedFunction.fControl = "meta::pure::tds::func_FunctionDefinition_1__TdsOlapRank_1_";
                            appliedFunction.parameters = Lists.mutable.with(func.get());
                        }
                    }
                }
            }
            return appliedFunction;
        }
    }
}
