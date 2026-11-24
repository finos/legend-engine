// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.sql.compiler;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.sql.metamodel.Expression;
import org.finos.legend.engine.protocol.sql.metamodel.NamedArgumentExpression;
import org.finos.legend.engine.protocol.sql.metamodel.StringLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.TableFunction;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_KeyedExecutionParameter;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_PureMultiExecution;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_PureSingleExecution;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.List;
import java.util.Set;

/**
 * service('/pattern', ...): a service of the model being compiled, matched on its pattern. Every service with the
 * pattern is returned, so the Pure source reports a missing or duplicated pattern.
 */
public class ServiceTableFunctionCompilerExtension implements TableFunctionCompilerExtension
{
    private static final String SERVICE = "service";
    private static final String PATTERN = "pattern";

    @Override
    public List<PackageableElement> extractElements(TableFunction tableFunction, PureModel pureModel)
    {
        MutableList<PackageableElement> elements = Lists.mutable.empty();
        servicesWithPattern(tableFunction, pureModel).forEach(service ->
        {
            elements.add(service);
            if (service._execution() instanceof Root_meta_legend_service_metamodel_PureSingleExecution)
            {
                Root_meta_legend_service_metamodel_PureSingleExecution execution = (Root_meta_legend_service_metamodel_PureSingleExecution) service._execution();
                addRuntime(execution._runtime(), elements, pureModel);
            }
            else if (service._execution() instanceof Root_meta_legend_service_metamodel_PureMultiExecution)
            {
                for (Root_meta_legend_service_metamodel_KeyedExecutionParameter parameter : ((Root_meta_legend_service_metamodel_PureMultiExecution) service._execution())._executionParameters())
                {
                    addRuntime(parameter._runtime(), elements, pureModel);
                }
            }
        });
        return elements.distinct();
    }

    @Override
    public Set<PackageableElementPointer> extractPrerequisiteElements(TableFunction tableFunction, CompileContext context)
    {
        return servicesWithPattern(tableFunction, context.pureModel)
                .collect(service -> new PackageableElementPointer(PackageableElementType.SERVICE, org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(service)))
                .toSet();
    }

    private static RichIterable<Root_meta_legend_service_metamodel_Service> servicesWithPattern(TableFunction tableFunction, PureModel pureModel)
    {
        if (!SERVICE.equals(tableFunction.functionCall.name.parts.get(0)))
        {
            return Lists.mutable.empty();
        }
        String pattern = pattern(tableFunction.functionCall.arguments);
        if (pattern == null)
        {
            return Lists.mutable.empty();
        }
        return pureModel.getAllPackageableElementsOfType(Root_meta_legend_service_metamodel_Service.class).select(service -> pattern.equals(service._pattern())).toList();
    }

    private static String pattern(List<Expression> arguments)
    {
        for (Expression argument : arguments)
        {
            if (argument instanceof NamedArgumentExpression && PATTERN.equals(((NamedArgumentExpression) argument).name) && ((NamedArgumentExpression) argument).expression instanceof StringLiteral)
            {
                return ((StringLiteral) ((NamedArgumentExpression) argument).expression).value;
            }
        }
        return !arguments.isEmpty() && arguments.get(0) instanceof StringLiteral ? ((StringLiteral) arguments.get(0)).value : null;
    }

    private static void addRuntime(Root_meta_core_runtime_Runtime runtime, MutableList<PackageableElement> elements, PureModel pureModel)
    {
        String runtimePath = runtime == null ? null : pureModel.getRuntimePath(runtime);
        if (runtimePath != null)
        {
            elements.add(pureModel.getPackageableElement(runtimePath));
        }
    }
}
