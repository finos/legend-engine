package org.finos.legend.engine.protocol.generation;

import org.finos.legend.engine.external.language.java.generation.GenerateJavaProject;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.generated.core_pure_protocol_generation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

public class GenerateTranslator extends GenerateJavaProject
{
    public static void main(String[] args)
    {
        new GenerateTranslator(args[0], args[1], args[2]).execute();
    }

    private final String fromPurePackage;
    private final String toJavaPackage;

    protected GenerateTranslator(String fromPurePackage, String toJavaPackage, String outputDirectory)
    {
        super(outputDirectory);
        this.fromPurePackage = fromPurePackage;
        this.toJavaPackage = toJavaPackage;
    }

    @Override
    protected Root_meta_external_language_java_metamodel_project_Project doExecute(CompiledExecutionSupport executionSupport)
    {
        return core_pure_protocol_generation.Root_meta_protocols_generation_java_translation_generateTranslation_String_1__String_1__Project_1_(fromPurePackage, toJavaPackage, executionSupport);
    }
}
