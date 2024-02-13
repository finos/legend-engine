
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


package org.finos.legend.engine.language.hostedService.grammar.test;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestHostedServiceRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testHostedServiceDefinitionWithUserList()
    {
        test("###HostedService\n" +
                "HostedService <<a::A.test>> {a::A.val = 'ok'} package1::MyHostedService\n" +
                "{\n" +
                "   pattern : '/a/b/{integer1}';\n" +
                "   ownership : UserList { users: [\n" +
                "    'user1'\n" +
                "    ] };\n" +
                "   function : zxx(Integer[1]):String[1];\n" +
                "   documentation : 'My hosted service';\n" +
                "   autoActivateUpdates : true;\n" +
                "}\n");
    }

    @Test
    public void testHostedServiceDefinitionWithDeploymentId()
    {
        test("###HostedService\n" +
                "HostedService <<a::A.test>> {a::A.val = 'ok'} package1::MyHostedService\n" +
                "{\n" +
                "   pattern : '/a/b/{integer1}';\n" +
                "   ownership : Deployment { identifier: '17' };\n" +
                "   function : zxx(Integer[1]):String[1];\n" +
                "   documentation : 'My hosted service';\n" +
                "   autoActivateUpdates : true;\n" +
                "}\n");
    }

}
