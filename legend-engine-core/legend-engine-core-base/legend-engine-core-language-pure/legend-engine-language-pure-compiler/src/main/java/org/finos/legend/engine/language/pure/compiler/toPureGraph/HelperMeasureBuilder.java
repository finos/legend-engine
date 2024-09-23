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

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Unit;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Unit_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class HelperMeasureBuilder
{
    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit processUnitPackageableElementFirstPass(Unit unit, CompileContext context)
    {
        Class<?> unitClass = context.pureModel.getClass(M3Paths.Unit);
        Class<?> genericTypeClass = context.pureModel.getClass(M3Paths.GenericType);

        SourceInformation sourceInfo = SourceInformationHelper.toM3SourceInformation(unit.sourceInformation);
        GenericType classifierGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", sourceInfo, genericTypeClass)._rawType(unitClass);

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit targetUnit = new Root_meta_pure_metamodel_type_Unit_Impl(unit.name, sourceInfo, unitClass)
                ._name(unit.name)
                ._classifierGenericType(classifierGenericType);

        String unitPath = buildUnitPath(unit, context);
        context.pureModel.typesIndex.put(unitPath, targetUnit);
        context.pureModel.typesGenericTypeIndex.put(unitPath, new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, genericTypeClass)._rawType(targetUnit));
        return targetUnit;
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit processUnitPackageableElementSecondPass(Unit unit, CompileContext context)
    {
        String unitPath = buildUnitPath(unit, context);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit targetUnit = context.resolveUnit(unitPath, unit.sourceInformation);
        targetUnit._measure(context.resolveMeasure(unit.measure, unit.sourceInformation));
        if (unit.conversionFunction != null)
        {
            String lambdaSrcId = unitPath.replace("::", "_");
            SourceInformation si = new SourceInformation(lambdaSrcId, 0, 0, 0, 0);
            ProcessingContext pctx = new ProcessingContext("Unit '" + unitPath + "'");
            ListIterate.forEach(unit.conversionFunction.parameters, param -> pctx.addInferredVariables(param.name, HelperModelBuilder.createVariableValueSpecification(context, param.name)));

            pctx.push("Unit lambda function for " + unit.name);
            LambdaFunction<?> newLambda = HelperValueSpecificationBuilder.buildLambdaWithContext(unit.name, unit.conversionFunction.body, unit.conversionFunction.parameters, context, pctx);
            newLambda.setSourceInformation(si);
            pctx.pop();

            targetUnit._conversionFunction(newLambda);
        }
        return targetUnit;
    }

    private static String buildUnitPath(Unit unit, CompileContext context)
    {
        return context.pureModel.addPrefixToTypeReference(unit.measure) + "~" + unit.name;
    }
}
