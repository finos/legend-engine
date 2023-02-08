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

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelStoreData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.DataQualityExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatExternalizeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatInternalizeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.UrlStreamExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.VariableResolutionExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Profile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Unit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchemaSet;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalSource;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.UrlStreamExternalSource;
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
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;

import java.util.List;
import java.util.Map;

public class CorePureProtocolExtension implements PureProtocolExtension
{
    public static final String MAPPING_CLASSIFIER_PATH = "meta::pure::mapping::Mapping";

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
                        .withSubtype(Unit.class, "unit")
                        .withSubtype(ExternalFormatSchemaSet.class, "externalFormatSchemaSet")
                        .withSubtype(Binding.class, "binding")
                        .build(),
                // Runtime
                ProtocolSubTypeInfo.newBuilder(Runtime.class)
                        .withDefaultSubType(LegacyRuntime.class)
                        .withSubtype(LegacyRuntime.class, "legacyRuntime")
                        .withSubtype(EngineRuntime.class, "engineRuntime")
                        .withSubtype(RuntimePointer.class, "runtimePointer")
                        .build(),
                // Connection
                ProtocolSubTypeInfo.newBuilder(Connection.class)
                        .withSubtype(ExternalFormatConnection.class, "ExternalFormatConnection")
                        .build(),
                // Embedded Data
                ProtocolSubTypeInfo.newBuilder(EmbeddedData.class)
                        .withSubtype(ExternalFormatData.class, "externalFormat")
                        .withSubtype(ModelStoreData.class, "modelStore")
                        .withSubtype(DataElementReference.class, "reference")
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
                // Assertion Status
                ProtocolSubTypeInfo.newBuilder(AssertionStatus.class)
                        .withSubtype(AssertPass.class, "assertPass")
                        .withSubtype(AssertFail.class, "assertFail")
                        .withSubtype(EqualToJsonAssertFail.class, "equalToJsonAssertFail")
                        .build(),
                // External Source
                ProtocolSubTypeInfo.newBuilder(ExternalSource.class)
                        .withSubtype(UrlStreamExternalSource.class, "urlStream")
                        .build(),
                // Execution Node
                ProtocolSubTypeInfo.newBuilder(ExecutionNode.class)
                        .withSubtype(DataQualityExecutionNode.class, "dataQuality")
                        .withSubtype(UrlStreamExecutionNode.class, "urlStream")
                        .withSubtype(VariableResolutionExecutionNode.class, "varResolution")
                        .withSubtype(ExternalFormatInternalizeExecutionNode.class, "externalFormatInternalize")
                        .withSubtype(ExternalFormatExternalizeExecutionNode.class, "externalFormatExternalize")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(TestSuite.class)
                        .withSubtype(MappingTestSuite.class, "mappingTestSuite")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(Test.class)
                        .withSubtype(MappingTest.class, "mappingTest")
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
                .withKeyValue(Function.class, "meta::pure::metamodel::function::ConcreteFunctionDefinition")
                .withKeyValue(Measure.class, "meta::pure::metamodel::type::Measure")
                .withKeyValue(PackageableConnection.class, "meta::pure::runtime::PackageableConnection")
                .withKeyValue(PackageableRuntime.class, "meta::pure::runtime::PackageableRuntime")
                .withKeyValue(Profile.class, "meta::pure::metamodel::extension::Profile")
                .withKeyValue(SectionIndex.class, "meta::pure::metamodel::section::SectionIndex")
                .withKeyValue(Unit.class, "meta::pure::metamodel::type::Unit")
                .withKeyValue(DataElement.class, "meta::pure::data::DataElement");
    }
}
