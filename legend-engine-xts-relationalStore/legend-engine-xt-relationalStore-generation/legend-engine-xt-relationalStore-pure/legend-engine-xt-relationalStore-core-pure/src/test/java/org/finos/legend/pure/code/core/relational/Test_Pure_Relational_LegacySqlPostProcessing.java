// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.code.core.relational;

import junit.framework.TestSuite;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.serialization.runtime.RuntimeOptions;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_Pure_Relational_LegacySqlPostProcessing
{
    static final String LEGACY_TESTS_PACKAGE = "meta::relational::tests::legacySqlPostProcessing";
    private static final String LEGACY_SQL_POST_PROCESSING = "LegacySqlPostProcessing";

    public static TestSuite suite()
    {
        CompiledExecutionSupport executionSupport = withLegacySqlPostProcessing(PureTestBuilderCompiled.getClassLoaderExecutionSupport());
        executionSupport.getConsole().disable();
        TestSuite suite = new TestSuite();
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests(LEGACY_TESTS_PACKAGE, executionSupport.getProcessorSupport(), ci -> PureTestBuilder.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        return suite;
    }

    // The switch is read from the runtime options, which are fixed per execution support, so this suite gets an execution support
    // of its own rather than a JVM-wide system property that would leak into every other suite in the same JVM.
    private static CompiledExecutionSupport withLegacySqlPostProcessing(CompiledExecutionSupport executionSupport)
    {
        RuntimeOptions options = executionSupport.getRuntimeOptions();
        return new CompiledExecutionSupport(
                new JavaCompilerState(null, executionSupport.getClassLoader()),
                executionSupport.getProcessorSupport(),
                null,
                executionSupport.getCodeStorage(),
                null,
                null,
                new ConsoleCompiled(),
                null,
                null,
                null,
                Sets.mutable.empty(),
                CompiledExtensionLoader.extensions(),
                name -> LEGACY_SQL_POST_PROCESSING.equals(name) || options.isOptionSet(name));
    }
}
