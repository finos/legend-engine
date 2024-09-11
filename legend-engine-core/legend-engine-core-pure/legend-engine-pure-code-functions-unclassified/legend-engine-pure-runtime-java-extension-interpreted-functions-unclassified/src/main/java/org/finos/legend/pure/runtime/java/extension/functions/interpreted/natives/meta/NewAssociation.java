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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class NewAssociation extends NativeFunction
{
    private final ModelRepository repository;

    public NewAssociation(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
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
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "Cannot create a new Association: '" + fullPathString + "'");
        }
        String name = fullPath.getLast();
        CoreInstance pack = _Package.findOrCreatePackageFromUserPath(fullPath.subList(0, fullPath.size() - 1), this.repository, processorSupport);

        CoreInstance newAssociation = this.repository.newCoreInstance(name, processorSupport.package_getByUserPath(M3Paths.Association), null);
        Instance.addValueToProperty(newAssociation, M3Properties.name, this.repository.newCoreInstance(name, this.repository.getTopLevel(M3Paths.String), null), processorSupport);
        Instance.addValueToProperty(newAssociation, M3Properties.properties, Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport), processorSupport);
        Instance.addValueToProperty(newAssociation, M3Properties.properties, Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport), processorSupport);
        PostProcessor.process(Lists.immutable.with(newAssociation), this.repository, new ParserLibrary(), new InlineDSLLibrary(), null, context, processorSupport, null, null);

        Instance.addValueToProperty(newAssociation, M3Properties._package, pack, processorSupport);

        return ValueSpecificationBootstrap.wrapValueSpecification(newAssociation, true, processorSupport);
    }
}
