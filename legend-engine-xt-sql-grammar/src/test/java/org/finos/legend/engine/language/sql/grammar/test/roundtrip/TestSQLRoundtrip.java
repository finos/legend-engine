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

package org.finos.legend.engine.language.sql.grammar.test.roundtrip;

import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.language.sql.grammar.from.SQLParserException;
import org.finos.legend.engine.language.sql.grammar.to.SQLGrammarComposer;
import org.finos.legend.engine.protocol.sql.metamodel.Node;
import org.hamcrest.MatcherAssert;
import org.hamcrest.text.IsEqualIgnoringCase;
import org.junit.Assert;
import org.junit.Test;

public class TestSQLRoundtrip
{
    @Test
    public void testSelectAllRoundTrip()
    {
        check("SELECT * FROM alloy.\"table\"");
    }

    @Test
    public void testEmptyStatement()
    {
        try
        {
            SQLGrammarParser parser = SQLGrammarParser.newInstance();
            parser.parseStatement("");
            Assert.fail();
        }
        catch (SQLParserException e)
        {
            Assert.assertEquals(1, e.getSourceInformation().startColumn);
            Assert.assertEquals(1, e.getSourceInformation().startLine);
            Assert.assertEquals("Unexpected token", e.getMessage());
        }
    }

    @Test
    public void testStringTableName()
    {
        try
        {
            SQLGrammarParser parser = SQLGrammarParser.newInstance();
            parser.parseStatement("SELECT * FROM 'table'");
            Assert.fail();
        }
        catch (SQLParserException e)
        {
            Assert.assertEquals(15, e.getSourceInformation().startColumn);
            Assert.assertEquals(1, e.getSourceInformation().startLine);
            Assert.assertEquals("no viable alternative at input 'SELECT * FROM 'table''", e.getMessage());
        }
    }

    protected void check(String value)
    {
        SQLGrammarParser parser = SQLGrammarParser.newInstance();
        Node node = parser.parseStatement(value);
        SQLGrammarComposer composer = SQLGrammarComposer.newInstance();
        String result = composer.renderNode(node);
        MatcherAssert.assertThat(result.trim(), IsEqualIgnoringCase.equalToIgnoringCase(value));
    }
}
