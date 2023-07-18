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

package org.finos.legend.engine.language.pure.grammar.from;

import org.finos.legend.engine.language.pure.grammar.to.MongoDBSchemaJsonComposer;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDatabase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

@RunWith(Parameterized.class)
public class TestSchemaParser
{

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TestSchemaParser.class);
    private final String inputJsonFile;

    public TestSchemaParser(String inputQueryFile)
    {
        inputJsonFile = inputQueryFile;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][]{
                {"json/schema/schema_def_1.json"},
                {"json/schema/schema_def_2.json"},
        });
    }

    @Test
    public void testValidSchemaFile() throws Exception
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource(this.inputJsonFile));
        String inputSchema = new String(Files.readAllBytes(Paths.get(url.toURI())), StandardCharsets.UTF_8);

        MongoDBSchemaParser parser = MongoDBSchemaParser.newInstance();
        MongoDatabase dbSchema = parser.parseDocument(inputSchema);
        // Pretty print input to canonical json format for test assertions
        String expected = parser.getObjectMapper().readTree(inputSchema).toPrettyString();

        //Compose back to String
        MongoDBSchemaJsonComposer schemaComposer = MongoDBSchemaJsonComposer.newInstance();
        String schemaComposerOutput = schemaComposer.renderDocument(dbSchema);

        // Pretty print output from composoer for test assertions
        String actual = parser.getObjectMapper().readTree(schemaComposerOutput).toPrettyString();

        Assert.assertEquals(expected, actual);
    }

}
