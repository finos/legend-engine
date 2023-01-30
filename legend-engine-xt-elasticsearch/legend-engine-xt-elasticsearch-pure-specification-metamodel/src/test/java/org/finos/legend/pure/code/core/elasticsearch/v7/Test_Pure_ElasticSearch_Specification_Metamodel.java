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

package org.finos.legend.pure.code.core.elasticsearch.v7;

import junit.framework.TestSuite;
import org.finos.legend.pure.code.core.compiled.test.PureTestBuilderHelper;
import org.finos.legend.pure.generated.platform_pure_corefunctions_meta;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import java.util.regex.Pattern;

public class Test_Pure_ElasticSearch_Specification_Metamodel
{
    public static TestSuite suite()
    {
        CompiledExecutionSupport executionSupport = PureTestBuilderHelper.getClassLoaderExecutionSupport();
        TestSuite suite = new TestSuite();
        Pattern allowedPackagesPattern = GenericCodeRepository.build("core_elasticsearch_specification_metamodel.definition.json").getAllowedPackagesPattern();
        TestCollection testCollection = TestCollection.collectTests(
                "meta::external::store::elasticsearch",
                executionSupport.getProcessorSupport(),
                ci -> allowedPackagesPattern.matcher(platform_pure_corefunctions_meta.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_((PackageableElement) ci, executionSupport)).matches()
                            && PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())
        );
        suite.addTest(PureTestBuilderHelper.buildSuite(testCollection, executionSupport));
        return suite;
    }
}
