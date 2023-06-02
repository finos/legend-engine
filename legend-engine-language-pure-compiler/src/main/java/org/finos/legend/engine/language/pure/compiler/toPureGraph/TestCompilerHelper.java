// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.assertion.TestAssertionFirstPassBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingDataTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingFunctionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingDataTest_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingQueryTest;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingQueryTest_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingTest;
import org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test;

public class TestCompilerHelper
{
    public static Test compilePureMappingTests(org.finos.legend.engine.protocol.pure.v1.model.test.Test test, CompileContext context, ProcessingContext processingContext)
    {
        if (test instanceof MappingTest)
        {
            MappingTest mappingTest = (MappingTest) test;
            // pre checks
            if (mappingTest.assertions == null || mappingTest.assertions.isEmpty())
            {
                throw new EngineException("Mapping Tests should have at least 1 assert", mappingTest.sourceInformation, EngineErrorType.COMPILATION);
            }
            Root_meta_pure_mapping_metamodel_MappingTest compiledMappingTest;
            if (mappingTest instanceof MappingFunctionTest)
            {
                MappingFunctionTest mappingFunctionTest = (MappingFunctionTest) mappingTest;
                Root_meta_pure_mapping_metamodel_MappingQueryTest cQueryMappingTest = new Root_meta_pure_mapping_metamodel_MappingQueryTest_Impl("");
                cQueryMappingTest._query(HelperValueSpecificationBuilder.buildLambda(mappingFunctionTest.func, context));
                compiledMappingTest = cQueryMappingTest;
            }
            else if (mappingTest instanceof MappingDataTest)
            {
                MappingDataTest mappingDataTest = (MappingDataTest) mappingTest;
                Root_meta_pure_mapping_metamodel_MappingDataTest_Impl compiledQueryTest = new Root_meta_pure_mapping_metamodel_MappingDataTest_Impl("");
                compiledQueryTest
                    ._storeTestData(ListIterate.collect(mappingDataTest.storeTestData, ele -> HelperMappingBuilder.processMappingElementTestData(ele, context, new ProcessingContext("Mapping Element: "))));
                compiledMappingTest = compiledQueryTest;
            }
            else
            {
                throw new EngineException("Unsupported Mapping Test", test.sourceInformation, EngineErrorType.COMPILATION);
            }
            compiledMappingTest._id(mappingTest.id);
            List<String> assertionIds = ListIterate.collect(mappingTest.assertions, a -> a.id);
            List<String> duplicateAssertionIds = assertionIds.stream().filter(e -> Collections.frequency(assertionIds, e) > 1).distinct().collect(Collectors.toList());
            if (!duplicateAssertionIds.isEmpty())
            {
                throw new EngineException("Multiple assertions found with ids : '" + String.join(",", duplicateAssertionIds) + "'", mappingTest.sourceInformation, EngineErrorType.COMPILATION);
            }
            compiledMappingTest._assertions(ListIterate.collect(mappingTest.assertions, assertion -> assertion.accept(new TestAssertionFirstPassBuilder(context, processingContext))));
            return compiledMappingTest;
        }
        throw new EngineException("Tests in mapping must be of type MappingTest", test.sourceInformation, EngineErrorType.COMPILATION);
    }
}