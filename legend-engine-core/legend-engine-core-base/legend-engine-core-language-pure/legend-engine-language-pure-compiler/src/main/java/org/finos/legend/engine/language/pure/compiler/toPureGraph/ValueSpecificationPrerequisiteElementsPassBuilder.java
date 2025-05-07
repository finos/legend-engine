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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.dsl.store.valuespecification.constant.classInstance.RelationStoreAccessor;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Collection;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.GenericTypeInstance;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.ClassInstance;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation.ColSpec;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation.ColSpecArray;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.EnumValue;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.UnitInstance;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CBoolean;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CByteArray;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CDateTime;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CDecimal;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CFloat;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CInteger;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CLatestDate;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CStrictDate;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CStrictTime;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CString;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Class;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.MappingInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.PrimitiveType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Whatever;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.AggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.Pair;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.PureList;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.RuntimeInstance;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ValueSpecificationPrerequisiteElementsPassBuilder implements ValueSpecificationVisitor<Set<PackageableElementPointer>>
{
    private final CompileContext context;
    private final Set<PackageableElementPointer> prerequisiteElements;

    public ValueSpecificationPrerequisiteElementsPassBuilder(CompileContext context, Set<PackageableElementPointer> prerequisiteElements)
    {
        this.context = context;
        this.prerequisiteElements = prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(ValueSpecification valueSpecification)
    {
        return this.prerequisiteElements; // TODO: Revisit if tests fail
    }

    @Override
    public Set<PackageableElementPointer> visit(PackageableElementPtr packageableElementPtr)
    {
        this.prerequisiteElements.add(new PackageableElementPointer(null, packageableElementPtr.fullPath, packageableElementPtr.sourceInformation));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(ClassInstance iv)
    {
        Procedure2<Object, Set<PackageableElementPointer>> extension = this.context.getCompilerExtensions().getExtraClassInstancePrerequisiteElementsPassProcessors().get(iv.type);
        if (extension != null)
        {
            extension.value(iv.value, this.prerequisiteElements);
            return this.prerequisiteElements;
        }
        switch (iv.type)
        {
            case ">":
            {
                RelationStoreAccessor relationStoreAccessor = (RelationStoreAccessor) iv.value;
                this.prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.STORE, relationStoreAccessor.path.get(0), relationStoreAccessor.sourceInformation));
                return this.prerequisiteElements;
            }
            case "colSpec":
            case "colSpecArray":
            {
                List<ColSpec> colSpecs;
                if (iv.value instanceof ColSpec)
                {
                    colSpecs = Lists.fixedSize.with((ColSpec) iv.value);
                }
                else
                {
                    colSpecs = ((ColSpecArray) iv.value).colSpecs;
                }
                colSpecs.forEach(colSpec ->
                {
                    if (Objects.nonNull(colSpec.function1))
                    {
                        colSpec.function1.accept(this);
                    }
                    if (Objects.nonNull(colSpec.function2))
                    {
                        colSpec.function2.accept(this);
                    }
                    HelperValueSpecificationBuilder.collectPrerequisiteElementsFromGenericType(this.prerequisiteElements, colSpec.genericType, context);
                });
                return this.prerequisiteElements;
            }
            case "keyExpression":
            {
                KeyExpression keyExpression = (KeyExpression) iv.value;
                keyExpression.accept(this);
                return this.prerequisiteElements;
            }
            case "listInstance":
            {
                PureList pureList = (PureList) iv.value;
                ListIterate.forEach(pureList.values, v -> v.accept(this));
                return this.prerequisiteElements;
            }
            case "aggregateValue":
            {
                AggregateValue aggregateValue = (AggregateValue) iv.value;
                aggregateValue.mapFn.accept(this);
                aggregateValue.aggregateFn.accept(this);
                return this.prerequisiteElements;
            }
            case "pair":
            {
                Pair pair = (Pair) iv.value;
                pair.first.accept(this);
                pair.second.accept(this);
                return this.prerequisiteElements;
            }
            case "runtimeInstance":
            {
                HelperRuntimeBuilder.collectPrerequisiteElementsFromPureRuntime(this.prerequisiteElements, ((RuntimeInstance) iv.value).runtime);
                return this.prerequisiteElements;
            }
            default:
            {
                return this.prerequisiteElements;
            }
//            case "path": TODO
//            {
//            }
//            case "rootGraphFetchTree": TODO
//            {
//            }
//            case "propertyGraphFetchTree": TODO
//            {
//            }
        }
    }

    @Override
    public Set<PackageableElementPointer> visit(CString cString)
    {
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(CDateTime cDateTime)
    {
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(CLatestDate cLatestDate)
    {
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(CStrictDate cStrictDate)
    {
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(CStrictTime cStrictTime)
    {
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(CBoolean cBoolean)
    {
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(EnumValue enumValue)
    {
        this.prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.ENUMERATION, enumValue.fullPath, enumValue.sourceInformation));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(CInteger cInteger)
    {
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(CDecimal cDecimal)
    {
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(CByteArray cByteArray)
    {
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(LambdaFunction lambda)
    {
        ListIterate.forEach(lambda.body, v -> v.accept(this));
        ListIterate.forEach(lambda.parameters, p -> p.accept(this));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(Variable variable)
    {
        HelperValueSpecificationBuilder.collectPrerequisiteElementsFromGenericType(this.prerequisiteElements, variable.genericType, context);
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(CFloat cFloat)
    {
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(GenericTypeInstance genericTypeInstance)
    {
        HelperValueSpecificationBuilder.collectPrerequisiteElementsFromGenericType(this.prerequisiteElements, genericTypeInstance.genericType, this.context);
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(Collection collection)
    {
        ListIterate.forEach(collection.values, expression -> expression.accept(this));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(AppliedFunction appliedFunction)
    {
        ListIterate.forEach(appliedFunction.parameters, expression -> expression.accept(this));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(AppliedProperty appliedProperty)
    {
        ListIterate.forEach(appliedProperty.parameters, expression -> expression.accept(this));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(UnitInstance unitInstance)
    {
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(KeyExpression keyExpression)
    {
        keyExpression.key.accept(this);
        keyExpression.expression.accept(this);
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(HackedUnit hackedUnit)
    {
        this.prerequisiteElements.add(new PackageableElementPointer(null, hackedUnit.fullPath, hackedUnit.sourceInformation));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(AppliedQualifiedProperty appliedQualifiedProperty)
    {
        ListIterate.forEach(appliedQualifiedProperty.parameters, expression -> expression.accept(this));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(UnitType unitType)
    {
        this.prerequisiteElements.add(new PackageableElementPointer(null, unitType.fullPath, unitType.sourceInformation));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(PrimitiveType primitiveType)
    {
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(Whatever whatever)
    {
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(MappingInstance mappingInstance)
    {
        this.prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.MAPPING, mappingInstance.fullPath, mappingInstance.sourceInformation));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(Class aClass)
    {
        this.prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.CLASS, aClass.fullPath, aClass.sourceInformation));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(UnknownAppliedFunction unknownAppliedFunction)
    {
        this.prerequisiteElements.add(new PackageableElementPointer(null, unknownAppliedFunction.returnType, unknownAppliedFunction.sourceInformation));
        return this.prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(Enum anEnum)
    {
        this.prerequisiteElements.add(new PackageableElementPointer(null, anEnum.fullPath, anEnum.sourceInformation));
        return this.prerequisiteElements;
    }
}
