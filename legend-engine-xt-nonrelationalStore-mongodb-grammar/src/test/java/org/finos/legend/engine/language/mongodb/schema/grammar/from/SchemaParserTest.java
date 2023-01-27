// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.mongodb.schema.grammar.from;

import org.finos.legend.engine.language.mongodb.schema.grammar.to.MongodbSchemaGrammarComposer;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MongoDatabase;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class SchemaParserTest
{

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Tests");


    @Test
    public void testSimpleCollectionSchema() throws Exception
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource("json/schema/schema_def_1.json"));
        String payload = new String(Files.readAllBytes(Paths.get(url.toURI())));
        check(payload);
    }


    @Test
    public void testComplexCollectionSchema() throws Exception
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource("json/schema/schema_def_2.json"));
        String payload = new String(Files.readAllBytes(Paths.get(url.toURI())));
        check(payload);
    }

    protected void check(String value)
    {
        MongodbSchemaGrammarParser parser = MongodbSchemaGrammarParser.newInstance();
        MongoDatabase database = parser.parseDocument(value);
        MongodbSchemaGrammarComposer composer = MongodbSchemaGrammarComposer.newInstance();
        String composedString = composer.renderDocument(database);
        Assert.assertEquals("Round-trip value should match", value, composedString);
        Assert.assertNotNull(database);
    }
}
