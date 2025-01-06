// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.relational.test.sdt;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.junit.Assert;

import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.getClassLoaderExecutionSupport;
import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;
import static org.finos.legend.pure.generated.core_external_store_relational_sdt_sdtFramework.*;

public class SdtTestSuiteBuilder
{
    private static final Set<String> SDT_TEST_PACKAGES = Sets.fixedSize.of(
            "meta::external::store::relational::sdt::suite"
    );

    public static Test buildSdtTestSuite(String dbType, Function<CompiledExecutionSupport, RichIterable<? extends Root_meta_pure_extension_Extension>> extensionsFunc, Function<CompiledExecutionSupport, PureMap> expectedErrorsFunc)
    {
        final CompiledExecutionSupport es = getClassLoaderExecutionSupport();
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = extensionsFunc.apply(es);
        PureMap expectedErrors = expectedErrorsFunc.apply(es);
        TestSuite suite = new TestSuite();
        SDT_TEST_PACKAGES.forEach(pkg ->
        {
            RichIterable<? extends ConcreteFunctionDefinition<?>> sdtTestInPackage = Root_meta_external_store_relational_sdt_framework_collectSDTTestsInPackage_String_1__ConcreteFunctionDefinition_MANY_(pkg, es);
            sdtTestInPackage.forEach(x ->
            {
                MutableList<? extends Root_meta_external_store_relational_sdt_framework_SqlDialectTest> sdtTests = Root_meta_external_store_relational_sdt_framework_getSqlDialectTests_ConcreteFunctionDefinition_1__SqlDialectTest_MANY_(x, es).toList();
                sdtTests.forEach(test -> suite.addTest(new SDTTestCase(test, dbType, extensions, es, (String) expectedErrors.getMap().get(test._identifier()))));
            });
        });
        return wrapSuite(
                () -> true,
                () -> suite,
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.valueOf(dbType)).getFirst())
        );
    }

    public static final class SDTTestCase extends TestCase
    {
        Root_meta_external_store_relational_sdt_framework_SqlDialectTest sdtTest;
        String dbType;
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions;
        CompiledExecutionSupport es;
        String expectedError;

        public SDTTestCase()
        {
        }

        public SDTTestCase(Root_meta_external_store_relational_sdt_framework_SqlDialectTest sdtTest, String dbType, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, CompiledExecutionSupport es, String expectedError)
        {
            super(sdtTest._identifier());
            this.sdtTest = sdtTest;
            this.dbType = dbType;
            this.extensions = extensions;
            this.es = es;
            this.expectedError = expectedError;
        }

        @Override
        protected void runTest() throws Throwable
        {
            System.out.print("EXECUTING " + this.getName() + " ... ");
            long start = System.nanoTime();

            boolean testPass = false;
            try
            {
                Root_meta_external_store_relational_sdt_framework_runSqlDialectTest_SqlDialectTest_1__String_1__Extension_MANY__DebugContext_1__Boolean_1_(
                        sdtTest,
                        this.dbType,
                        this.extensions,
                        new Root_meta_pure_tools_DebugContext_Impl("")._debug(false),
                        this.es
                );
                testPass = true;
            }
            catch (Exception e)
            {
                if (expectedError != null)
                {
                    String errorMessage = e.getMessage();
                    Pattern p = Pattern.compile("Assert failure at \\((.*?)\\), (.*)", Pattern.DOTALL);
                    Matcher m = p.matcher(errorMessage);
                    if (m.matches())
                    {
                        // Check assert message
                        String assertMessage = m.group(2);
                        Assert.assertEquals(expectedError, assertMessage.startsWith("\"") ? assertMessage.substring(1, assertMessage.length() - 1) : assertMessage);
                    }
                    else
                    {
                        Assert.assertEquals(expectedError, e.getMessage());
                    }
                    testPass = true;
                    return;
                }
                throw e;
            }
            finally
            {
                System.out.format("%s (%.6fs)\n", (testPass ? "DONE" : "FAIL"), (System.nanoTime() - start) / 1_000_000_000.0);
            }
        }
    }
}
