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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MongoDatabase;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

import java.io.IOException;

public class MongoDBSchemaParseTreeWalker
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");


    private MongoDBSchemaParseTreeWalker()
    {
    }

    public static MongoDBSchemaParseTreeWalker newInstance()
    {
        return new MongoDBSchemaParseTreeWalker();
    }

//    private static MongoDBSchemaParserException raiseException(int line, int startColumn, String errMessage)
//    {
//        SourceInformation sourceInformation = new SourceInformation("", line, startColumn, line, startColumn);
//        return new MongoDBSchemaParserException(errMessage, sourceInformation);
//    }

    private ObjectMapper getObjectMapper()
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(MongoDatabase.class, new MongoDBSchemaDeserializer());
        mapper.registerModule(module);
        //mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        //mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
        return mapper;
    }

    public MongoDatabase parseDocument(String code)
    {
        return this.parse(code);
    }

    private MongoDatabase parse(String inputJson)
    {

        try
        {
            JsonFactory jFactory = new JsonFactory();
            JsonParser jParser = jFactory.createParser(inputJson);
            jParser.setCodec(getObjectMapper());
            while (jParser.nextToken() != JsonToken.END_OBJECT)
            {
                if ("database".equals(jParser.getCurrentName()))
                {
                    jParser.nextToken();
                    return getObjectMapper().readValue(jParser, MongoDatabase.class);
                }
                else
                {
                    LOGGER.info("Skipping top-level element: {}, expected only database field", jParser.getCurrentName());
                }
            }
            jParser.close();
            return new MongoDatabase();
        }
        catch (JsonParseException jpException)
        {
            String msg = jpException.getMessage();
            int line = jpException.getLocation().getLineNr();
            int charPositionInLine = jpException.getLocation().getColumnNr();
            SourceInformation sourceInformation = new SourceInformation("", line, charPositionInLine + 1, line, charPositionInLine + 1);
            throw new MongoDBSchemaParserException(msg, sourceInformation);
        }
        catch (IOException e)
        {
            String msg = "Unexpected IOException: " + e.getMessage();
            SourceInformation sourceInformation = new SourceInformation("", -1, -1, -1, -1);
            throw new MongoDBSchemaParserException(msg, sourceInformation);
        }

    }

}
