package org.finos.legend.engine.external.format.json;

import org.finos.legend.engine.external.format.json.read.IJsonDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.shared.ExternalFormatJavaCompilerExtension;
import org.finos.legend.engine.plan.compilation.GeneratePureConfig;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionPlanJavaCompilerExtension;
import org.finos.legend.engine.shared.javaCompiler.ClassListFilter;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilter;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonJavaCompilerExtension implements ExecutionPlanJavaCompilerExtension
{
    private static final String PURE_PACKAGE = "meta::external::format::json::executionPlan::engine::";
    private static final Map<String, Class<?>> DEPENDENCIES = new LinkedHashMap<>();

    static
    {
        DEPENDENCIES.put(PURE_PACKAGE + "_IJsonDeserializeExecutionNodeSpecifics", IJsonDeserializeExecutionNodeSpecifics.class);
    }

    @Override
    public ClassPathFilter getExtraClassPathFilter()
    {
        return new ClassListFilter(DEPENDENCIES.values());
    }

    /**
     *  Use to generate content of core_external_format_json/executionPlan/engine.pure (see GeneratePureConfig)
     */
    public static void main(String[] args)
    {
        GeneratePureConfig extension = new GeneratePureConfig("externalFormatJson", ExternalFormatJavaCompilerExtension.class, PURE_PACKAGE);
        DEPENDENCIES.forEach(extension::addClass);
        System.out.println(extension.generate());
    }

}
