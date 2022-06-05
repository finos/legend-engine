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

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalSchemaCompileContext;
import org.finos.legend.engine.external.shared.format.model.compile.ExternalFormatSchemaException;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatSchema;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatSchemaSet;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_Schema;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet_Impl;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_Schema_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SchemaSetCompiler
{
    private final MutableMap<String, Root_meta_external_shared_format_metamodel_SchemaSet> schemaSetIndex = Maps.mutable.empty();
    private final Map<String, ExternalFormatExtension> externalFormatExtensions;

    public SchemaSetCompiler(Map<String, ExternalFormatExtension> externalFormatExtensions)
    {
        this.externalFormatExtensions = externalFormatExtensions;
    }

    public Processor<ExternalFormatSchemaSet> getProcessor()
    {
        return Processor.newProcessor(ExternalFormatSchemaSet.class, this::firstPass, this::secondPass);
    }

    public Root_meta_external_shared_format_metamodel_SchemaSet getCompiledSchemaSet(String fullPath)
    {
        return schemaSetIndex.get(fullPath);
    }

    // First pass - create and index schemas
    private PackageableElement firstPass(ExternalFormatSchemaSet srcSchemaSet, CompileContext context)
    {
        if (externalFormatExtensions.get(srcSchemaSet.format) == null)
        {
            throw new EngineException("Unknown schema format: " + srcSchemaSet.format, srcSchemaSet.formatSourceInformation, EngineErrorType.COMPILATION);
        }

        Root_meta_external_shared_format_metamodel_SchemaSet schemaSet = new Root_meta_external_shared_format_metamodel_SchemaSet_Impl(srcSchemaSet.name)
                ._name(srcSchemaSet.name)
                ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")._rawType(context.pureModel.getType("meta::external::shared::format::metamodel::SchemaSet")))
                ._format(srcSchemaSet.format);

        Set<String> ids = Sets.mutable.empty();
        Set<String> locations = Sets.mutable.empty();
        for (ExternalFormatSchema srcSchema : srcSchemaSet.schemas)
        {
            if (srcSchema.id != null)
            {
                if (!ids.add(srcSchema.id))
                {
                    throw new EngineException("Schema id '" + srcSchema.id + "' is duplicated", srcSchema.sourceInformation, EngineErrorType.COMPILATION);
                }
            }
            if (srcSchema.location != null)
            {
                if (!locations.add(srcSchema.location))
                {
                    throw new EngineException("Schema location '" + srcSchema.location + "' is duplicated", srcSchema.sourceInformation, EngineErrorType.COMPILATION);
                }
            }

            Root_meta_external_shared_format_metamodel_Schema schema = new Root_meta_external_shared_format_metamodel_Schema_Impl("")
                    ._id(srcSchema.id)
                    ._location(srcSchema.location);
            schemaSet._schemasAdd(schema);
        }

        String path = context.pureModel.buildPackageString(srcSchemaSet._package, srcSchemaSet.name);
        this.schemaSetIndex.put(path, schemaSet);
        return schemaSet;
    }

    // Second pass - compile external schemas using extension
    private void secondPass(ExternalFormatSchemaSet srcSchemaSet, CompileContext context)
    {
        String path = context.pureModel.buildPackageString(srcSchemaSet._package, srcSchemaSet.name);
        Root_meta_external_shared_format_metamodel_SchemaSet compiled = schemaSetIndex.get(path);

        ExternalFormatExtension schemaExtension = externalFormatExtensions.get(srcSchemaSet.format);
        compiled._schemas(Lists.mutable.empty());
        for (ExternalFormatSchema srcSchema : srcSchemaSet.schemas)
        {
            try
            {
                Root_meta_external_shared_format_metamodel_SchemaDetail detail = schemaExtension.compileSchema(new SchemaCompileContext(srcSchema, srcSchemaSet, context));
                Root_meta_external_shared_format_metamodel_Schema schema = new Root_meta_external_shared_format_metamodel_Schema_Impl("")
                        ._id(srcSchema.id)
                        ._location(srcSchema.location)
                        ._detail(detail);
                compiled._schemasAdd(schema);
            }
            catch (ExternalFormatSchemaException e)
            {
                throw new EngineException(e.getMessage(), srcSchema.contentSourceInformation, EngineErrorType.COMPILATION, e);
            }
        }
    }

    private static class SchemaCompileContext implements ExternalSchemaCompileContext
    {
        private final ExternalFormatSchema srcSchema;
        private final ExternalFormatSchemaSet srcSchemaSet;
        private final CompileContext compileContext;

        SchemaCompileContext(ExternalFormatSchema srcSchema, ExternalFormatSchemaSet srcSchemaSet, CompileContext compileContext)
        {
            this.srcSchema = srcSchema;
            this.srcSchemaSet = srcSchemaSet;
            this.compileContext = compileContext;
        }

        @Override
        public String getContent()
        {
            return srcSchema.content;
        }

        @Override
        public String getContent(String location)
        {
            Objects.requireNonNull(location, "Cannot find schema for null location");
            return srcSchemaSet.schemas.stream()
                    .filter(s -> location.equals(s.location))
                    .findFirst()
                    .map(s -> s.content)
                    .orElseThrow(() -> new IllegalArgumentException("No schema found for location: " + location));
        }

        @Override
        public String getLocation()
        {
            return srcSchema.location;
        }

        @Override
        public PureModel getPureModel()
        {
            return compileContext.pureModel;
        }
    }
}
