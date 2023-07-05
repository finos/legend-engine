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

package org.finos.legend.engine.language.pure.compiler.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_runtime_ServiceStoreConnection;
import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;
import static org.finos.legend.engine.language.pure.compiler.test.TestServiceStoreCompilationUtils.TEST_BINDING;

public class TestServiceStoreConnectionCompilationFromGrammar
{
    @Test
    public void testServiceStoreConnectionCompilation()
    {
        Pair<PureModelContextData, PureModel> compiledGraph = test(TEST_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::serviceStore\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "###Connection\n" +
                "ServiceStoreConnection simple::serviceStoreConnection\n" +
                "{\n" +
                "  store: test::serviceStore;\n" +
                "  baseUrl: 'http://baseUrl';\n" +
                "}\n");
        Root_meta_external_store_service_metamodel_runtime_ServiceStoreConnection connection = (Root_meta_external_store_service_metamodel_runtime_ServiceStoreConnection) compiledGraph.getTwo().getConnection("simple::serviceStoreConnection", SourceInformation.getUnknownSourceInformation());

        Assert.assertEquals("http://baseUrl", connection._baseUrl());
    }
}
