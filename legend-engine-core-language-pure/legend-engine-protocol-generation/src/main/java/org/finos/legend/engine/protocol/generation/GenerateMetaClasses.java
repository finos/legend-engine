//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.protocol.generation;

import org.finos.legend.engine.external.language.java.generation.GenerateJavaProject;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.generated.core_external_language_java_protocol_generation_generation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

public class GenerateMetaClasses extends GenerateJavaProject
{
    public static void main(String[] args) throws Exception
    {
        new GenerateMetaClasses(GenerationArgument.fromArgs(args)).execute();
    }

    private final GenerationArgument argument;

    protected GenerateMetaClasses(GenerationArgument argument)
    {
        super(argument.outputDirectory);
        this.argument = argument;
    }

    @Override
    protected Root_meta_external_language_java_metamodel_project_Project doExecute(CompiledExecutionSupport executionSupport)
    {
        return core_external_language_java_protocol_generation_generation.Root_meta_protocols_generation_java_generateProtocolClasses_String_1__String_MANY__Project_1_(argument.configJson, argument.dependencies, executionSupport);
    }
}
