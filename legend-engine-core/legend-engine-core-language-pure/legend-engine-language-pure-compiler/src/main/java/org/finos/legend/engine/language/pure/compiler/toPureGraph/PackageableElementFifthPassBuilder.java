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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
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
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.slf4j.Logger;

public class PackageableElementFifthPassBuilder implements PackageableElementVisitor<PackageableElement>
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PackageableElementSecondPassBuilder.class);

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
            function.parameters.forEach(p -> p.accept(new ValueSpecificationBuilder(this.context, Lists.mutable.empty(), ctx)));
            body = ListIterate.collect(function.body, expression -> expression.accept(new ValueSpecificationBuilder(this.context, Lists.mutable.empty(), ctx)));
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
        if (mapping.classMappings != null)
        {
            mapping.classMappings.forEach(cm -> cm.accept(new ClassMappingThirdPassBuilder(this.context, pureMapping)));
        }
        return pureMapping;
    }

    @Override
    public PackageableElement visit(PackageableRuntime packageableRuntime)
    {
        return null;
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
