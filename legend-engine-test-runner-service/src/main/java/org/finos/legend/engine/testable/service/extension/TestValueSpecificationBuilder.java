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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.TestData;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Class;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PrimitiveType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDecimal;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CLatestDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ClassInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.GenericTypeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Whatever;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.MappingInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.RuntimeInstance;

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
            org.eclipse.collections.api.tuple.Pair<Runtime, List<Closeable>> testRuntimeWithCloseable = TestRuntimeBuilder.getTestRuntimeAndClosableResources(packageableRuntime.runtimeValue, testData, pureModelContextData);
            RuntimeInstance runtimeInstance = new RuntimeInstance();
            runtimeInstance.runtime = testRuntimeWithCloseable.getOne();
            closeables.addAll(testRuntimeWithCloseable.getTwo());
            return new ClassInstance("runtimeInstance", runtimeInstance);
        }
        return packageableElementPtr;
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
        return new ClassInstance("runtimeInstance", runtimeInstance);
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
    public ValueSpecification visit(GenericTypeInstance genericTypeInstance)
    {
        return null;
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