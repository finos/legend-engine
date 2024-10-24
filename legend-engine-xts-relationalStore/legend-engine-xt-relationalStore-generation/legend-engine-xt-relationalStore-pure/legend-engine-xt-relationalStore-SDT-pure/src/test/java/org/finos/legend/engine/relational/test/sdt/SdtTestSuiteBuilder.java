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
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.junit.Assert;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.getClassLoaderExecutionSupport;
import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;
import static org.finos.legend.pure.generated.core_external_store_relational_sdt_sdtFramework.*;
import static org.finos.legend.pure.generated.platform_pure_essential_meta_graph_elementToPath.*;

public class SdtTestSuiteBuilder
{
    private static final Set<String> SDT_TEST_PACKAGES = Sets.fixedSize.of(
            "meta::external::store::relational::sdt::suite"
    );

    public static Test buildSdtTestSuite(String dbType, Function<CompiledExecutionSupport, RichIterable<? extends Root_meta_pure_extension_Extension>> extensionsFunc, Map<String, String> expectedErrors)
    {
        final CompiledExecutionSupport es = getClassLoaderExecutionSupport();
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = extensionsFunc.apply(es);
        TestSuite suite = new TestSuite();
        SDT_TEST_PACKAGES.forEach(pkg ->
        {
            RichIterable<? extends ConcreteFunctionDefinition<?>> sdtTestInPackage = Root_meta_external_store_relational_sdt_framework_collectSDTTestsInPackage_String_1__ConcreteFunctionDefinition_MANY_(pkg, es);
            sdtTestInPackage.forEach(x -> suite.addTest(new SDTTestCase(x, dbType, extensions, es, null)));
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
        ConcreteFunctionDefinition<?> func;
        String dbType;
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions;
        CompiledExecutionSupport es;
        String expectedError;

        public SDTTestCase()
        {
        }

        public SDTTestCase(ConcreteFunctionDefinition<?> func, String dbType, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, CompiledExecutionSupport es, String expectedError)
        {
            super(Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_(func, es));
            this.dbType = dbType;
            this.extensions = extensions;
            this.func = func;
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
                Root_meta_external_store_relational_sdt_framework_runSqlDialect_SqlDialectTest_1__String_1__Extension_MANY__Boolean_1_(
                        Root_meta_external_store_relational_sdt_framework_getSqlDialectTest_ConcreteFunctionDefinition_1__SqlDialectTest_1_(this.func, this.es),
                        this.dbType,
                        this.extensions,
                        this.es
                );
                testPass = true;
            }
            catch (Exception e)
            {
                if (expectedError != null)
                {
                    Assert.assertEquals(expectedError, e.getMessage());
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
