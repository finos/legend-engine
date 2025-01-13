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
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.TestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ExecutionEnvironmentInstance;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ExecutionParameters;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.SingleExecutionParameters;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.MultiExecutionParameters;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CByteArray;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Class;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.PrimitiveType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CDecimal;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CLatestDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CStrictDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CStrictTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ClassInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.packageableElement.GenericTypeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.UnitInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Whatever;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.MappingInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.packageableElement.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.RuntimeInstance;

import java.io.Closeable;
import java.util.List;

public class TestValueSpecificationBuilder implements ValueSpecificationVisitor<ValueSpecification>
{
    private final List<Closeable> closeables;
    private final TestData testData;
    private final PureModelContextData pureModelContextData;
    private List<String> keys = null;

    public TestValueSpecificationBuilder(List<Closeable> closeables, TestData testData, PureModelContextData pureModelContextData)
    {
        this.closeables = closeables;
        this.testData = testData;
        this.pureModelContextData = pureModelContextData;
    }

    public TestValueSpecificationBuilder(List<String> keys, List<Closeable> closeables, TestData testData, PureModelContextData pureModelContextData)
    {
        this.closeables = closeables;
        this.testData = testData;
        this.pureModelContextData = pureModelContextData;
        this.keys = keys;
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
            org.eclipse.collections.api.tuple.Pair<Runtime, List<Closeable>> testRuntimeWithCloseable = TestRuntimeBuilder.getTestRuntimeAndClosableResources(packageableRuntime.runtimeValue, testData, pureModelContextData);
            RuntimeInstance runtimeInstance = new RuntimeInstance();
            runtimeInstance.runtime = testRuntimeWithCloseable.getOne();
            closeables.addAll(testRuntimeWithCloseable.getTwo());
            return new ClassInstance("runtimeInstance", runtimeInstance, runtimeInstance.sourceInformation);
        }
        else if (packageableElement instanceof ExecutionEnvironmentInstance)
        {
            ExecutionEnvironmentInstance testExecutionEnvironment = (ExecutionEnvironmentInstance) packageableElement;
            testExecutionEnvironment.executionParameters.stream().forEach(param -> getTestParameters(param));
            return new ClassInstance("executionEnvironmentInstance", testExecutionEnvironment, testExecutionEnvironment.sourceInformation);
        }
        return packageableElementPtr;
    }

    private void getTestParameters(ExecutionParameters params)
    {
        if (params instanceof SingleExecutionParameters && keys.contains(((SingleExecutionParameters) params).key))
        {
            org.eclipse.collections.api.tuple.Pair<Runtime, List<Closeable>> testRuntimeWithCloseable = TestRuntimeBuilder.getTestRuntimeAndClosableResources(((SingleExecutionParameters) params).runtime, testData, pureModelContextData);
            ((SingleExecutionParameters) params).runtime = testRuntimeWithCloseable.getOne();
            closeables.addAll(testRuntimeWithCloseable.getTwo());
        }
        else if (params instanceof MultiExecutionParameters)
        {
            ((MultiExecutionParameters) params).singleExecutionParameters.stream().filter(param -> keys.contains(param.key)).forEach(param -> getTestParameters(param));
        }
    }

    @Override
    public ValueSpecification visit(ClassInstance iv)
    {
        if (iv.type.equals("runtimeInstance"))
        {
            return processClassInstanceToRuntime((RuntimeInstance) iv.value);
        }
        return iv;
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

    public ValueSpecification processClassInstanceToRuntime(RuntimeInstance runtimeInstance)
    {
        org.eclipse.collections.api.tuple.Pair<Runtime, List<Closeable>> testRuntimeWithCloseable = TestRuntimeBuilder.getTestRuntimeAndClosableResources(runtimeInstance.runtime, testData, pureModelContextData);
        runtimeInstance.runtime = testRuntimeWithCloseable.getOne();
        closeables.addAll(testRuntimeWithCloseable.getTwo());
        return new ClassInstance("runtimeInstance", runtimeInstance, runtimeInstance.sourceInformation);
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
    public ValueSpecification visit(CByteArray cByteArray)
    {
        return cByteArray;
    }

    @Override
    public ValueSpecification visit(GenericTypeInstance genericTypeInstance)
    {
        return genericTypeInstance;
    }

    @Override
    public ValueSpecification visit(MappingInstance mappingInstance)
    {
        return mappingInstance;
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
    public ValueSpecification visit(AppliedProperty appliedProperty)
    {
        return appliedProperty;
    }

    @Override
    public ValueSpecification visit(AppliedQualifiedProperty appliedQualifiedProperty)
    {
        return appliedQualifiedProperty;
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