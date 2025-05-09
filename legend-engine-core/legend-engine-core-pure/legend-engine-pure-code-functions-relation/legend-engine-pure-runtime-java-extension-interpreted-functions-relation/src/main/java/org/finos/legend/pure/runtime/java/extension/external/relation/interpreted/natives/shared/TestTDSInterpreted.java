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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared;

import io.deephaven.csv.parsers.DataType;
import io.deephaven.csv.reading.CsvReader;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;

import java.math.BigDecimal;

import static org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap.*;

public class TestTDSInterpreted extends TestTDS
{
    private ModelRepository modelRepository;
    private ProcessorSupport processorSupport;

    public TestTDSInterpreted(String csv, ModelRepository repository, ProcessorSupport processorSupport)
    {
        super(csv);
        this.modelRepository = repository;
        this.processorSupport = processorSupport;
    }

    public TestTDSInterpreted(CsvReader.Result result, ModelRepository repository, ProcessorSupport processorSupport)
    {
        super(result);
        this.modelRepository = repository;
        this.processorSupport = processorSupport;
    }

    private TestTDSInterpreted(MutableList<String> columnOrdered, MutableMap<String, DataType> columnType, int rows, ModelRepository repository, ProcessorSupport processorSupport)
    {
        super(columnOrdered, columnType, rows);
        this.modelRepository = repository;
        this.processorSupport = processorSupport;

    }

    public TestTDSInterpreted(ModelRepository repository, ProcessorSupport processorSupport)
    {
        this.modelRepository = repository;
        this.processorSupport = processorSupport;
    }

    @Override
    public TestTDS newTDS()
    {
        return new TestTDSInterpreted(this.modelRepository, this.processorSupport);
    }

    @Override
    public TestTDS newTDS(MutableList<String> columnOrdered, MutableMap<String, DataType> columnType, int rows)
    {
        return new TestTDSInterpreted(columnOrdered, columnType, rows, this.modelRepository, this.processorSupport);
    }

    public CoreInstance getValueAsCoreInstance(String columnName, int rowNum)
    {
        Object dataAsObject = dataByColumnName.get(columnName);
        if (dataAsObject == null)
        {
            throw new RuntimeException("The column " + columnName + " can't be found in the TDS");
        }
        boolean[] isNull = isNullByColumn.get(columnName);
        CoreInstance result;
        switch (columnType.get(columnName))
        {
            case LONG:
            {
                long[] data = (long[]) dataAsObject;
                long value = data[rowNum];
                result = !isNull[rowNum] ? newIntegerLiteral(modelRepository, value, processorSupport) : ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(Lists.mutable.empty(), Type.wrapGenericType(_Package.getByUserPath(M3Paths.Integer, processorSupport), processorSupport), true, processorSupport);
                break;
            }
            case STRING:
            case CUSTOM:
            {
                String[] data = (String[]) dataAsObject;
                String value = data[rowNum];
                result = value != null ? newStringLiteral(modelRepository, value, processorSupport) : ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(Lists.mutable.empty(), Type.wrapGenericType(_Package.getByUserPath(M3Paths.String, processorSupport), processorSupport), true, processorSupport);
                break;
            }
            case BOOLEAN_AS_BYTE:
            {
                boolean[] data = (boolean[]) dataAsObject;
                result = !isNull[rowNum] ? newBooleanLiteral(modelRepository, data[rowNum], processorSupport) : ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(Lists.mutable.empty(), Type.wrapGenericType(_Package.getByUserPath(M3Paths.Float, processorSupport), processorSupport), true, processorSupport);
                break;
            }
            case DOUBLE:
            {
                double[] data = (double[]) dataAsObject;
                result = !isNull[rowNum] ? newFloatLiteral(modelRepository, BigDecimal.valueOf(data[rowNum]), processorSupport) : ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(Lists.mutable.empty(), Type.wrapGenericType(_Package.getByUserPath(M3Paths.Float, processorSupport), processorSupport), true, processorSupport);
                break;
            }
            case DATETIME_AS_LONG:
            {
                PureDate[] data = (PureDate[]) dataAsObject;
                PureDate value = data[rowNum];
                result = value != null ? newDateLiteral(modelRepository, value, processorSupport) : ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(Lists.mutable.empty(), Type.wrapGenericType(_Package.getByUserPath(M3Paths.String, processorSupport), processorSupport), true, processorSupport);
                break;
            }
            default:
                throw new RuntimeException("ERROR " + columnType.get(columnName) + " not supported in getValue");
        }
        return result;
    }
}
