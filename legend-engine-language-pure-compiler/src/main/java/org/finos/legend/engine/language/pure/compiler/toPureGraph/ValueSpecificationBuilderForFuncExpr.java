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
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableRuntime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime;

public class ValueSpecificationBuilderForFuncExpr extends ValueSpecificationBuilder implements ValueSpecificationVisitor<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification>
{
    private final CompileContext context;
    private final ProcessingContext processingContext;

    public ValueSpecificationBuilderForFuncExpr(CompileContext context, MutableList<String> openVariables, ProcessingContext processingContext)
    {
        super(context, openVariables, processingContext);
        this.context = context;
        this.processingContext = processingContext;
    }

    @Override
    public ValueSpecification visit(org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr packageableElementPtr)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = this.context.resolvePackageableElement(packageableElementPtr.fullPath, packageableElementPtr.sourceInformation);

        if (packageableElement instanceof Root_meta_pure_runtime_PackageableRuntime)
        {
            Runtime resolvedRuntime = this.context.resolveRuntime(packageableElementPtr.fullPath);
            GenericType runtimeGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.pureModel.getType("meta::pure::runtime::Runtime"));
            return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                    ._genericType(runtimeGenericType)
                    ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                    ._values(this.processingContext.peek().equals("Applying new") ? FastList.newList() : FastList.newListWith(resolvedRuntime));
        }

        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(packageableElement._classifierGenericType())
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(this.processingContext.peek().equals("Applying new") ? FastList.newList() : FastList.newListWith(packageableElement));
    }
}
