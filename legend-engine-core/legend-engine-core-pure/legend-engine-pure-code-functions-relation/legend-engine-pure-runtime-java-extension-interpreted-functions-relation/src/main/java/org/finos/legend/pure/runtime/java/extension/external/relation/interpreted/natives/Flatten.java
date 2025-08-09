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
//

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives;

import io.deephaven.csv.parsers.DataType;
import java.util.Stack;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericTypeCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.variant.Variant;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.Shared;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TestTDSInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

public class Flatten extends Shared
{
    public Flatten(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        ListIterable<? extends CoreInstance> toFlatten = Instance.getValueForMetaPropertyToManyResolved(params.get(0), M3Properties.values, processorSupport);
        CoreInstance toFlattenType = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.genericType, processorSupport);

        CoreInstance colSpec = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);

        Column<?, ?> columnInstance = _Column.getColumnInstance(
                PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(colSpec, M3Properties.name, processorSupport)),
                false,
                GenericTypeCoreInstanceWrapper.toGenericType(toFlattenType),
                (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity) Multiplicity.newMultiplicity(0, 1, processorSupport),
                colSpec.getSourceInformation(),
                processorSupport
        );

        RelationType<?> returnRelationType = _RelationType.build(Lists.fixedSize.with(columnInstance), functionExpressionCallStack.peek().getSourceInformation(), processorSupport);

        CoreInstance returnGenericType = newRelationGenericType(returnRelationType, processorSupport);

        CoreInstance type = Instance.getValueForMetaPropertyToOneResolved(toFlattenType, M3Properties.rawType, processorSupport);
        DataType colResType;
        Object colRes;

        if (type == _Package.getByUserPath(M3Paths.String, processorSupport))
        {
            colResType = DataType.STRING;
            colRes = toFlatten.collect(PrimitiveUtilities::getStringValue).toArray(new String[0]);
        }
        else if (type == _Package.getByUserPath(M3Paths.Integer, processorSupport))
        {
            colResType = DataType.LONG;
            colRes = toFlatten.collectLong(x -> PrimitiveUtilities.getIntegerValue(x).longValue()).toArray();
        }
        else if (type == _Package.getByUserPath(M3Paths.Float, processorSupport))
        {
            colResType = DataType.DOUBLE;
            colRes = toFlatten.collectDouble(x -> PrimitiveUtilities.getFloatValue(x).doubleValue()).toArray();
        }
        else if (type == _Package.getByUserPath(M3Paths.Boolean, processorSupport))
        {
            colResType = DataType.BOOLEAN_AS_BYTE;
            colRes = toFlatten.collectByte(x -> PrimitiveUtilities.getBooleanValue(x) ? (byte) 1 : (byte) 0).toArray();
        }
        else if (type == _Package.getByUserPath(M3Paths.Variant, processorSupport))
        {
            colResType = DataType.CUSTOM;
            colRes = toFlatten.toArray(new Variant[0]);
        }
        else
        {
            throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "Flatten does not support type: " + GenericType.print(toFlattenType, processorSupport) + ". Supported types are String, Integer, Float, Boolean, and Variant.");
        }

        TestTDSInterpreted tds = new TestTDSInterpreted(this.repository, processorSupport);
        tds.addColumn(columnInstance._name(), colResType, colRes);

        return ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(tds, returnGenericType, repository, processorSupport), true, processorSupport);
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType newRelationGenericType(RelationType<?> relationType, ProcessorSupport processorSupport)
    {
        SourceInformation sourceInfo = relationType.getSourceInformation();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> tdsType = (Class<?>) processorSupport.package_getByUserPath(M3Paths.Relation);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType typeParam = ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType) processorSupport.newAnonymousCoreInstance(sourceInfo, M3Paths.GenericType))._rawType(relationType);
        return ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType) processorSupport.newAnonymousCoreInstance(sourceInfo, M3Paths.GenericType))
                ._rawType(tdsType)
                ._typeArgumentsAdd(typeParam);
    }
}
