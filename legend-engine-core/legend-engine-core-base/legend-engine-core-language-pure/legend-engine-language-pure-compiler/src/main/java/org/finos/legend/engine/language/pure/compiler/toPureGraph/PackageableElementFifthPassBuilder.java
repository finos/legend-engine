// Copyright 2020 Goldman Sachs
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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
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
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_core_runtime_EngineRuntime_Impl;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableRuntime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageableElementFifthPassBuilder implements PackageableElementVisitor<PackageableElement>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageableElementFifthPassBuilder.class);
    private final CompileContext context;

    public PackageableElementFifthPassBuilder(CompileContext context)
    {
        this.context = context;
    }

    @Override
    public PackageableElement visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement element)
    {
        this.context.getExtraProcessorOrThrow(element).processFifthPass(element, this.context);
        return null;
    }

    @Override
    public PackageableElement visit(Profile profile)
    {
        return null;
    }

    @Override
    public PackageableElement visit(Enumeration _enum)
    {
        return null;
    }

    @Override
    public PackageableElement visit(Class _class)
    {
        return null;
    }

    @Override
    public PackageableElement visit(Association association)
    {
        return null;
    }

    @Override
    public PackageableElement visit(Function function)
    {
        String packageString = this.context.pureModel.buildPackageString(function._package, HelperModelBuilder.getSignature(function));
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> targetFunc = this.context.pureModel.getConcreteFunctionDefinition(packageString, function.sourceInformation);
        ProcessingContext ctx = new ProcessingContext("Function '" + packageString + "' Fifth Pass");
        MutableList<ValueSpecification> body;
        try
        {
            function.parameters.forEach(p -> p.accept(new ValueSpecificationBuilder(this.context, org.eclipse.collections.api.factory.Lists.mutable.empty(), ctx)));
            body = ListIterate.collect(function.body, expression -> expression.accept(new ValueSpecificationBuilder(this.context, org.eclipse.collections.api.factory.Lists.mutable.empty(), ctx)));
        }
        catch (Exception e)
        {
            LOGGER.warn(new LogInfo(Identity.getAnonymousIdentity().getName(), LoggingEventType.GRAPH_EXPRESSION_ERROR, "Can't build function '" + packageString + "' - stack: " + ctx.getStack()).toString());
            throw e;
        }
        FunctionType fType = ((FunctionType) targetFunc._classifierGenericType()._typeArguments().getFirst()._rawType());
        HelperModelBuilder.checkCompatibility(this.context, body.getLast()._genericType()._rawType(), body.getLast()._multiplicity(), fType._returnType()._rawType(), fType._returnMultiplicity(), "Error in function '" + packageString + "'", function.body.get(function.body.size() - 1).sourceInformation);
        ctx.pop();
        targetFunc._expressionSequence(body);
        HelperFunctionBuilder.processFunctionSuites(function, targetFunc, this.context, ctx);
        return targetFunc;
    }

    @Override
    public PackageableElement visit(Measure measure)
    {
        return null;
    }

    @Override
    public PackageableElement visit(Mapping mapping)
    {
        final org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping pureMapping = this.context.pureModel.getMapping(this.context.pureModel.buildPackageString(mapping._package, mapping.name), mapping.sourceInformation);
        if (mapping.associationMappings != null)
        {
            RichIterable<AssociationImplementation> associationImplementations = ListIterate.collect(mapping.associationMappings, cm -> HelperMappingBuilder.processAssociationImplementation(cm, this.context, pureMapping));
            pureMapping._associationMappings(associationImplementations);
        }
        if (mapping.classMappings != null)
        {
            mapping.classMappings.forEach(cm -> cm.accept(new ClassMappingSecondPassBuilder(this.context, pureMapping)));
        }
        return pureMapping;
    }

    @Override
    public PackageableElement visit(PackageableRuntime packageableRuntime)
    {
        String fullPath = this.context.pureModel.buildPackageString(packageableRuntime._package, packageableRuntime.name);
        Root_meta_pure_runtime_PackageableRuntime metamodel = this.context.pureModel.getPackageableRuntime(fullPath, packageableRuntime.sourceInformation);
        Root_meta_core_runtime_Runtime runtime = this.context.pureModel.getRuntime(fullPath);
        HelperRuntimeBuilder.buildEngineRuntime(packageableRuntime.runtimeValue, runtime, this.context);
        metamodel._runtimeValue(new Root_meta_core_runtime_EngineRuntime_Impl("", SourceInformationHelper.toM3SourceInformation(packageableRuntime.sourceInformation), context.pureModel.getClass("meta::core::runtime::EngineRuntime"))._mappings(ListIterate.collect(packageableRuntime.runtimeValue.mappings, mappingPointer -> context.resolveMapping(mappingPointer.path, mappingPointer.sourceInformation)))._connectionStores(runtime._connectionStores()));
        return metamodel;
    }

    @Override
    public PackageableElement visit(PackageableConnection packageableConnection)
    {
        return null;
    }

    @Override
    public PackageableElement visit(SectionIndex sectionIndex)
    {
        return null;
    }

    @Override
    public PackageableElement visit(DataElement dataElement)
    {
        return null;
    }
}
