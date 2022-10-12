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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.test.core;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.assertion.TestAssertionFirstPassBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingTest;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingTest_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TestCompilerHelper
{
    public static Test compilePureMappingTests(org.finos.legend.engine.protocol.pure.v1.model.test.Test test, CompileContext context, ProcessingContext processingContext)
    {
        if (test instanceof MappingTest)
        {
            MappingTest mappingTest = (MappingTest) test;
            Root_meta_pure_mapping_metamodel_MappingTest pureMappingTest = new Root_meta_pure_mapping_metamodel_MappingTest_Impl("");
            pureMappingTest._query(HelperValueSpecificationBuilder.buildLambda(mappingTest.query, context));
            pureMappingTest._id(mappingTest.id);
            if (mappingTest.assertions == null || mappingTest.assertions.isEmpty())
            {
                throw new EngineException("Mapping Tests should have atleast 1 assert", mappingTest.sourceInformation, EngineErrorType.COMPILATION);
            }

            List<String> assertionIds = ListIterate.collect(mappingTest.assertions, a -> a.id);
            List<String> duplicateAssertionIds = assertionIds.stream().filter(e -> Collections.frequency(assertionIds, e) > 1).distinct().collect(Collectors.toList());

            if (!duplicateAssertionIds.isEmpty())
            {
                throw new EngineException("Multiple assertions found with ids : '" + String.join(",", duplicateAssertionIds) + "'", mappingTest.sourceInformation, EngineErrorType.COMPILATION);
            }
            pureMappingTest._assertions(ListIterate.collect(mappingTest.assertions, assertion -> assertion.accept(new TestAssertionFirstPassBuilder(context, processingContext))));
            return pureMappingTest;
        }
        else
        {
            return null;
        }
    }
}