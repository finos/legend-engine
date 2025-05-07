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

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.assertion.TestAssertionFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.assertion.TestAssertionPrerequisiteElementsPassBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingTest;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingTest_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.testable.Test;

import java.util.Set;

public class TestCompilerHelper
{
    public static Test compilePureMappingTests(org.finos.legend.engine.protocol.pure.v1.model.test.Test test, CompileContext context, ProcessingContext processingContext)
    {
        if (test instanceof MappingTest)
        {
            MappingTest mappingTest = (MappingTest) test;
            // pre checks
            if (mappingTest.assertions == null || mappingTest.assertions.size() != 1)
            {
                throw new EngineException("Mapping Tests can only have one assertion", mappingTest.sourceInformation, EngineErrorType.COMPILATION);
            }

            Root_meta_pure_mapping_metamodel_MappingTest compiledTest = new Root_meta_pure_mapping_metamodel_MappingTest_Impl("", SourceInformationHelper.toM3SourceInformation(mappingTest.sourceInformation), context.pureModel.getClass("meta::pure::mapping::metamodel::MappingTest"));
            compiledTest
                    ._storeTestData(ListIterate.collect(mappingTest.storeTestData, ele -> HelperMappingBuilder.processMappingElementTestData(ele, context, new ProcessingContext("Mapping Element: "))))
                    ._id(mappingTest.id)
                    ._assertions(ListIterate.collect(mappingTest.assertions, assertion -> assertion.accept(new TestAssertionFirstPassBuilder(context, processingContext))));
            return compiledTest;
        }
        throw new EngineException("Tests in mapping must be of type MappingTest", test.sourceInformation, EngineErrorType.COMPILATION);
    }

    public static void collectPrerequisiteElementsFromPureMappingTests(Set<PackageableElementPointer> prerequisiteElements, org.finos.legend.engine.protocol.pure.v1.model.test.Test test, CompileContext context)
    {
        if (test instanceof MappingTest)
        {
            MappingTest mappingTest = (MappingTest) test;
            ListIterate.forEach(mappingTest.storeTestData, ele -> HelperMappingBuilder.collectPrerequisiteElementsFromMappingElementTestData(prerequisiteElements, ele, context));
            TestAssertionPrerequisiteElementsPassBuilder testAssertionPrerequisiteElementsPassBuilder = new TestAssertionPrerequisiteElementsPassBuilder(context, prerequisiteElements);
            ListIterate.forEach(mappingTest.assertions, assertion -> assertion.accept(testAssertionPrerequisiteElementsPassBuilder));
        }
    }
}