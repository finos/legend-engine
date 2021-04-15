// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.utils;

import org.finos.legend.engine.plan.execution.stores.relational.utils.DynamicPortGenerator;
import org.junit.Assert;
import org.junit.Test;

public class TestDynamicPortGenerator
{
    @Test
    public void generatePort() {
        int port = DynamicPortGenerator.generatePort();

        Assert.assertTrue(port >= DynamicPortGenerator.MIN_TEST_PORT);
        Assert.assertTrue(port <= DynamicPortGenerator.MAX_TEST_PORT);
    }
}