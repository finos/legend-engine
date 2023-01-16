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

package org.finos.legend.engine.language.mongodb.schema.grammar.from;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDbQueryBaseListener;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDbQueryLexer;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDbQueryListener;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDbQueryParser;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.DatabaseCommand;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MongoDbQueryParseTreeWalkerTest
{

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    public void testEmptyAggregate() throws JsonProcessingException
    {
        String input = "{ aggregate: 'firms', pipeline: [  ], cursor: { } }";

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals("{\n" +
                "  \"type\" : \"aggregate\",\n" +
                "  \"collectionName\" : \"'firms'\",\n" +
                "  \"aggregationPipeline\" : {\n    \"stages\" : [ ]\n  }\n" +
                "}", mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithEmptyMatch() throws Exception
    {
        String input = "{ aggregate: 'firms', pipeline: [ { $match: { } } ] }";

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals("{\n" +
                "  \"type\" : \"aggregate\",\n" +
                "  \"collectionName\" : \"'firms'\",\n" +
                "  \"aggregationPipeline\" : {\n" +
                "    \"stages\" : [ {\n" +
                "      \"stageName\" : \"$match\",\n" +
                "      \"expression\" : [ {\n" +
                "        \"arguments\" : null\n" +
                "      } ]\n " +
                "   } ]\n " +
                " }\n" +
                "}", mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMatchSimpleExpression() throws Exception
    {
        String input = "{ aggregate: 'firms', pipeline: [ { $match: { test : 'testingg' } } ] }";

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals("{\n" +
                "  \"type\" : \"aggregate\",\n" +
                "  \"collectionName\" : \"'firms'\",\n" +
                "  \"aggregationPipeline\" : {\n" +
                "    \"stages\" : [ {\n" +
                "      \"stageName\" : \"$match\",\n" +
                "      \"expression\" : [ {\n" +
                "        \"arguments\" : [ {\n" +
                "          \"operator\" : null,\n" +
                "          \"expression\" : {\n" +
                "            \"field\" : {\n" +
                "              \"path\" : \"test\"\n" +
                "            },\n" +
                "            \"argument\" : {\n" +
                "              \"value\" : {\n" +
                "                \"pattern\" : \"'testingg'\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        } ]\n" +
                "      } ]\n" +
                "    } ]\n " +
                " }\n" +
                "}", mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMatchExpressionWithOperator() throws Exception
    {
        String input = "{ aggregate: 'firms', pipeline: [ { $match: { test : { $eq: 'testingg' } } } ] }";

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals("{\n" +
                "  \"type\" : \"aggregate\",\n" +
                "  \"collectionName\" : \"'firms'\",\n" +
                "  \"aggregationPipeline\" : {\n" +
                "    \"stages\" : [ {\n" +
                "      \"stageName\" : \"$match\",\n" +
                "      \"expression\" : [ {\n" +
                "        \"arguments\" : [ {\n" +
                "          \"operator\" : \"$eq\",\n" +
                "          \"expression\" : {\n" +
                "            \"field\" : {\n" +
                "              \"path\" : \"test\"\n" +
                "            },\n" +
                "            \"argument\" : {\n" +
                "              \"value\" : {\n" +
                "                \"pattern\" : \"'testingg'\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        } ]\n" +
                "      } ]\n" +
                "    } ]\n " +
                " }\n" +
                "}", mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithoutAndOperator() throws Exception
    {
        String input = "{ aggregate: 'firms', pipeline: [ { $match: { test : { $eq: 'USA' }, test2 : { $ne: 'GB' } } } ] }";

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals("{\n" +
                "  \"type\" : \"aggregate\",\n" +
                "  \"collectionName\" : \"'firms'\",\n" +
                "  \"aggregationPipeline\" : {\n" +
                "    \"stages\" : [ {\n" +
                "      \"stageName\" : \"$match\",\n" +
                "      \"expression\" : [ {\n" +
                "        \"arguments\" : [ {\n" +
                "          \"operator\" : \"$eq\",\n" +
                "          \"expression\" : {\n" +
                "            \"field\" : {\n" +
                "              \"path\" : \"test\"\n" +
                "            },\n" +
                "            \"argument\" : {\n" +
                "              \"value\" : {\n" +
                "                \"pattern\" : \"'USA'\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"operator\" : \"$ne\",\n" +
                "          \"expression\" : {\n" +
                "            \"field\" : {\n" +
                "              \"path\" : \"test2\"\n" +
                "            },\n" +
                "            \"argument\" : {\n" +
                "              \"value\" : {\n" +
                "                \"pattern\" : \"'GB'\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        } ]\n" +
                "      } ]\n" +
                "    } ]\n " +
                " }\n" +
                "}", mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithAndOperator() throws Exception
    {
        String input = "{ aggregate: 'firms', pipeline: [ { $match:{ $and : [ { fName: { $eq: 'Peter' }  }, { lName: { $eq: 'Smith' }  }  ]} } ] }";

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals("{\n" +
                "  \"type\" : \"aggregate\",\n" +
                "  \"collectionName\" : \"'firms'\",\n" +
                "  \"aggregationPipeline\" : {\n" +
                "    \"stages\" : [ {\n" +
                "      \"stageName\" : \"$match\",\n" +
                "      \"expression\" : [ {\n" +
                "        \"arguments\" : [ {\n" +
                "          \"expressions\" : [ {\n" +
                "            \"operator\" : \"$eq\",\n" +
                "            \"expression\" : {\n" +
                "              \"field\" : {\n" +
                "                \"path\" : \"fName\"\n" +
                "              },\n" +
                "              \"argument\" : {\n" +
                "                \"value\" : {\n" +
                "                  \"pattern\" : \"'Peter'\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }, {\n" +
                "            \"operator\" : \"$eq\",\n" +
                "            \"expression\" : {\n" +
                "              \"field\" : {\n" +
                "                \"path\" : \"lName\"\n" +
                "              },\n" +
                "              \"argument\" : {\n" +
                "                \"value\" : {\n" +
                "                  \"pattern\" : \"'Smith'\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          } ],\n" +
                "          \"operators\" : \"$and\"\n" +
                "        } ]\n" +
                "      } ]\n" +
                "    } ]\n " +
                " }\n" +
                "}", mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithOrOperator() throws Exception
    {
        String input = "{ aggregate: 'firms', pipeline: [ { $match:{ $or : [ { fName: { $eq: 'Peter' }  }, { lName: { $eq: 'Smith' }  }  ]} } ] }";

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals("{\n" +
                "  \"type\" : \"aggregate\",\n" +
                "  \"collectionName\" : \"'firms'\",\n" +
                "  \"aggregationPipeline\" : {\n" +
                "    \"stages\" : [ {\n" +
                "      \"stageName\" : \"$match\",\n" +
                "      \"expression\" : [ {\n" +
                "        \"arguments\" : [ {\n" +
                "          \"expressions\" : [ {\n" +
                "            \"operator\" : \"$eq\",\n" +
                "            \"expression\" : {\n" +
                "              \"field\" : {\n" +
                "                \"path\" : \"fName\"\n" +
                "              },\n" +
                "              \"argument\" : {\n" +
                "                \"value\" : {\n" +
                "                  \"pattern\" : \"'Peter'\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }, {\n" +
                "            \"operator\" : \"$eq\",\n" +
                "            \"expression\" : {\n" +
                "              \"field\" : {\n" +
                "                \"path\" : \"lName\"\n" +
                "              },\n" +
                "              \"argument\" : {\n" +
                "                \"value\" : {\n" +
                "                  \"pattern\" : \"'Smith'\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          } ],\n" +
                "          \"operators\" : \"$or\"\n" +
                "        } ]\n" +
                "      } ]\n" +
                "    } ]\n " +
                " }\n" +
                "}", mapper.writeValueAsString(databaseCommand));

    }
}