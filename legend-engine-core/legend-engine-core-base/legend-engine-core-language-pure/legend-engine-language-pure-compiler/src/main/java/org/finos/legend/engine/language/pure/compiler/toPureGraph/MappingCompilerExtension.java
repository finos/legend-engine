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
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensions;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.IncludedMappingHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.TestBuilderHelper;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.m3.relationship.Association;
import org.finos.legend.engine.protocol.pure.m3.type.Class;
import org.finos.legend.engine.protocol.pure.m3.type.Enumeration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_Mapping_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingIncludeAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;

import java.util.Set;

public class MappingCompilerExtension implements CompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Mapping");
    }

    @Override
    public CompilerExtension build()
    {
        return new MappingCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        Mapping.class,
                        Lists.fixedSize.with(Enumeration.class, Class.class, Association.class, Binding.class, Store.class),
                        this::mappingFirstPass,
                        this::mappingSecondPass,
                        this::mappingThirdPass,
                        this::mappingPrerequisiteElementsPass
                )
        );
    }

    private PackageableElement mappingFirstPass(Mapping mapping, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping pureMapping = new Root_meta_pure_mapping_Mapping_Impl(mapping.name, SourceInformationHelper.toM3SourceInformation(mapping.sourceInformation), context.pureModel.getClass("meta::pure::mapping::Mapping"));
        GenericType mappingGenericType = context.newGenericType(context.pureModel.getType("meta::pure::mapping::Mapping"));
        return pureMapping._classifierGenericType(mappingGenericType);
    }

    private void mappingSecondPass(Mapping mapping, CompileContext context)
    {
        final org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping pureMapping = context.pureModel.getMapping(context.pureModel.buildPackageString(mapping._package, mapping.name), mapping.sourceInformation);
        RichIterable<EnumerationMapping<Object>> enumerationMappings = ListIterate.collect(mapping.enumerationMappings, em -> HelperMappingBuilder.processEnumMapping(em, pureMapping, context));
        if (!mapping.includedMappings.isEmpty())
        {
            CompilerExtensions extensions = context.pureModel.extensions;
            RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude> mappingIncludes =
                    ListIterate.collect(mapping.includedMappings, i ->
                    {
                        IncludedMappingHandler handler = extensions.getExtraIncludedMappingHandlers(i.getClass().getName());
                        return handler.processMappingInclude(i, context, pureMapping,
                                handler.resolveMapping(i, context));
                    });
            pureMapping._includesAddAll(mappingIncludes);
            // validate no duplicated included mappings
            Set<String> uniqueMappingIncludes = Sets.mutable.empty();
            mappingIncludes.forEach(includedMapping ->
            {
                String mappingName = IncludedMappingHandler.parseIncludedMappingNameRecursively(includedMapping);
                if (!uniqueMappingIncludes.add(mappingName))
                {
                    throw new EngineException("Duplicated mapping include '" + mappingName +
                            "' in " + "mapping " +
                            "'" + context.pureModel.buildPackageString(mapping._package, mapping.name) + "'", mapping.sourceInformation, EngineErrorType.COMPILATION);
                }
            });
        }
        if (!enumerationMappings.isEmpty())
        {
            pureMapping._enumerationMappings(enumerationMappings);
        }
    }

    private void mappingThirdPass(Mapping mapping, CompileContext context)
    {
        final org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping pureMapping = context.pureModel.getMapping(context.pureModel.buildPackageString(mapping._package, mapping.name), mapping.sourceInformation);
        if (mapping.classMappings != null && pureMapping._classMappings().isEmpty())
        {
            RichIterable<Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>>> setImplementations = ListIterate.collect(mapping.classMappings, cm -> cm.accept(new ClassMappingFirstPassBuilder(context, pureMapping)));
            pureMapping._classMappingsAddAll(setImplementations.flatCollect(p -> org.eclipse.collections.impl.factory.Lists.mutable.with(p.getOne()).withAll(p.getTwo())));
        }
        if (!mapping.tests.isEmpty())
        {
            mapping.tests.forEach(t -> HelperMappingBuilder.processMappingTest(t, context));
        }
        if (mapping.testSuites != null)
        {
            TestBuilderHelper.validateTestSuiteIdsList(mapping.testSuites, mapping.sourceInformation);
            pureMapping._tests(ListIterate.collect(mapping.testSuites, suite -> HelperMappingBuilder.processMappingTestAndTestSuite(suite, pureMapping, context)));
        }
        if (mapping.associationMappings != null)
        {
            RichIterable<AssociationImplementation> associationImplementations = ListIterate.collect(mapping.associationMappings, cm -> HelperMappingBuilder.processAssociationImplementation(cm, context, pureMapping));
            pureMapping._associationMappings(associationImplementations);
        }
        if (mapping.classMappings != null)
        {
            mapping.classMappings.forEach(cm -> cm.accept(new ClassMappingSecondPassBuilder(context, pureMapping)));
            mapping.classMappings.forEach(cm -> cm.accept(new ClassMappingThirdPassBuilder(context, pureMapping)));
        }
    }

    private RichIterable<? extends PackageableElement> mappingPrerequisiteElementsPass(Mapping mapping, CompileContext context)
    {
        final org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping pureMapping = context.pureModel.getMapping(context.pureModel.buildPackageString(mapping._package, mapping.name), mapping.sourceInformation);
        return pureMapping._includes().collect(MappingIncludeAccessor::_included);
    }
}
