//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.testable.service.extension;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.TestData;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.AggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDecimal;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CLatestDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Class;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ExecutionContextInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedClass;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.MappingInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Pair;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PrimitiveType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PureList;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.RuntimeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.SerializationConfig;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TDSAggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TDSColumnInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TDSSortInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TdsOlapAggregation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TdsOlapRank;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Whatever;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.path.Path;

import java.io.Closeable;
import java.util.List;

public class TestValueSpecificationBuilder implements ValueSpecificationVisitor<ValueSpecification>
{
    private final List<Closeable> closeables;
    private final TestData testData;
    private final PureModelContextData pureModelContextData;

    public TestValueSpecificationBuilder(List<Closeable> closeables, TestData testData, PureModelContextData pureModelContextData)
    {
        this.closeables = closeables;
        this.testData = testData;
        this.pureModelContextData = pureModelContextData;
    }

    @Override
    public ValueSpecification visit(ValueSpecification valueSpecification)
    {
        throw new UnsupportedOperationException("ValueSpecification type - " + valueSpecification.getClass().getSimpleName() + " not supported for building test value specification!!");
    }

    @Override
    public ValueSpecification visit(PackageableElementPtr packageableElementPtr)
    {
        PackageableElement packageableElement = ListIterate.detect(pureModelContextData.getElements(), ele -> ele.getPath().equals(packageableElementPtr.fullPath));

        if (packageableElement instanceof PackageableRuntime)
        {
            PackageableRuntime packageableRuntime = (PackageableRuntime) packageableElement;
            org.eclipse.collections.api.tuple.Pair<EngineRuntime, List<Closeable>> testRuntimeWithCloseable = TestRuntimeBuilder.getTestRuntimeAndClosableResources(packageableRuntime.runtimeValue, testData, pureModelContextData);
            RuntimeInstance runtimeInstance = new RuntimeInstance();
            runtimeInstance.runtime = testRuntimeWithCloseable.getOne();
            closeables.addAll(testRuntimeWithCloseable.getTwo());

            return runtimeInstance;
        }
        return packageableElementPtr;
    }

    @Override
    public ValueSpecification visit(Whatever whatever)
    {
        return whatever;
    }

    @Override
    public ValueSpecification visit(CString cString)
    {
        return cString;
    }

    @Override
    public ValueSpecification visit(CDateTime cDateTime)
    {
        return cDateTime;
    }

    @Override
    public ValueSpecification visit(CLatestDate cLatestDate)
    {
        return cLatestDate;
    }

    @Override
    public ValueSpecification visit(CStrictDate cStrictDate)
    {
        return cStrictDate;
    }

    @Override
    public ValueSpecification visit(CStrictTime cStrictTime)
    {
        return cStrictTime;
    }

    @Override
    public ValueSpecification visit(AggregateValue aggregateValue)
    {
        return aggregateValue;
    }

    @Override
    public ValueSpecification visit(Class aClass)
    {
        return aClass;
    }

    @Override
    public ValueSpecification visit(CBoolean cBoolean)
    {
        return cBoolean;
    }

    @Override
    public ValueSpecification visit(UnknownAppliedFunction unknownAppliedFunction)
    {
        return unknownAppliedFunction;
    }

    @Override
    public ValueSpecification visit(Enum anEnum)
    {
        return anEnum;
    }

    @Override
    public ValueSpecification visit(EnumValue enumValue)
    {
        return enumValue;
    }

    @Override
    public ValueSpecification visit(RuntimeInstance runtimeInstance)
    {
        org.eclipse.collections.api.tuple.Pair<EngineRuntime, List<Closeable>> testRuntimeWithCloseable = TestRuntimeBuilder.getTestRuntimeAndClosableResources(runtimeInstance.runtime, testData, pureModelContextData);
        runtimeInstance.runtime = testRuntimeWithCloseable.getOne();
        closeables.addAll(testRuntimeWithCloseable.getTwo());
        return runtimeInstance;
    }

    @Override
    public ValueSpecification visit(Path path)
    {
        return path;
    }

    @Override
    public ValueSpecification visit(CInteger cInteger)
    {
        return cInteger;
    }

    @Override
    public ValueSpecification visit(CDecimal cDecimal)
    {
        return cDecimal;
    }

    @Override
    public ValueSpecification visit(Lambda lambda)
    {
        return lambda;
    }

    @Override
    public ValueSpecification visit(ExecutionContextInstance executionContextInstance)
    {
        return executionContextInstance;
    }

    @Override
    public ValueSpecification visit(Pair pair)
    {
        return pair;
    }

    @Override
    public ValueSpecification visit(PureList pureList)
    {
        return pureList;
    }

    @Override
    public ValueSpecification visit(Variable variable)
    {
        return variable;
    }

    @Override
    public ValueSpecification visit(CFloat cFloat)
    {
        return cFloat;
    }

    @Override
    public ValueSpecification visit(MappingInstance mappingInstance)
    {
        return mappingInstance;
    }

    @Override
    public ValueSpecification visit(HackedClass hackedClass)
    {
        return hackedClass;
    }

    @Override
    public ValueSpecification visit(Collection collection)
    {
        return collection;
    }

    @Override
    public ValueSpecification visit(AppliedFunction appliedFunction)
    {
        appliedFunction.parameters = ListIterate.collect(appliedFunction.parameters, param -> param.accept(this));
        return appliedFunction;
    }

    @Override
    public ValueSpecification visit(AppliedQualifiedProperty appliedQualifiedProperty)
    {
        return appliedQualifiedProperty;
    }

    @Override
    public ValueSpecification visit(PropertyGraphFetchTree propertyGraphFetchTree)
    {
        return propertyGraphFetchTree;
    }

    @Override
    public ValueSpecification visit(RootGraphFetchTree rootGraphFetchTree)
    {
        return rootGraphFetchTree;
    }

    @Override
    public ValueSpecification visit(SerializationConfig serializationConfig)
    {
        return serializationConfig;
    }

    @Override
    public ValueSpecification visit(AppliedProperty appliedProperty)
    {
        return appliedProperty;
    }

    @Override
    public ValueSpecification visit(TdsOlapAggregation tdsOlapAggregation)
    {
        return tdsOlapAggregation;
    }

    @Override
    public ValueSpecification visit(TDSAggregateValue tdsAggregateValue)
    {
        return tdsAggregateValue;
    }

    @Override
    public ValueSpecification visit(TDSSortInformation tdsSortInformation)
    {
        return tdsSortInformation;
    }

    @Override
    public ValueSpecification visit(TDSColumnInformation tdsColumnInformation)
    {
        return tdsColumnInformation;
    }

    @Override
    public ValueSpecification visit(TdsOlapRank tdsOlapRank)
    {
        return tdsOlapRank;
    }

    @Override
    public ValueSpecification visit(HackedUnit hackedUnit)
    {
        return hackedUnit;
    }

    @Override
    public ValueSpecification visit(UnitInstance unitInstance)
    {
        return unitInstance;
    }

    @Override
    public ValueSpecification visit(UnitType unitType)
    {
        return unitType;
    }

    @Override
    public ValueSpecification visit(KeyExpression keyExpression)
    {
        return keyExpression;
    }

    @Override
    public ValueSpecification visit(PrimitiveType primitiveType)
    {
        return primitiveType;
    }
}
