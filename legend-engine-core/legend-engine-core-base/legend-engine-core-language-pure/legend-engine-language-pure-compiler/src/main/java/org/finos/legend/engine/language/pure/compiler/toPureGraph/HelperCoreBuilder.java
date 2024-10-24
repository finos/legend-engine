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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TagPtr;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_extension_TaggedValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;

public class HelperCoreBuilder
{
    protected static GenericType newGenericType(Type rawType, CompileContext context)
    {
        return new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                ._rawType(rawType);
    }

    protected static GenericType newGenericType(Type rawType, GenericType typeArgument, CompileContext context)
    {
        return newGenericType(rawType, Lists.fixedSize.with(typeArgument), context);
    }

    private static GenericType newGenericType(Type rawType, RichIterable<? extends GenericType> typeArguments, CompileContext context)
    {
        return newGenericType(rawType, context)._typeArguments(typeArguments);
    }

    protected static TaggedValue newTaggedValue(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue taggedValue, CompileContext context)
    {
        return new Root_meta_pure_metamodel_extension_TaggedValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::extension::TaggedValue"))
                ._tag(resolveTag(taggedValue.tag, context))
                ._value(taggedValue.value);
    }

    private static Tag resolveTag(TagPtr tagPointer, CompileContext context)
    {
        return context.resolveTag(tagPointer.profile, tagPointer.value, tagPointer.profileSourceInformation, tagPointer.sourceInformation);
    }

    protected static Stereotype resolveStereotype(StereotypePtr stereotypePointer, CompileContext context)
    {
        return context.resolveStereotype(stereotypePointer.profile, stereotypePointer.value, stereotypePointer.profileSourceInformation, stereotypePointer.sourceInformation);
    }
}
