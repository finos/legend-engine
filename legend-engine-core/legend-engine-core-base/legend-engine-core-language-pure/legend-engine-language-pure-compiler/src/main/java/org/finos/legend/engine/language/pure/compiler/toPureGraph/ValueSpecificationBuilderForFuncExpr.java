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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableRuntime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;

import java.util.Objects;

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
        if (packageableElementPtr.fullPath.contains("~"))
        {
            // for backward compatibility, since some protocol versions use PackageableElementPtr for units
            return visit(new UnitType(packageableElementPtr.fullPath, packageableElementPtr.sourceInformation));
        }

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = this.context.resolvePackageableElement(packageableElementPtr.fullPath, packageableElementPtr.sourceInformation);

        if (packageableElement instanceof Root_meta_pure_runtime_PackageableRuntime)
        {
            Root_meta_core_runtime_Runtime resolvedRuntime = this.context.resolveRuntime(packageableElementPtr.fullPath);
            GenericType runtimeGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(this.context.pureModel.getType("meta::core::runtime::Runtime"));
            return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(packageableElementPtr.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                    ._genericType(runtimeGenericType)
                    ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                    ._values(Lists.mutable.with(resolvedRuntime));
        }

        MutableList<InstanceValue> values = this.context.getCompilerExtensions().getExtraValueSpecificationBuilderForFuncExpr().collect(x -> x.value(packageableElement, this.context, this.processingContext), Lists.mutable.empty());
        values.removeIf(Objects::isNull);
        switch (values.size())
        {
            case 0:
            {
                return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(packageableElementPtr.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                        ._genericType(packageableElement._classifierGenericType())
                        ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                        ._values(Lists.mutable.with(packageableElement));
            }
            case 1:
            {
                return values.get(0);
            }
            default:
            {
                throw new EngineException("More than one handler found for the Packageable Element '" + packageableElementPtr.fullPath + "'", packageableElementPtr.sourceInformation, EngineErrorType.COMPILATION);
            }
        }
    }
}
