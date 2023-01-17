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

import org.finos.legend.engine.external.shared.format.model.compile.ExternalSchemaCompileContext;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ExternalFormatSchemaGenerationExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_binding_fromPure_sdl_ModelToGraphQLConfig_Impl;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_GraphQLSDLContainer;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_ExternalFormatContract;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_fromPure_ModelToSchemaConfiguration;
import org.finos.legend.pure.generated.core_external_query_graphql_binding_fromPure_sdl_fromPure_sdl;
import org.finos.legend.pure.generated.core_external_query_graphql_contract;

public class GraphQLSDLFormatExtension implements ExternalFormatSchemaGenerationExtension<Root_meta_external_query_graphQL_metamodel_sdl_GraphQLSDLContainer, ModelToGraphQLSchemaConfiguration>
{

    private static final Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_query_graphQL_metamodel_sdl_GraphQLSDLContainer> contract =
        (Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_query_graphQL_metamodel_sdl_GraphQLSDLContainer>) core_external_query_graphql_contract.Root_meta_external_query_graphQL_contract_graphQLSdlContract__ExternalFormatContract_1_(
            PureModel.CORE_PURE_MODEL.getExecutionSupport());
    public static final String TYPE = contract._id();

    @Override
    public Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_query_graphQL_metamodel_sdl_GraphQLSDLContainer> getExternalFormatContract()
    {
        return contract;
    }

    @Override
    public Root_meta_external_query_graphQL_metamodel_sdl_GraphQLSDLContainer compileSchema(ExternalSchemaCompileContext context)
    {
        return null;
    }

    @Override
    public String metamodelToText(Root_meta_external_query_graphQL_metamodel_sdl_GraphQLSDLContainer schemaDetail, PureModel pureModel)
    {
        return core_external_query_graphql_binding_fromPure_sdl_fromPure_sdl.Root_meta_external_query_graphQL_binding_fromPure_sdl_schemaDetailToString_GraphQLSDLContainer_1__String_1_(schemaDetail, pureModel.getExecutionSupport());
    }

    @Override
    public Root_meta_external_shared_format_transformation_fromPure_ModelToSchemaConfiguration compileModelToSchemaConfiguration(ModelToGraphQLSchemaConfiguration configuration, PureModel pureModel)
    {
        return new Root_meta_external_query_graphQL_binding_fromPure_sdl_ModelToGraphQLConfig_Impl("", null,
            pureModel.getClass("meta::external::query::graphQL::binding::fromPure::sdl::ModelToGraphQLConfig"))._targetSchemaSet(configuration.targetSchemaSet);
    }
}
