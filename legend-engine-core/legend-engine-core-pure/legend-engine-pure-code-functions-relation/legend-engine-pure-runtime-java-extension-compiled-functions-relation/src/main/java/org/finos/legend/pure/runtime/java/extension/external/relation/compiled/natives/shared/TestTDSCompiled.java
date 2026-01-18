// Copyright 2023 Goldman Sachs
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

import io.deephaven.csv.reading.CsvReader;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m2.inlinedsl.tds.M2TDSPaths;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;


public class TestTDSCompiled extends TestTDS
{
    private CoreInstance classifierGenericType;

    public TestTDSCompiled(ProcessorSupport processorSupport)
    {
        super(processorSupport);
    }

    public TestTDSCompiled(MutableList<String> columnOrdered, MutableMap<String, GenericType> pureTypes, int rows, ProcessorSupport processorSupport)
    {
        super(columnOrdered, pureTypes, rows, processorSupport);
    }

    public TestTDSCompiled(CsvReader.Result result, CoreInstance classifierGenericType, ProcessorSupport processorSupport)
    {
        super(result, ((RelationType<?>)((GenericType)classifierGenericType)._typeArguments().getFirst()._rawType())._columns().collect(_Column::getColumnType).toList(), processorSupport);
        this.classifierGenericType = classifierGenericType;
    }

    @Override
    public TestTDS newTDS(MutableList<String> columnOrdered, MutableMap<String, GenericType> pureTypes, int rows)
    {
        return new TestTDSCompiled(columnOrdered, pureTypes, rows, this.processorSupport);
    }

    public Object getValueAsCoreInstance(String columnName, int rowNum)
    {
        return this.getValue(columnName, rowNum);
    }

    public GenericType getClassifierGenericType()
    {
        return (GenericType) classifierGenericType;
    }

    @Override
    public TestTDS copy()
    {
        TestTDSCompiled res = (TestTDSCompiled) super.copy();
        res.classifierGenericType = this.classifierGenericType;
        return res;
    }

    @Override
    public TestTDS concatenate(TestTDS tds2)
    {
        TestTDSCompiled res = (TestTDSCompiled) super.concatenate(tds2);
        res.classifierGenericType = this.classifierGenericType;
        return res;
    }

    public TestTDSCompiled updateColumns(ProcessorSupport processorSupport)
    {
        Class<?> relationDatabaseAccessorType = (Class<?>) processorSupport.package_getByUserPath(M2TDSPaths.TDS);
        GenericType genericType = (GenericType) processorSupport.type_wrapGenericType(relationDatabaseAccessorType);
        GenericType typeParam = (GenericType) processorSupport.newGenericType(null, relationDatabaseAccessorType, false);
        MutableList<CoreInstance> columns = columnsOrdered.collect(c -> (CoreInstance) _Column.getColumnInstance(c, false, pureTypesByColumnName.get(c), (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.newMultiplicity(0, 1, processorSupport), null, processorSupport)).toList();
        typeParam._rawType(_RelationType.build(columns, null, processorSupport));
        genericType._typeArguments(Lists.mutable.with(typeParam));
        this.classifierGenericType = genericType;
        return this;
    }
}
