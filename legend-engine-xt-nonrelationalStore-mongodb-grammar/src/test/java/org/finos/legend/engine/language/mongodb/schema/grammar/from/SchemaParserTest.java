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

import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MongoDatabase;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class SchemaParserTest
{
    @Test
    public void testSingleCollectionSchema() throws Exception
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource("json/schema/schema_def_1.json"));
        String payload = new String(Files.readAllBytes(Paths.get(url.toURI())));
        check(payload);
    }


    protected void check(String value)
    {
        MongodbSchemaGrammarParser parser = MongodbSchemaGrammarParser.newInstance();
        MongoDatabase database = parser.parseDocument(value);
        Assert.assertNull(database);
    }
}
