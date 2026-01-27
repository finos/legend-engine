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

import io.deephaven.csv.reading.CsvReader;

import java.math.BigDecimal;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.variant.Variant;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;

import static org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap.*;

public class TestTDSInterpreted extends TestTDS
{
    protected ModelRepository modelRepository;

    public TestTDSInterpreted(CsvReader.Result result, MutableList<GenericType> pureTypes, ModelRepository repository, ProcessorSupport processorSupport)
    {
        super(result, pureTypes, processorSupport);
        this.modelRepository = repository;
    }

    private TestTDSInterpreted(MutableList<String> columnOrdered, MutableMap<String, GenericType> pureTypesByColumnName, int rows, ModelRepository repository, ProcessorSupport processorSupport)
    {
        super(columnOrdered, pureTypesByColumnName, rows, processorSupport);
        this.modelRepository = repository;
        this.pureTypesByColumnName = pureTypesByColumnName;
    }

    public TestTDSInterpreted(ModelRepository repository, ProcessorSupport processorSupport)
    {
        super(processorSupport);
        this.modelRepository = repository;
    }

    @Override
    public TestTDS newTDS(MutableList<String> columnOrdered, MutableMap<String, GenericType> pureTypesByColumnName, int rows)
    {
        return new TestTDSInterpreted(columnOrdered, pureTypesByColumnName, rows, this.modelRepository, this.processorSupport);
    }

    public CoreInstance getValueAsCoreInstance(String columnName, int rowNum)
    {
        Object dataAsObject = dataByColumnName.get(columnName);
        if (dataAsObject == null)
        {
            throw new RuntimeException("The column " + columnName + " can't be found in the TDS");
        }
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type pureType = pureTypesByColumnName.get(columnName)._rawType();
        CoreInstance result;
        Object[] data = (Object[]) dataAsObject;
        if (processorSupport.type_subTypeOf(pureType, processorSupport.package_getByUserPath(M3Paths.Integer)))
        {
            result = data[rowNum] == null ? ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(Lists.mutable.empty(), Type.wrapGenericType(_Package.getByUserPath(M3Paths.Integer, processorSupport), processorSupport), true, processorSupport) : newIntegerLiteral(modelRepository, (Long) data[rowNum], processorSupport);
        }
        else if (processorSupport.type_subTypeOf(pureType, processorSupport.package_getByUserPath(M3Paths.Variant)))
        {
            result = data[rowNum] == null ? ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(Lists.mutable.empty(), Type.wrapGenericType(_Package.getByUserPath(M3Paths.Variant, processorSupport), processorSupport), true, processorSupport) : wrapValueSpecification((Variant) data[rowNum], true, processorSupport);
        }
        else if (processorSupport.type_subTypeOf(pureType, processorSupport.package_getByUserPath(M3Paths.String)))
        {
            result = data[rowNum] == null ? ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(Lists.mutable.empty(), Type.wrapGenericType(_Package.getByUserPath(M3Paths.String, processorSupport), processorSupport), true, processorSupport) : newStringLiteral(modelRepository, (String) data[rowNum], processorSupport);
        }
        else if (processorSupport.type_subTypeOf(pureType, processorSupport.package_getByUserPath(M3Paths.Boolean)))
        {
            result = data[rowNum] == null ? ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(Lists.mutable.empty(), Type.wrapGenericType(_Package.getByUserPath(M3Paths.Boolean, processorSupport), processorSupport), true, processorSupport) : newBooleanLiteral(modelRepository, (Boolean) data[rowNum], processorSupport);
        }
        else if (processorSupport.type_subTypeOf(pureType, processorSupport.package_getByUserPath(M3Paths.Float)))
        {
            result = data[rowNum] == null ? ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(Lists.mutable.empty(), Type.wrapGenericType(_Package.getByUserPath(M3Paths.Float, processorSupport), processorSupport), true, processorSupport) : newFloatLiteral(modelRepository, BigDecimal.valueOf((Double) data[rowNum]), processorSupport);
        }
        else if (processorSupport.type_subTypeOf(pureType, processorSupport.package_getByUserPath(M3Paths.Date)))
        {
            result = data[rowNum] == null ? ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(Lists.mutable.empty(), Type.wrapGenericType(_Package.getByUserPath(M3Paths.Date, processorSupport), processorSupport), true, processorSupport) : newDateLiteral(modelRepository, (PureDate) data[rowNum], processorSupport);
        }
        else if (processorSupport.type_subTypeOf(pureType, processorSupport.package_getByUserPath(M3Paths.Decimal)))
        {
            result = data[rowNum] == null ? ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(Lists.mutable.empty(), Type.wrapGenericType(_Package.getByUserPath(M3Paths.Decimal, processorSupport), processorSupport), true, processorSupport) : wrapValueSpecification(modelRepository.newDecimalCoreInstance((BigDecimal) data[rowNum]), true, processorSupport);
        }
        else if (processorSupport.type_subTypeOf(pureType, processorSupport.package_getByUserPath(M3Paths.Number)))
        {
            result = data[rowNum] == null ? ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(Lists.mutable.empty(), Type.wrapGenericType(_Package.getByUserPath(M3Paths.Number, processorSupport), processorSupport), true, processorSupport) : wrapValueSpecification((data[rowNum] instanceof BigDecimal ? modelRepository.newDecimalCoreInstance((BigDecimal) data[rowNum]) : modelRepository.newFloatCoreInstance(BigDecimal.valueOf((double) data[rowNum]))), true, processorSupport);
        }
        else
        {
            throw new RuntimeException("Error " + pureType._name());
        }
        return result;
    }
}
