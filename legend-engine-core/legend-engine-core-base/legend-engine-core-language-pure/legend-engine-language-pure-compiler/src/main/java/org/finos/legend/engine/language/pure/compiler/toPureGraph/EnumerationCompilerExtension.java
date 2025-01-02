// Copyright 2024 Goldman Sachs
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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.domain.Enumeration;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relationship_Generalization_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enum_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;

public class EnumerationCompilerExtension implements CompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Enumeration");
    }

    @Override
    public CompilerExtension build()
    {
        return new EnumerationCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        Enumeration.class,
                        this::enumerationFirstPass,
                        this::enumerationSecondPass
                )
        );
    }

    private PackageableElement enumerationFirstPass(Enumeration enumeration, CompileContext context)
    {
        String fullPath = context.pureModel.buildPackageString(enumeration._package, enumeration.name);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<Object> en = new Root_meta_pure_metamodel_type_Enumeration_Impl<>(enumeration.name, SourceInformationHelper.toM3SourceInformation(enumeration.sourceInformation), context.pureModel.getClass("meta::pure::metamodel::type::Enumeration"));
        context.pureModel.typesIndex.put(fullPath, en);
        GenericType genericType = context.newGenericType(en);
        context.pureModel.typesGenericTypeIndex.put(fullPath, genericType);
        return en._classifierGenericType(context.newGenericType(context.pureModel.getClass("meta::pure::metamodel::type::Enumeration"), genericType))
                ._stereotypes(ListIterate.collect(enumeration.stereotypes, context::resolveStereotype))
                ._taggedValues(ListIterate.collect(enumeration.taggedValues, context::newTaggedValue))
                ._values(ListIterate.collect(enumeration.values, v -> new Root_meta_pure_metamodel_type_Enum_Impl(v.value, SourceInformationHelper.toM3SourceInformation(v.sourceInformation), null)
                        ._classifierGenericType(genericType)
                        ._stereotypes(ListIterate.collect(v.stereotypes, context::resolveStereotype))
                        ._taggedValues(ListIterate.collect(v.taggedValues, context::newTaggedValue))
                        ._name(v.value)));
    }

    private void enumerationSecondPass(Enumeration enumeration, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<?> targetEnum = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<?>) context.pureModel.getType(context.pureModel.buildPackageString(enumeration._package, enumeration.name), enumeration.sourceInformation);
        if (targetEnum._generalizations().isEmpty())
        {
            Generalization g = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::relationship::Generalization"))
                    ._general(context.pureModel.getGenericType("meta::pure::metamodel::type::Any"))
                    ._specific(targetEnum);
            targetEnum._generalizationsAdd(g);
        }
    }
}
