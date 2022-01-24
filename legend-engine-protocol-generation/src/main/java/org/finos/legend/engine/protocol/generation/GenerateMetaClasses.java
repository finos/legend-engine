package org.finos.legend.engine.protocol.generation;

import org.finos.legend.engine.external.language.java.generation.GenerateJavaProject;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.generated.core_pure_protocol_generation_java_generation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

public class GenerateMetaClasses extends GenerateJavaProject
{
    public static void main(String[] args)
    {
        new GenerateMetaClasses(args[0], args[1], args[2]).execute();
    }

    private final String fromPurePackage;
    private final String toJavaPackage;

    protected GenerateMetaClasses(String fromPurePackage, String toJavaPackage, String outputDirectory)
    {
        super(outputDirectory);
        this.fromPurePackage = fromPurePackage;
        this.toJavaPackage = toJavaPackage;
    }

    @Override
    protected Root_meta_external_language_java_metamodel_project_Project doExecute(CompiledExecutionSupport executionSupport)
    {
        return core_pure_protocol_generation_java_generation.Root_meta_protocols_generation_java_generateProtocolClasses_String_1__String_1__Project_1_(fromPurePackage, toJavaPackage, executionSupport);
    }
}
