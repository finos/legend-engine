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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class NewEnumeration extends NativeFunction
{
    private final ModelRepository repository;

    public NewEnumeration(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {
        String fullPathString = PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(params.getFirst(), M3Properties.values, processorSupport));
        ListIterable<String> fullPath = PackageableElement.splitUserPath(fullPathString);
        if (fullPath.isEmpty())
        {
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "Cannot create a new Enumeration: '" + fullPathString + "'");
        }
        String name = fullPath.getLast();
        CoreInstance pack = _Package.findOrCreatePackageFromUserPath(fullPath.subList(0, fullPath.size() - 1), this.repository, processorSupport);

        CoreInstance newEnumeration = this.repository.newCoreInstance(name, processorSupport.package_getByUserPath(M3Paths.Enumeration), null);

        //classifierGenericType
        CoreInstance selfGenericType = this.repository.newAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M3Paths.GenericType));
        Instance.addValueToProperty(selfGenericType, M3Properties.rawType, newEnumeration, processorSupport);
        CoreInstance genericType = this.repository.newAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M3Paths.GenericType));
        Instance.addValueToProperty(genericType, M3Properties.rawType, processorSupport.package_getByUserPath(M3Paths.Enumeration), processorSupport);
        Instance.addValueToProperty(genericType, M3Properties.typeArguments, selfGenericType, processorSupport);

        // generalizations
        CoreInstance generalization = this.repository.newAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M3Paths.Generalization));
        Instance.addValueToProperty(generalization, M3Properties.specific, newEnumeration, processorSupport);
        Instance.addValueToProperty(generalization, M3Properties.general, Type.wrapGenericType(processorSupport.package_getByUserPath(M3Paths.Enum), processorSupport), processorSupport);

        Instance.addValueToProperty(newEnumeration, M3Properties.classifierGenericType, genericType, processorSupport);
        Instance.addValueToProperty(newEnumeration, M3Properties.generalizations, generalization, processorSupport);

        // name
        Instance.addValueToProperty(newEnumeration, M3Properties.name, this.repository.newCoreInstance(name, this.repository.getTopLevel(M3Paths.String), null), processorSupport);

        Instance.addValueToProperty(newEnumeration, M3Properties._package, pack, processorSupport);

        // Enum values
        ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(params.get(1), M3Properties.values, processorSupport);
        for (CoreInstance val : values)
        {
            Instance.addValueToProperty(newEnumeration, M3Properties.values, this.repository.newCoreInstance(val.getName(), newEnumeration, null), processorSupport);
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(newEnumeration, true, processorSupport);
    }
}
