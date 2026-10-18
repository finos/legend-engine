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

package org.finos.legend.engine.pure.runtime.compiler.compiled.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.generated.CoreGen;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;

public class LegendCompileWithFunction extends AbstractNative
{
    private final String implementation;

    private LegendCompileWithFunction(String signature, String implementation)
    {
        super(signature);
        this.implementation = "org.finos.legend.engine.pure.runtime.compiler.compiled.natives.LegendCompileWithFunction." + implementation;
    }

    public static LegendCompileWithFunction grammar()
    {
        return new LegendCompileWithFunction("compile_String_1__Function_1__T_m_", "compileExec");
    }

    public static LegendCompileWithFunction pmcd()
    {
        return new LegendCompileWithFunction("compilePMCD_String_1__Function_1__T_m_", "compilePMCDExec");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport);
        String type = TypeProcessor.typeToJavaObjectWithMul(Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.genericType, processorSupport), multiplicity, processorSupport);
        String call = this.implementation + "(" + transformedParams.get(0) + ", (" + FullJavaPaths.Function + "<?>)" + transformedParams.get(1) + ", es)";
        return "((" + type + ")(Object)" + (Multiplicity.isToOne(multiplicity, false) ? "CompiledSupport.makeOne(" + call + ")" : "CompiledSupport.toPureCollection(" + call + ")") + ")";
    }

    @Override
    public String buildBody()
    {
        return "new SharedPureFunction<Object>()\n" +
                "{\n" +
                "    @Override\n" +
                "    public Object execute(ListIterable<?> vars, final ExecutionSupport es)\n" +
                "    {\n" +
                "        Object value = " + this.implementation + "((String) vars.get(0), (" + FullJavaPaths.Function + "<?>) vars.get(1), es);\n" +
                "        return value instanceof Iterable ? CompiledSupport.toPureCollection(value) : value;\n" +
                "    }\n" +
                "}";
    }

    public static Object compileExec(String grammar, Function<?> f, ExecutionSupport es)
    {
        return evaluateInLayer(org.finos.legend.engine.pure.runtime.compiler.shared.LegendCompile.compileLayer(grammar, callingMetadata(es)), f, es);
    }

    public static Object compilePMCDExec(String pmcdJSON, Function<?> f, ExecutionSupport es)
    {
        return evaluateInLayer(org.finos.legend.engine.pure.runtime.compiler.shared.LegendCompile.compilePMCDLayer(pmcdJSON, callingMetadata(es)), f, es);
    }

    private static Metadata callingMetadata(ExecutionSupport es)
    {
        return ((CompiledExecutionSupport) es).getProcessorSupport().getMetadata();
    }

    private static Object evaluateInLayer(Metadata layer, Function<?> f, ExecutionSupport es)
    {
        return CoreGen.evaluate(new LayerExecutionSupport((CompiledExecutionSupport) es, layer), f);
    }
}
