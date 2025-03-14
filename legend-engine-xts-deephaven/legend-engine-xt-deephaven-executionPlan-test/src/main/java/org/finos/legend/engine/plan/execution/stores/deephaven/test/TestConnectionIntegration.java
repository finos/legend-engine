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

package org.finos.legend.engine.plan.execution.stores.deephaven.test;

import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.engine.shared.core.extension.LegendConnectionExtension;

public interface TestConnectionIntegration extends LegendConnectionExtension
{
    @Override
    default String type()
    {
        return "Test_Connection";
    }

    void setup() throws Exception;

    DeephavenConnection getConnection() throws Exception;

    void cleanup() throws Exception;
}
