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
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.ResultNormalizer;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.freemarker.PlanDateParameterDateFormatFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class FreeMarkerExecutor
{
    private static Configuration freemarkerConfig = new Configuration();
    private static Map<String, TemplateDateFormatFactory> customDateFormats = Maps.mutable.with("alloyDate", PlanDateParameterDateFormatFactory.INSTANCE);
    public static Pattern pattern = Pattern.compile("(\\$)[\\{](?:[^\\{\\}]+|[\\{][^\\{\\}]*[\\}])*[\\}]");
    private static ObjectWrapper objectWrapper = FreeMarkerExecutor.freemarkerConfig.getObjectWrapper();
    public static String overridePropertyForTemplateModel = "overrideTemplateModel";

    static
    {
        freemarkerConfig.setNumberFormat("computer");
    }

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
                if (value != null)
                {
                    value = dbType != null ?
                            (databaseTimeZone != null ?
                                    ResultNormalizer.normalizeToSql(value, databaseTimeZone) :
                                    ResultNormalizer.normalizeToSql(value)) :
                            value;
                    variableMap.put(k, value);
                }
            }
            else if (v instanceof StreamingResult)
            {
                variableMap.put(k, v);  // This is here for evaluating conditional checks only and should not be actually processed to extract value
            }
        });
        String templateFunctions = String.join("", executionState.getTemplateFunctions());
        variableMap.put("instanceOf", new FreemarkerInstanceOfMethod());

        return StringUtils.isBlank(templateFunctions) ? process(input, variableMap, templateFunctions) : processRecursively(input, variableMap, templateFunctions);
    }
    
    private static boolean isPlaceHolder(Object object)
    {
        return object instanceof String && pattern.matcher((String)object).find();
    }

    private static class TemplateHashModelOverride implements TemplateHashModel
    {
        private Map<String,?> map;
        private String templateFunctions;

        TemplateHashModelOverride(Map<String,?> map, String templateFunctions)
        {
            this.map = map;
            this.templateFunctions = templateFunctions;
        }

        @Override
        public TemplateModel get(String s) throws TemplateModelException
        {
            Object result = map.get(s);
            boolean isResultUnchanged = false;
            while (!isResultUnchanged && isPlaceHolder(result))
            {
                String lastProcessedResult = (String) result;
                result = process(lastProcessedResult, map, templateFunctions);
                isResultUnchanged = lastProcessedResult.equals(((String) result).replace("\\\"", "\""));
            }
            return objectWrapper.wrap(result);
        }

        @Override
        public boolean isEmpty()
        {
            return map.isEmpty();
        }
    }

    public static String processRecursively(String input, Map<String, ?> variableMap, String templateFunctions)
    {
        boolean overrideTemplateModelFlag = Boolean.valueOf(System.getProperty(overridePropertyForTemplateModel));
        if (!overrideTemplateModelFlag)
        {
            return process(input, new TemplateHashModelOverride(variableMap, templateFunctions), templateFunctions);
        }
        else
        {
            return recur(input, variableMap, templateFunctions);
        }
    }

    private static String recur(String input, Map<String,?> variableMap, String templateFunctions)
    {
        String result = process(input, variableMap, templateFunctions);
        if (!result.equals(input.replace("\\\"", "\"")))
        {
            return recur(result, variableMap, templateFunctions);
        }
        return result;
    }

    private static String process(String input, Map<String, ?> variableMap, String templateFunctions)
    {
        return process(input, new TemplateHashModelOverride(variableMap, templateFunctions), templateFunctions);
    }

    private static String process(String input, TemplateHashModelOverride variableMap, String templateFunctions)
    {
        StringWriter stringWriter = new StringWriter();
        try
        {
            Template template = new Template("template", new StringReader(templateFunctions + input.replace("\\\"", "\"")), freemarkerConfig);
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
