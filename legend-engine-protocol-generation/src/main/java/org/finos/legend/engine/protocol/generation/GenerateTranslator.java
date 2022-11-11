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

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.language.java.generation.GenerateJavaProject;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.generated.core_pure_protocol_generation_java_generation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GenerateTranslator extends GenerateJavaProject
{
    public static void main(String[] args)
    {
        List<String> elementsToBeExcluded = args.length == 4 ? Arrays.asList(args[3].split(",")) : Collections.emptyList();
        new GenerateTranslator(args[0], args[1], args[2], elementsToBeExcluded).execute();
    }

    private final String fromPurePackage;
    private final String toJavaPackage;
    private final List<String> elementsToBeExcluded;

    protected GenerateTranslator(String fromPurePackage, String toJavaPackage, String outputDirectory, List<String> elementsToBeExcluded)
    {
        super(outputDirectory);
        this.fromPurePackage = fromPurePackage;
        this.toJavaPackage = toJavaPackage;
        this.elementsToBeExcluded = elementsToBeExcluded;
    }

    @Override
    protected Root_meta_external_language_java_metamodel_project_Project doExecute(CompiledExecutionSupport executionSupport)
    {
        return core_pure_protocol_generation_java_generation.Root_meta_protocols_generation_java_translation_generateTranslation_String_1__String_1__String_MANY__Project_1_(fromPurePackage, toJavaPackage, Lists.mutable.withAll(elementsToBeExcluded), executionSupport);
    }
}
