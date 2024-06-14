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

import io.deephaven.csv.parsers.DataType;
import io.deephaven.csv.reading.CsvReader;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m2.inlinedsl.tds.M2TDSPaths;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;


public class TestTDSCompiled extends TestTDS
{
    private CoreInstance classifierGenericType;

    public TestTDSCompiled()
    {
        super();
    }

    public TestTDSCompiled(CsvReader.Result result)
    {
        super(result);
    }

    public TestTDSCompiled(MutableList<String> columnOrdered, MutableMap<String, DataType> columnType, int rows)
    {
        super(columnOrdered, columnType, rows);
    }

    public TestTDSCompiled(CsvReader.Result result, CoreInstance classifierGenericType)
    {
        super(result);
        this.classifierGenericType = classifierGenericType;
    }

    @Override
    public TestTDS newTDS()
    {
        return new TestTDSCompiled();
    }

    @Override
    public TestTDS newTDS(MutableList<String> columnOrdered, MutableMap<String, DataType> columnType, int rows)
    {
        return new TestTDSCompiled(columnOrdered, columnType, rows);
    }

    public Object getValueAsCoreInstance(String columnName, int rowNum)
    {
        Object dataAsObject = dataByColumnName.get(columnName);
        boolean[] isNull = (boolean[]) isNullByColumn.get(columnName);
        Object result;
        switch (columnType.get(columnName))
        {
            case INT:
            {
                int[] data = (int[]) dataAsObject;
                int value = data[rowNum];
                result = !isNull[rowNum] ? (long) value : null;
                break;
            }
            case CHAR:
            {
                char[] data = (char[]) dataAsObject;
                result = !isNull[rowNum] ? "" + data[rowNum] : null;
                break;
            }
            case STRING:
            {
                String[] data = (String[]) dataAsObject;
                result = data[rowNum];
                break;
            }
            case DOUBLE:
            {
                double[] data = (double[]) dataAsObject;
                result = !isNull[rowNum] ? Double.valueOf(data[rowNum]) : null;
                break;
            }
            default:
                throw new RuntimeException("ERROR " + columnType.get(columnName) + " not supported in getValue");
        }
        return result;
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

    public void updateClassifierGenericType(CoreInstance coreInstance)
    {
        this.classifierGenericType = coreInstance;
    }

    public TestTDSCompiled updateColumns(ProcessorSupport processorSupport)
    {
        Class<?> relationDatabaseAccessorType = (Class<?>) processorSupport.package_getByUserPath(M2TDSPaths.TDS);
        GenericType genericType = (GenericType) processorSupport.type_wrapGenericType(relationDatabaseAccessorType);
        GenericType typeParam = (GenericType) processorSupport.newGenericType(null, relationDatabaseAccessorType, false);
        MutableList<CoreInstance> columns = columnsOrdered.collect(c -> (CoreInstance) _Column.getColumnInstance(c,false, typeParam, (GenericType) processorSupport.type_wrapGenericType(_Package.getByUserPath(convert(columnType.get(c)), processorSupport)), null, processorSupport)).toList();
        typeParam._rawType(_RelationType.build(columns, null, processorSupport));
        genericType._typeArguments(Lists.mutable.with(typeParam));
        this.classifierGenericType = genericType;
        return this;
    }

    public String convert(DataType dataType)
    {
        switch (dataType)
        {
            case INT:
                return "Integer";
            case STRING:
            case CHAR:
                return "String";
            case DOUBLE:
                return "Float";
        }
        throw new RuntimeException("To Handle " + dataType);
    }
}
