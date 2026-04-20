// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.h2.semistructured;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;
import java.util.Set;
import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;
import org.eclipse.collections.api.factory.Maps;

// TODO move to a better module!
public class Test_Relational_H2_Semistructured
{
    // Skip every test whose Pure user-path lives under one of these packages.
    // Cheaper to maintain than listing each function individually — entries here are
    // matched as `<package>::` prefixes so a sibling package can't accidentally match.
    private static final Set<String> SKIPPED_PACKAGES = Sets.mutable.with(
            "meta::relational::tests::semistructured::flattening",
            "meta::relational::tests::mapping::relation"
    );

    // One-off skips that aren't worth (or can't be) skipped by package. Empty by default.
    private static final Set<String> SKIPPED_TESTS = Sets.mutable.empty();

    public static Test suite()
    {
        String testPackage = "meta::relational::tests::pct::h2::semistructured";
        CompiledExecutionSupport executionSupport = PureTestBuilderCompiled.getClassLoaderExecutionSupport();

        Set<CoreInstance> skippedCoreInstances = Sets.mutable.empty();
        for (String path : SKIPPED_TESTS)
        {
            CoreInstance ci = executionSupport.getProcessorSupport().package_getByUserPath(path);
            if (ci != null)
            {
                skippedCoreInstances.add(ci);
            }
        }

        // Pre-compute prefixes once (with trailing "::") so the executor only does a startsWith per test.
        String[] skippedPackagePrefixes = SKIPPED_PACKAGES.stream()
                .map(p -> p + "::")
                .toArray(String[]::new);

        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> executor = (test, params) ->
        {
            if (skippedCoreInstances.contains(test))
            {
                return true; // skip
            }
            if (skippedPackagePrefixes.length > 0)
            {
                String testPath = PackageableElement.getUserPathForPackageableElement(test);
                for (String prefix : skippedPackagePrefixes)
                {
                    if (testPath.startsWith(prefix))
                    {
                        return true; // skip — covered by a SKIPPED_PACKAGES entry
                    }
                }
            }
            return PureTestBuilderCompiled.executeFn(test, null, Maps.mutable.empty(), executionSupport, params);
        };

        return wrapSuite(
                () -> true,
                () -> PureTestBuilder.buildSuite(
                        TestCollection.collectTests(testPackage, executionSupport.getProcessorSupport(),
                                fn -> PureTestBuilderCompiled.generatePureTestCollection(fn, executionSupport),
                                ci -> PureTestBuilder.satisfiesConditions(ci, executionSupport.getProcessorSupport())
                        ),
                        executor,
                        executionSupport
                ),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.H2).getFirst())
        );
    }
}
