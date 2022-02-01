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

package org.finos.legend.engine.plan.execution.nodes.helpers.freemarker;

import freemarker.core.TemplateDateFormatFactory;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.ResultNormalizer;
import org.finos.legend.engine.plan.execution.result.freemarker.PlanDateParameterDateFormatFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class FreeMarkerExecutor
{
    public static String process(String input, ExecutionState executionState)
    {
        return process(input, executionState, null, null);
    }

    public static String process(String input, ExecutionState executionState, String dbType)
    {
        return process(input, executionState, dbType, null);
    }

    public static String process(String input, ExecutionState executionState, String dbType, String databaseTimeZone)
    {
        Map<String, Object> variableMap = new HashMap<>();
        executionState.getResults().forEach((k, v) ->
        {
            if (v instanceof ConstantResult)
            {
                Object value = ((ConstantResult) v).getValue();
                value = dbType != null ?
                        (databaseTimeZone != null ?
                                ResultNormalizer.normalizeToSql(value, databaseTimeZone) :
                                ResultNormalizer.normalizeToSql(value)) :
                        value;
                variableMap.put(k, value);
            }
        });
        String templateFunctions = String.join("", executionState.getTemplateFunctions());
        variableMap.put("instanceOf", new FreemarkerInstanceOfMethod());
        return processRecursively(input, variableMap, templateFunctions);
    }

    public static String processRecursively(String input, Map<String, ?> variableMap, String templateFunctions)
    {
        String result = process(input, variableMap, templateFunctions);
        if (!result.equals(input.replace("\\\"", "\"")))
        {
            return processRecursively(result, variableMap, templateFunctions);
        }
        return result;
    }

    private static String process(String input, Map<String, ?> variableMap, String templateFunctions)
    {
        StringWriter stringWriter = new StringWriter();
        try
        {
            Configuration cfg = new Configuration();
            cfg.setNumberFormat("computer");
            Template template = new Template("template", new StringReader(templateFunctions + input.replace("\\\"", "\"")), cfg);
            Map<String, TemplateDateFormatFactory> customDateFormats = Maps.mutable.with("alloyDate", PlanDateParameterDateFormatFactory.INSTANCE);
            template.setCustomDateFormats(customDateFormats);
            template.setDateFormat("@alloyDate");
            template.process(variableMap, stringWriter);
            return stringWriter.toString();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Issue processing freemarker function.  Template with error: " + stringWriter.toString(), e);
        }
    }
}
