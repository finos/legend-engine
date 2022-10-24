//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.code.core;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.graphQL.introspection.model.__Schema;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_introspection___Schema;
import org.finos.legend.pure.generated.core_external_query_graphql_binding_toPure_introspection_toPure_introspection;
import org.finos.legend.pure.generated.core_pure_serialization_toPureGrammar;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;

public class GeneratePureModelFromIntrospectionInstance
{
    public static void main(String[] args) throws Exception
    {
        PureModel pureModel = new PureModel(PureModelContextData.newBuilder().build(), Lists.mutable.empty(), DeploymentMode.TEST);
        __Schema schema = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(GeneratePureModelFromIntrospectionInstance.class.getClassLoader().getResourceAsStream("exampleModel.json"), __Schema.class);
        Root_meta_external_query_graphQL_metamodel_introspection___Schema pureSchema = new org.finos.legend.engine.protocol.graphQL.introspection.model.Translator().translate(schema, pureModel);
        RichIterable<? extends Type> types = core_external_query_graphql_binding_toPure_introspection_toPure_introspection.Root_meta_external_query_graphQL_binding_toPure_introspection_buildPureTypesFromGraphQLSchema___Schema_1__String_1__Type_MANY_(pureSchema, "pack", pureModel.getExecutionSupport());
        String res = types.select(t -> t instanceof Class).collect(t -> core_pure_serialization_toPureGrammar.Root_meta_pure_metamodel_serialization_grammar_printType_Type_1__String_1_(t, pureModel.getExecutionSupport())).makeString("\n");
        System.out.println(res);
    }
}
