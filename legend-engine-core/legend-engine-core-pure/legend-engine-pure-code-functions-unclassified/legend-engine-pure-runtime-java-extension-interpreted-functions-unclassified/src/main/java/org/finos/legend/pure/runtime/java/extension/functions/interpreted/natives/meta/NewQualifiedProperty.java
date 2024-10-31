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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class NewQualifiedProperty extends NativeFunction
{
    private final ModelRepository repository;

    public NewQualifiedProperty(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {
        String name = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport).getName();
        CoreInstance ownerGenericType = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        CoreInstance targetGenericType = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);
        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(params.get(3), M3Properties.values, processorSupport);
        ListIterable<? extends CoreInstance> variableExpressions = Instance.getValueForMetaPropertyToManyResolved(params.get(4), M3Properties.values, processorSupport);

        CoreInstance newQualifiedProperty = this.repository.newEphemeralCoreInstance(name, processorSupport.package_getByUserPath(M3Paths.QualifiedProperty), null);

        // validate property owner
        CoreInstance owner = Instance.getValueForMetaPropertyToOneResolved(ownerGenericType, M3Properties.rawType, processorSupport);
        if ((owner == null) || !Instance.instanceOf(owner, M3Paths.PropertyOwner, processorSupport))
        {
            StringBuilder message = new StringBuilder("Invalid property owner: ");
            GenericType.print(message, ownerGenericType, processorSupport);
            throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), message.toString(), functionExpressionCallStack);
        }

        // construct function type
        CoreInstance functionType = this.repository.newAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M3Paths.FunctionType));
        CoreInstance funcTypeClassifierGenericType = this.repository.newEphemeralAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M3Paths.GenericType));
        Instance.addValueToProperty(funcTypeClassifierGenericType, M3Properties.rawType, processorSupport.package_getByUserPath(M3Paths.FunctionType), processorSupport);
        Instance.addValueToProperty(functionType, M3Properties.classifierGenericType, funcTypeClassifierGenericType, processorSupport);
        Instance.addValueToProperty(functionType, M3Properties.returnType, targetGenericType, processorSupport);
        Instance.addValueToProperty(functionType, M3Properties.returnMultiplicity, multiplicity, processorSupport);
        Instance.addValueToProperty(functionType, M3Properties.parameters, variableExpressions, processorSupport);

        // classifierGenericType
        CoreInstance genericType = this.repository.newEphemeralAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M3Paths.GenericType));
        Instance.addValueToProperty(genericType, M3Properties.rawType, processorSupport.package_getByUserPath(M3Paths.QualifiedProperty), processorSupport);
        CoreInstance funcTypeGenericType = this.repository.newAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M3Paths.GenericType));
        Instance.addValueToProperty(funcTypeGenericType, M3Properties.rawType, functionType, processorSupport);
        Instance.addValueToProperty(genericType, M3Properties.typeArguments, funcTypeGenericType, processorSupport);

        Instance.addValueToProperty(newQualifiedProperty, M3Properties.classifierGenericType, genericType, processorSupport);

        Instance.addValueToProperty(newQualifiedProperty, M3Properties.genericType, targetGenericType, processorSupport);
        Instance.addValueToProperty(newQualifiedProperty, M3Properties.multiplicity, multiplicity, processorSupport);

        // name
        Instance.addValueToProperty(newQualifiedProperty, M3Properties.name, this.repository.newCoreInstance(name, this.repository.getTopLevel(M3Paths.String), null), processorSupport);
        Instance.addValueToProperty(newQualifiedProperty, M3Properties.functionName, this.repository.newCoreInstance(name, this.repository.getTopLevel(M3Paths.String), null), processorSupport);

        // owner
        Instance.addValueToProperty(newQualifiedProperty, M3Properties.owner, Instance.getValueForMetaPropertyToOneResolved(ownerGenericType, M3Properties.rawType, processorSupport), processorSupport);

        return ValueSpecificationBootstrap.wrapValueSpecification(newQualifiedProperty, true, processorSupport);
    }    
}
