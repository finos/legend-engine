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

package org.finos.legend.engine.query.graphQL.api.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.external.shared.format.model.compile.ExternalSchemaCompileContext;
import org.finos.legend.engine.external.shared.format.model.transformation.toModel.ExternalFormatModelGenerationExtension;
import org.finos.legend.engine.external.shared.format.model.transformation.toModel.SchemaToModelConfiguration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.graphQL.introspection.model.Translator;
import org.finos.legend.engine.protocol.graphQL.introspection.model.__Schema;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_binding_toPure_introspection_GraphQLIntrospectionContainer;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_binding_toPure_introspection_GraphQLIntrospectionContainer_Impl;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_ExternalFormatContract;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_toPure_SchemaToModelConfiguration;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_toPure_SchemaToModelConfiguration_Impl;
import org.finos.legend.pure.generated.core_external_query_graphql_contract;

public class GraphQLFormatExtension implements ExternalFormatModelGenerationExtension<Root_meta_external_query_graphQL_binding_toPure_introspection_GraphQLIntrospectionContainer, GraphQLSchemaToModelConfiguration>
{
    private final Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_query_graphQL_binding_toPure_introspection_GraphQLIntrospectionContainer> graphQLIntrospectionContract = (Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_query_graphQL_binding_toPure_introspection_GraphQLIntrospectionContainer>) core_external_query_graphql_contract.Root_meta_external_query_graphQL_contract_graphQLIntrospectionContract__ExternalFormatContract_1_(PureModel.CORE_PURE_MODEL.getExecutionSupport());

    @Override
    public Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_query_graphQL_binding_toPure_introspection_GraphQLIntrospectionContainer> getExternalFormatContract()
    {
        return graphQLIntrospectionContract;
    }

    @Override
    public Root_meta_external_query_graphQL_binding_toPure_introspection_GraphQLIntrospectionContainer compileSchema(ExternalSchemaCompileContext context)
    {
        try
        {
            return new Root_meta_external_query_graphQL_binding_toPure_introspection_GraphQLIntrospectionContainer_Impl("", null, context.getPureModel().getClass("meta::external::query::graphQL::binding::toPure::introspection::GraphQLIntrospectionContainer"))
                    ._schema(
                            new Translator().translate(
                                    new ObjectMapper().readValue(context.getContent(), __Schema.class),
                                    context.getPureModel()
                            )
                    );
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String metamodelToText(Root_meta_external_query_graphQL_binding_toPure_introspection_GraphQLIntrospectionContainer schemaDetail, PureModel pureModel)
    {
        return null;
    }

    @Override
    public Root_meta_external_shared_format_transformation_toPure_SchemaToModelConfiguration compileSchemaToModelConfiguration(GraphQLSchemaToModelConfiguration configuration, PureModel pureModel)
    {
        return new Root_meta_external_shared_format_transformation_toPure_SchemaToModelConfiguration_Impl("", null, pureModel.getClass("meta::external::shared::format::transformation::toPure::SchemaToModelConfiguration"))
                ._sourceSchemaId(configuration.sourceSchemaId)
                ._targetPackage(configuration.targetPackage);
    }
}
