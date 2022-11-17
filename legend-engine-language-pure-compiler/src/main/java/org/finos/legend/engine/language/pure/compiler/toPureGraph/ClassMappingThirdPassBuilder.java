// Copyright 2020 Goldman Sachs
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

import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMappingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.OperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwareClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PureInstanceClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PurePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.EnumValue;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.core_pure_router_operations_router_operations;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ClassMappingThirdPassBuilder implements ClassMappingVisitor<SetImplementation>
{
    private final CompileContext context;
    private final Mapping parentMapping;

    public ClassMappingThirdPassBuilder(CompileContext context, Mapping parentMapping)
    {
        this.context = context;
        this.parentMapping = parentMapping;
    }

    // NOTE: when we remove this visitor, we can return "void"
    @Override
    public SetImplementation visit(ClassMapping classMapping)
    {
        return null;
    }

    @Override
    public SetImplementation visit(OperationClassMapping classMapping)
    {
        return null;
    }

    @Override
    public SetImplementation visit(PureInstanceClassMapping classMapping)
    {
        PureInstanceSetImplementation s = (PureInstanceSetImplementation) parentMapping._classMappings().select(c -> c._id().equals(HelperMappingBuilder.getClassMappingId(classMapping, this.context))).getFirst();
        s._propertyMappings().forEachWithIndex((ObjectIntProcedure<PropertyMapping>) ((p, i) ->
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PurePropertyMapping pm = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PurePropertyMapping) p;
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> property = pm._property();
            SourceInformation pSourceInformation = SourceInformationHelper.fromM3SourceInformation(p.getSourceInformation());
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification last = pm._transform()._expressionSequence().getLast();

            List<Type> typesToCheck;
            if (property._genericType()._rawType()._classifierGenericType()._rawType()._name().equals("Class"))
            {
                SetImplementation setImplementation;
                if (p._targetSetImplementationId() != null && !p._targetSetImplementationId().equals(""))
                {
                    setImplementation = parentMapping._classMappingByIdRecursive(Lists.fixedSize.with(p._targetSetImplementationId()), this.context.pureModel.getExecutionSupport()).getFirst();
                    Assert.assertTrue(setImplementation != null, () -> "Can't find class mapping '" + p._targetSetImplementationId() + "'", pSourceInformation, EngineErrorType.COMPILATION);
                }
                else
                {
                    setImplementation = parentMapping.classMappingByClass((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<Object>) property._genericType()._rawType(), this.context.pureModel.getExecutionSupport()).getFirst();
                    Assert.assertTrue(setImplementation != null, () -> "Can't find class mapping for '" + HelperModelBuilder.getElementFullPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) property._genericType()._rawType(), this.context.pureModel.getExecutionSupport()) + "'", pSourceInformation, EngineErrorType.COMPILATION);
                }

                List<? extends InstanceSetImplementation> setImpls = core_pure_router_operations_router_operations.Root_meta_pure_router_routing_resolveOperation_SetImplementation_MANY__Mapping_1__InstanceSetImplementation_MANY_(Lists.immutable.of(setImplementation), parentMapping, this.context.pureModel.getExecutionSupport()).toList();
                typesToCheck = setImpls.stream().map(setImpl -> ((PureInstanceSetImplementation) setImpl)._srcClass()).collect(Collectors.toList());
            }
            else if (((org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PurePropertyMapping) p)._transformer() != null)
            {
                EnumerationMapping<Object> m = ((EnumerationMapping<Object>) ((org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PurePropertyMapping) p)._transformer());
                Object val = m._enumValueMappings().getFirst()._sourceValues().getFirst();
                if (val instanceof String)
                {
                    typesToCheck = Collections.singletonList(this.context.pureModel.getType("String"));
                }
                else if (val instanceof Long)
                {
                    typesToCheck = Collections.singletonList(this.context.pureModel.getType("Integer"));
                }
                else if (val instanceof EnumValue)
                {
                    typesToCheck = Collections.singletonList(this.context.resolveEnumeration(((EnumValue) val).fullPath, pSourceInformation));
                }
                else if (val instanceof Enum)
                {
                    GenericType genericType = ((Enum) val)._classifierGenericType();
                    typesToCheck = genericType != null ? Collections.singletonList(this.context.resolveEnumeration(PackageableElement.getUserPathForPackageableElement(genericType._rawType()), pSourceInformation)) : Collections.emptyList();
                }
                else
                {
                    typesToCheck = Collections.emptyList();
                }
            }
            else
            {
                typesToCheck = Collections.singletonList(property._genericType()._rawType());
            }
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity multiplicityToCheck = pm._explodeProperty() != null && pm._explodeProperty() ? this.context.pureModel.getMultiplicity("zeromany") : property._multiplicity();
            List<ValueSpecification> lines = ((PurePropertyMapping) classMapping.propertyMappings.get(i)).transform.body;
            typesToCheck.stream().filter(Objects::nonNull).forEach(t -> checkPureMappingCompatibility(this.context,
                    last._genericType()._rawType(),
                    t,
                    "Error in class mapping '" + HelperModelBuilder.getElementFullPath(parentMapping, this.context.pureModel.getExecutionSupport()) + "' for property '" + pm._property()._name() + "'",
                    lines.get(lines.size() - 1).sourceInformation));

            HelperModelBuilder.checkMultiplicityCompatibility(last._multiplicity(),
                    multiplicityToCheck,
                    "Error in class mapping '" + HelperModelBuilder.getElementFullPath(parentMapping, this.context.pureModel.getExecutionSupport()) + "' for property '" + pm._property()._name() + "'",
                    lines.get(lines.size() - 1).sourceInformation);
        }));
        return s;
    }


    @Override
    public SetImplementation visit(AggregationAwareClassMapping classMapping)
    {
        return null;
    }


    private static void checkPureMappingCompatibility(CompileContext context, Type actualReturnType, Type signatureType, String errorStub, org.finos.legend.engine.protocol.pure.v1.model.SourceInformation errorSourceInformation)
    {
        //In a pure mapping. The src implementation  can be anywhere in the class hierarchy of the return type
        if (signatureType != null && !actualReturnType.equals(signatureType) && !org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(actualReturnType, signatureType, context.pureModel.getExecutionSupport().getProcessorSupport()) && !org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(signatureType, actualReturnType, context.pureModel.getExecutionSupport().getProcessorSupport()))
        {
            throw new EngineException(errorStub + " - Type error: '" + HelperModelBuilder.getElementFullPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) actualReturnType, context.pureModel.getExecutionSupport()) + "' is not in the class hierarchy of '" + HelperModelBuilder.getElementFullPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) signatureType, context.pureModel.getExecutionSupport()) + "'", errorSourceInformation, EngineErrorType.COMPILATION);
        }
    }
}
