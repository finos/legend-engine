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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Profile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingIncludeAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

public class PackageableElementPrerequisiteFinder implements PackageableElementVisitor<RichIterable<? extends PackageableElement>>
{
    private final CompileContext context;

    public PackageableElementPrerequisiteFinder(CompileContext context)
    {
        this.context = context;
    }

    @Override
    public RichIterable<? extends PackageableElement> visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        return this.context.getExtraProcessorOrThrow(element).getPrerequisiteElements(element, this.context);
    }

    @Override
    public RichIterable<? extends PackageableElement> visit(Profile profile)
    {
        return Lists.fixedSize.empty();
    }

    @Override
    public RichIterable<? extends PackageableElement> visit(Enumeration _enum)
    {
        return Lists.fixedSize.empty();
    }

    @Override
    public RichIterable<? extends PackageableElement> visit(Class _class)
    {
        return Lists.fixedSize.empty();
    }

    @Override
    public RichIterable<? extends PackageableElement> visit(Association association)
    {
        return Lists.fixedSize.empty();
    }

    @Override
    public RichIterable<? extends PackageableElement> visit(Function function)
    {
        return Lists.fixedSize.empty();
    }

    @Override
    public RichIterable<? extends PackageableElement> visit(Measure measure)
    {
        return Lists.fixedSize.empty();
    }

    @Override
    public RichIterable<? extends PackageableElement> visit(SectionIndex sectionIndex)
    {
        return Lists.fixedSize.empty();
    }

    @Override
    public RichIterable<? extends PackageableElement> visit(Mapping mapping)
    {
        final org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping pureMapping = this.context.pureModel.getMapping(this.context.pureModel.buildPackageString(mapping._package, mapping.name), mapping.sourceInformation);
        return pureMapping._includes().collect(MappingIncludeAccessor::_included);
    }

    @Override
    public RichIterable<? extends PackageableElement> visit(PackageableRuntime packageableRuntime)
    {
        return Lists.fixedSize.empty();
    }

    @Override
    public RichIterable<? extends PackageableElement> visit(PackageableConnection packageableConnection)
    {
        return Lists.fixedSize.empty();
    }

    @Override
    public RichIterable<? extends PackageableElement> visit(DataElement dataElement)
    {
        return Lists.fixedSize.empty();
    }
}
