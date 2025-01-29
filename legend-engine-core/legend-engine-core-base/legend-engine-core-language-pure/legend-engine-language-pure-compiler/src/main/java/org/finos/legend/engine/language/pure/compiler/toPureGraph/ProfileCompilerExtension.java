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
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.extension.Profile;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_extension_Profile_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_extension_Stereotype_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_extension_Tag_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag;

public class ProfileCompilerExtension implements CompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Profile");
    }

    @Override
    public CompilerExtension build()
    {
        return new ProfileCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        Profile.class,
                        this::profileFirstPass
                )
        );
    }

    private PackageableElement profileFirstPass(Profile profile, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile targetProfile = new Root_meta_pure_metamodel_extension_Profile_Impl(profile.name, SourceInformationHelper.toM3SourceInformation(profile.sourceInformation), context.pureModel.getClass("meta::pure::metamodel::extension::Profile"));
        return targetProfile._p_stereotypes(ListIterate.collect(profile.stereotypes, st -> newStereotype(targetProfile, st.value, st.sourceInformation, context)))
                ._p_tags(ListIterate.collect(profile.tags, t -> newTag(targetProfile, t.value, t.sourceInformation, context)));
    }

    private Stereotype newStereotype(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile profile, String name, SourceInformation sourceInformation, CompileContext context)
    {
        return new Root_meta_pure_metamodel_extension_Stereotype_Impl(name, SourceInformationHelper.toM3SourceInformation(sourceInformation), context.pureModel.getClass("meta::pure::metamodel::extension::Stereotype"))
                ._value(name)
                ._profile(profile);
    }

    private Tag newTag(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile profile, String name, SourceInformation sourceInformation, CompileContext context)
    {
        return new Root_meta_pure_metamodel_extension_Tag_Impl(name, SourceInformationHelper.toM3SourceInformation(sourceInformation), context.pureModel.getClass("meta::pure::metamodel::extension::Tag"))
                ._value(name)
                ._profile(profile);
    }
}
