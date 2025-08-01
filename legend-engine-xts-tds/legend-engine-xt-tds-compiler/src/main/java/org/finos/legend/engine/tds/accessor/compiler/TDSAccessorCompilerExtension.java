// Copyright 2025 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.tds.accessor.compiler;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.tds.accessor.protocol.TDSContainer;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl;
import org.finos.legend.pure.m2.inlinedsl.tds.TDSExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.TDS;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Map;

public class TDSAccessorCompilerExtension implements CompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return Lists.mutable.with("ClassInstance", "TDSRelationAccessor");
    }

    @Override
    public CompilerExtension build()
    {
        return new TDSAccessorCompilerExtension();
    }

    @Override
    public Map<String, Function3<Object, CompileContext, ProcessingContext, ValueSpecification>> getExtraClassInstanceProcessors()
    {
        return Maps.mutable.with("TDS", new Function3<Object, CompileContext, ProcessingContext, ValueSpecification>()
        {
            @Override
            public ValueSpecification value(Object o, CompileContext compileContext, ProcessingContext processingContext)
            {
                TDSContainer tdsContainer = (TDSContainer) o;

                TDS<?> tds = TDSExtension.parse(tdsContainer.tdsString.trim(), (SourceInformation) null, compileContext.pureModel.getExecutionSupport().getProcessorSupport());

                return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, compileContext.pureModel.getClass(M3Paths.InstanceValue))
                        ._genericType(tds._classifierGenericType())
                        ._multiplicity(compileContext.pureModel.getMultiplicity("one"))
                        ._values(Lists.mutable.with(tds));
            }
        });
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.mutable.empty();
    }

    @Override
    public MutableMap<String, MutableSet<String>> getExtraSubtypesForFunctionMatching()
    {
        return Maps.mutable.with("cov_relation_Relation", Sets.mutable.with("TDS"));
    }
}
