// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_multiplicity_Multiplicity_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

public class SingleValueTDS extends TestTDSCompiled
{
    Object value;
    GenericType genericType;
    CompiledExecutionSupport es;

    public SingleValueTDS(Object value, GenericType genericType, CompiledExecutionSupport es)
    {
        super(es.getProcessorSupport());
        this.value = value;
        this.genericType = genericType;
        this.es = es;
    }

    @Override
    public GenericType getClassifierGenericType()
    {
        Multiplicity m = new Root_meta_pure_metamodel_multiplicity_Multiplicity_Impl("", null, es.getMetadataAccessor().getClass(M3Paths.Multiplicity))
                ._lowerBound(new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl("", null, es.getMetadataAccessor().getClass(M3Paths.MultiplicityValue))._value(1L))
                ._upperBound(new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl("", null, es.getMetadataAccessor().getClass(M3Paths.MultiplicityValue))._value(1L));

        return new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, es.getMetadataAccessor().getClass(M3Paths.GenericType))
                ._rawType(es.getMetadataAccessor().getClass("meta::pure::metamodel::relation::TDS"))
                ._typeArguments(
                        Lists.mutable.with(
                                new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, es.getMetadataAccessor().getClass(M3Paths.GenericType))
                                        ._rawType(_RelationType.build(
                                                Lists.mutable.with(
                                                        _Column.getColumnInstance("value", false, genericType, m, null, es.getProcessorSupport())
                                                ),
                                                null,
                                                es.getProcessorSupport()
                                        ))
                        )
                );
    }

    @Override
    public Object getValueAsCoreInstance(String columnName, int rowNum)
    {
        return value;
    }

    public Object getValue(String columnName, int rowNum)
    {
        return value;
    }

    @Override
    public long getRowCount()
    {
        return 1;
    }
}
