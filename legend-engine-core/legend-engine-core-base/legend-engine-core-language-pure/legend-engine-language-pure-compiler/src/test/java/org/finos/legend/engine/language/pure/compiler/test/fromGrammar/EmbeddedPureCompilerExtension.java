// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.test.fromGrammar;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.embedded.extensions.NewValueSpecificationForTest;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.shared.core.function.Procedure3;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;

import java.util.Map;
import java.util.Set;

public class EmbeddedPureCompilerExtension implements CompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Embedded");
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.mutable.empty();
    }

    @Override
    public CompilerExtension build()
    {
        return new EmbeddedPureCompilerExtension();
    }

    // #Test{body:<function name>}# reads that function's compiled body, so it only compiles when the function's third
    // pass has already run: the island's prerequisites decide the order.
    private static final String READ_BODY_OF = "body:";

    @Override
    public Map<String, Function3<Object, CompileContext, ProcessingContext, ValueSpecification>> getExtraClassInstanceProcessors()
    {
        return Maps.mutable.with("Test", (obj, context, processingContext) ->
        {
            String value = ((NewValueSpecificationForTest) obj).x;
            if (value.startsWith(READ_BODY_OF))
            {
                String functionName = value.substring(READ_BODY_OF.length());
                ConcreteFunctionDefinition<?> function = findFunction(functionName, context);
                if (function == null)
                {
                    throw new EngineException("Can't find function '" + functionName + "'", null, EngineErrorType.COMPILATION);
                }
                if (function._expressionSequence().isEmpty())
                {
                    throw new EngineException("The body of '" + functionName + "' has not been compiled yet", null, EngineErrorType.COMPILATION);
                }
            }
            return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                    ._genericType(context.pureModel.getGenericType("String"))
                    ._multiplicity(context.pureModel.getMultiplicity("one"))
                    ._values(Lists.mutable.with(value));
        });
    }

    @Override
    public Map<String, Procedure3<Object, CompileContext, Set<PackageableElementPointer>>> getExtraClassInstancePrerequisiteElementsPassProcessorsWithContext()
    {
        return Maps.mutable.with("Test", (obj, context, prerequisiteElements) ->
        {
            String value = ((NewValueSpecificationForTest) obj).x;
            if (value.startsWith(READ_BODY_OF))
            {
                ConcreteFunctionDefinition<?> function = findFunction(value.substring(READ_BODY_OF.length()), context);
                if (function != null)
                {
                    prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.FUNCTION, PackageableElement.getUserPathForPackageableElement(function)));
                }
            }
        });
    }

    public static final String CONTEXT_FREE_HOOK_MARKER = "test::contextFreeHookWasUsed";

    @Override
    public Map<String, Procedure2<Object, Set<PackageableElementPointer>>> getExtraClassInstancePrerequisiteElementsPassProcessors()
    {
        return Maps.mutable.with("Test", (obj, prerequisiteElements) -> prerequisiteElements.add(new PackageableElementPointer(null, CONTEXT_FREE_HOOK_MARKER)));
    }

    private static ConcreteFunctionDefinition<?> findFunction(String functionName, CompileContext context)
    {
        return context.pureModel.getAllPackageableElementsOfType(ConcreteFunctionDefinition.class).detect(f -> functionName.equals(f._functionName()));
    }
}
