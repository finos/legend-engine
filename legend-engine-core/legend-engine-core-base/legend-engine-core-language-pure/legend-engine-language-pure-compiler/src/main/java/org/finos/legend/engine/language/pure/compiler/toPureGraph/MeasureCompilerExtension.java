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
import org.finos.legend.engine.protocol.pure.v1.model.domain.Measure;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Measure_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;

public class MeasureCompilerExtension implements CompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Measure");
    }

    @Override
    public CompilerExtension build()
    {
        return new MeasureCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        Measure.class,
                        this::measureFirstPass,
                        this::measureSecondPass
                )
        );
    }

    private PackageableElement measureFirstPass(Measure measure, CompileContext context)
    {
        String fullPath = context.pureModel.buildPackageString(measure._package, measure.name);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure targetMeasure = new Root_meta_pure_metamodel_type_Measure_Impl(measure.name, SourceInformationHelper.toM3SourceInformation(measure.sourceInformation), null);
        context.pureModel.typesIndex.put(fullPath, targetMeasure);
        GenericType genericType = context.newGenericType(targetMeasure);
        context.pureModel.typesGenericTypeIndex.put(fullPath, genericType);
        targetMeasure._classifierGenericType(context.newGenericType(context.pureModel.getType(M3Paths.Measure)));
        if (measure.canonicalUnit != null)
        {
            HelperMeasureBuilder.processUnitPackageableElementFirstPass(measure.canonicalUnit, context);
        }
        measure.nonCanonicalUnits.forEach(ncu -> HelperMeasureBuilder.processUnitPackageableElementFirstPass(ncu, context));
        return targetMeasure;
    }

    private void measureSecondPass(Measure measure, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure targetMeasure = context.pureModel.getMeasure(context.pureModel.buildPackageString(measure._package, measure.name), measure.sourceInformation);
        if (measure.canonicalUnit != null)
        {
            targetMeasure._canonicalUnit(HelperMeasureBuilder.processUnitPackageableElementSecondPass(measure.canonicalUnit, context));
        }
        targetMeasure._nonCanonicalUnits(ListIterate.collect(measure.nonCanonicalUnits, ncu -> HelperMeasureBuilder.processUnitPackageableElementSecondPass(ncu, context)));
    }
}
