//  Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.query.sql.code.compiled;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.pure.runtime.compiler.shared.LegendCompile;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

import java.io.IOException;
import java.io.UncheckedIOException;

public class LegendCompileVSProtocol extends AbstractNative
{
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    public LegendCompileVSProtocol()
    {
        super("legendCompileVSProtocol_String_1__String_$0_1$__ValueSpecification_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        final ProcessorSupport processorSupport = processorContext.getSupport();
        final ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);

        String json = ValueSpecificationProcessor.processValueSpecification(topLevelElement, parametersValues.get(0), processorContext);
        String base = ValueSpecificationProcessor.processValueSpecification(topLevelElement, parametersValues.get(1), processorContext);

        return "org.finos.legend.engine.query.sql.code.compiled.LegendCompileVSProtocol.compileExec(" + json + ", " + base + ", es)";
    }

    @Override
    public String buildBody()
    {
        return "new SharedPureFunction<Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object execute(ListIterable<?> vars, final ExecutionSupport es)\n" +
                "            {\n" +
                "                return org.finos.legend.engine.query.sql.code.compiled.LegendCompileVSProtocol.compileExec((String) vars.get(0), (String) vars.get(1), es);\n" +
                "            }\n" +
                "        }";
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification compileExec(String code, String base, ExecutionSupport es)
    {
        ValueSpecification vs;
        PureModelContextData pmcd;
        try
        {
            vs = OBJECT_MAPPER.readValue(code, ValueSpecification.class);
            pmcd = base != null ? OBJECT_MAPPER.readValue(base, PureModelContextData.class) : PureModelContextData.newPureModelContextData();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        return LegendCompile.doCompileVS(vs, pmcd, ((CompiledExecutionSupport) es).getProcessorSupport().getMetadata());
    }
}
