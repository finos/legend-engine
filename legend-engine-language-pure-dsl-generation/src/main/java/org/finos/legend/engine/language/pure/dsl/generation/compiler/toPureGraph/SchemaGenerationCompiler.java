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


package org.finos.legend.engine.language.pure.dsl.generation.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generation.SchemaGeneration;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_SchemaGeneration;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_SchemaGeneration_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;

public class SchemaGenerationCompiler
{

    private final MutableMap<String, Root_meta_pure_generation_metamodel_SchemaGeneration> schemaGenerationIndex = Maps.mutable.empty();



    public static void processSchemaGeneration(SchemaGeneration protocol,Root_meta_pure_generation_metamodel_SchemaGeneration metamodel, CompileContext context)
    {






    }



    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement firstPass(SchemaGeneration schemaGeneration, CompileContext context)
    {
        Root_meta_pure_generation_metamodel_SchemaGeneration metamodel = new Root_meta_pure_generation_metamodel_SchemaGeneration_Impl(schemaGeneration.name)
            ._name(schemaGeneration.name)
            ._classifierGenericType(
                new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                    ._rawType(context.pureModel.getType("meta::pure::generation::metamodel::SchemaGeneration")));

        String path = context.pureModel.buildPackageString(schemaGeneration._package, schemaGeneration.name);
        this.schemaGenerationIndex.put(path, metamodel);
        return metamodel;
    }

    public void secondPass(SchemaGeneration schemaGeneration, CompileContext context)
    {
        String path = context.pureModel.buildPackageString(schemaGeneration._package, schemaGeneration.name);
        this.schemaGenerationIndex.get(path);



    }



}
