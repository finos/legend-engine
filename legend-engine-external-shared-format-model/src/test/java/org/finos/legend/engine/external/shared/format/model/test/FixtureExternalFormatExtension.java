// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.shared.format.model.test;

import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalSchemaCompileContext;
import org.finos.legend.engine.external.shared.format.model.fromModel.ModelToSchemaConfiguration;
import org.finos.legend.engine.external.shared.format.model.toModel.SchemaToModelConfiguration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatSchemaSet;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_validation_BindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_validation_BindingDetail_Impl;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_Schema;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaDetail_Impl;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FixtureExternalFormatExtension implements ExternalFormatExtension<Root_meta_external_shared_format_metamodel_SchemaDetail, SchemaToModelConfiguration, ModelToSchemaConfiguration>
{
    @Override
    public String getFormat()
    {
        return "Example";
    }

    @Override
    public List<String> getContentTypes()
    {
        return Collections.singletonList("text/example");
    }

    @Override
    public Root_meta_external_shared_format_metamodel_SchemaDetail compileSchema(ExternalSchemaCompileContext context)
    {
        return new Root_meta_external_shared_format_metamodel_SchemaDetail_Impl("");
    }

    @Override
    public Root_meta_external_shared_format_binding_validation_BindingDetail bindDetails(Root_meta_external_shared_format_binding_Binding binding, CompileContext context)
    {
        return new Root_meta_external_shared_format_binding_validation_BindingDetail_Impl("");
    }

    @Override
    public String metamodelToText(Root_meta_external_shared_format_metamodel_SchemaDetail schemaDetail)
    {
        return "";
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateModel(Root_meta_external_shared_format_metamodel_SchemaSet schemaSet, SchemaToModelConfiguration configuration, PureModel pureModel)
    {
        return null;
    }

    @Override
    public String getFileExtension()
    {
        return "txt";
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateSchema(ModelToSchemaConfiguration configuration, PureModel pureModel)
    {
        return null;
    }

    @Override
    public List<String> getRegisterablePackageableElementNames()
    {
        return Collections.emptyList();
    }
}
