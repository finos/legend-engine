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

package org.finos.legend.engine.runtime.java.extension.external.json.natives;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.Test;

public abstract class AbstractTestParseJson extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testParseJsonSuccess()
    {
        compileTestSource(
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   let s = '{\"name\":\"some name\",\"nested\":{\"nestedName\":\"some other name\",\"amount\":-12.3,\"furtherNested\":{\"integer\":0,\"description\":\"nested object\"}},\"list\":[{},1,2,3.14159,false,\"the quick brown fox\",null]}';\n" +
                        "   assert(!meta::json::parseJSON($s)->isEmpty(), |'');\n" +
                        "}");
        execute("test():Boolean[1]");
    }
}
