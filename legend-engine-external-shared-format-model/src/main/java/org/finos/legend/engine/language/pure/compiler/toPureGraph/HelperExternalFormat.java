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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;

import java.util.Objects;

public class HelperExternalFormat
{
    public static Root_meta_external_shared_format_metamodel_SchemaSet getSchemaSet(String fullPath, CompileContext context)
    {
        return getSchemaSet(fullPath, SourceInformation.getUnknownSourceInformation(), context);
    }

    public static Root_meta_external_shared_format_metamodel_SchemaSet getSchemaSet(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        Root_meta_external_shared_format_metamodel_SchemaSet schemaSet = getExternalFormatCompilerExtension(context).schemaSetCompiler.getCompiledSchemaSet(fullPath);
        Assert.assertTrue(schemaSet != null, () -> "Can't find SchemaSet '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return schemaSet;
    }

    public static Root_meta_external_shared_format_binding_Binding getBinding(String fullPath, CompileContext context)
    {
        return getBinding(fullPath, SourceInformation.getUnknownSourceInformation(), context);
    }

    public static Root_meta_external_shared_format_binding_Binding getBinding(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        Root_meta_external_shared_format_binding_Binding binding = getExternalFormatCompilerExtension(context).bindingCompiler.getCompiledBinding(fullPath);
        Assert.assertTrue(binding != null, () -> "Can't find (external format) Binding '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return binding;
    }

    private static ExternalFormatCompilerExtension getExternalFormatCompilerExtension(CompileContext context)
    {
        return Objects.requireNonNull(ListIterate.selectInstancesOf(context.getCompilerExtensions().getExtensions(), ExternalFormatCompilerExtension.class).getAny(), "Serializable model specification extension is not in scope");
    }

    public static ExternalFormatExtension<?, ?, ?> getExternalFormatExtension(Root_meta_external_shared_format_binding_Binding binding)
    {
        return ExternalFormatExtensionLoader.extensions().values().stream()
                .filter(ext -> ext.getContentTypes().contains(binding._contentType()))
                .findFirst()
                .orElseThrow(() -> new EngineException("Unknown contentType '" + binding._contentType() + "'", SourceInformation.getUnknownSourceInformation(), EngineErrorType.COMPILATION));  // Should never reach here as binding should be compiled before
    }
}
