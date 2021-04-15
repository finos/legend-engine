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

package org.finos.legend.engine.protocol.pure.v1.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;

public class TestLoadRelationalModelAndMapping
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    @Test
    public void selfJoinTest() throws Exception 
    {
        PureModelContextData context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("selfJoinTest.json")), PureModelContextData.class);
        PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build());
        String formatted = grammarTransformer.renderPureModelContextData(context);
        
        // Test checks that we do not output schema name before {target} - the grammar round trip tests do not exercise this code path as the graph
        // produced for the roundtrip always sets the schema name in the table alias to default. 
        // This is testing out a graph as would be produced by tools such as Studio and checking that syntactically valid Pure is produced for self-joins
        String expected = 
                "###Relational\n" +  
                "Database test::db\n" + "(\n" + 
                "  Schema mySchema\n" +
                "  (\n" + 
                "    Table table1\n"+
                "    (\n"+
                "      col1 CHAR(32) PRIMARY KEY,\n"+
                "      col2 CHAR(32)\n"+
                "    )\n"+
                "  )\n"+
                "\n"+ 
                "  Join selfJoin(mySchema.table1.col1 = {target}.col2)\n" +
                ")\n";

        Assert.assertEquals(expected, formatted);
    }
}
