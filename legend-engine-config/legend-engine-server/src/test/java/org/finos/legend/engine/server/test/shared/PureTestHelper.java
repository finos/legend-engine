// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.server.test.shared;

import junit.extensions.TestSetup;
import junit.framework.TestSuite;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.test.H2TestServerResource;
import org.finos.legend.engine.test.shared.framework.PureTestHelperFramework;
import org.junit.Ignore;

public class PureTestHelper
{
    @Ignore
    public static TestSetup wrapSuite(Function0<Boolean> init, Function0<TestSuite> suiteBuilder, Function0<Boolean> shutdown)
    {
        return PureTestHelperFramework.wrapSuite(
                init,
                suiteBuilder,
                shutdown,
                Lists.mutable.with(new H2TestServerResource(), new MetadataTestServerResource(), new ServerTestServerResource("org/finos/legend/engine/server/test/userTestConfig.json"))
        );
    }
}
