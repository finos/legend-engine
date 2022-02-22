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
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalSchemaCompileContext;
import org.finos.legend.engine.external.shared.format.model.fromModel.ModelToSchemaConfiguration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.graphQL.introspection.model.Translator;
import org.finos.legend.engine.protocol.graphQL.introspection.model.__Schema;
import org.finos.legend.pure.generated.*;

import java.util.Collections;
import java.util.List;

public class GraphQLFormatExtension implements ExternalFormatExtension<Root_meta_external_query_graphQL_binding_toPure_introspection_GraphQLIntrospectionContainer, GraphQLSchemaToModelConfiguration, ModelToSchemaConfiguration>
{
    private static final String TYPE = "GraphQL_Introspection";

    @Override
    public String getFormat()
    {
        return TYPE;
    }

    @Override
    public List<String> getContentTypes()
    {
        return Collections.emptyList();
    }

    @Override
    public Root_meta_external_query_graphQL_binding_toPure_introspection_GraphQLIntrospectionContainer compileSchema(ExternalSchemaCompileContext context)
    {
        try
        {
            return new Root_meta_external_query_graphQL_binding_toPure_introspection_GraphQLIntrospectionContainer_Impl("")
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
    public Root_meta_external_shared_format_binding_validation_BindingDetail bindDetails(Root_meta_external_shared_format_binding_Binding binding, CompileContext context)
    {
        return null;
    }

    @Override
    public boolean supportsModelGeneration()
    {
        return true;
    }

    @Override
    public RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationParameter> getModelGenerationProperties(PureModel pureModel)
    {
        return Lists.mutable.empty();
    }

    @Override
    public String getFileExtension()
    {
        return "json";
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateModel(Root_meta_external_shared_format_metamodel_SchemaSet schemaSet, GraphQLSchemaToModelConfiguration config, PureModel pureModel)
    {
        Root_meta_external_shared_format_binding_toPure_SchemaToModelConfiguration configuration = new Root_meta_external_shared_format_binding_toPure_SchemaToModelConfiguration_Impl("")
                ._sourceSchemaId(config.sourceSchemaId)
                ._targetPackage(config.targetPackage == null ? "target::package": config.targetPackage)
                ._targetBinding(config.targetBinding == null ? "target::package::GeneratedBinding" : config.targetBinding);
        return core_external_query_graphql_introspection_transformation.Root_meta_external_query_graphQL_binding_toPure_introspection_IntrospectionToPure_SchemaSet_1__SchemaToModelConfiguration_1__Binding_1_(schemaSet, configuration, pureModel.getExecutionSupport());
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateSchema(ModelToSchemaConfiguration modelToXsdConfiguration, PureModel pureModel)
    {
        return null;
    }

    @Override
    public String metamodelToText(Root_meta_external_query_graphQL_binding_toPure_introspection_GraphQLIntrospectionContainer schemaDetail)
    {
        return null;
    }

    @Override
    public List<String> getRegisterablePackageableElementNames()
    {
        return Lists.mutable.empty();
    }
}
