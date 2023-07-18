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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.finos.legend.engine.language.pure.grammar.from.deserializer.MongoDBQueryDeserializer;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

import java.io.IOException;

public class MongoDBQueryParser
{

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MongoDBQueryParser.class);

    private MongoDBQueryParser()
    {
    }

    public static MongoDBQueryParser newInstance()
    {
        return new MongoDBQueryParser();
    }


    public DatabaseCommand parseQueryDocument(String inputQry)
    {
        try
        {
            JsonFactory jFactory = new JsonFactory();
            JsonParser jParser = jFactory.createParser(inputQry);
            jParser.setCodec(getObjectMapper());
            DatabaseCommand dbCommand = getObjectMapper().readValue(jParser, DatabaseCommand.class);
            jParser.close();
            return dbCommand;
        }
        catch (JsonParseException jpException)
        {
            String msg = jpException.getMessage();
            int line = jpException.getLocation().getLineNr();
            int charPositionInLine = jpException.getLocation().getColumnNr();
            SourceInformation sourceInformation = new SourceInformation("", line, charPositionInLine + 1, line, charPositionInLine + 1);
            throw new MongoDBParserException(msg, sourceInformation);
        }
        catch (IOException e)
        {
            String msg = "Unexpected IOException: " + e.getMessage();
            SourceInformation sourceInformation = new SourceInformation("", -1, -1, -1, -1);
            throw new MongoDBParserException(msg, sourceInformation);
        }

    }

    ObjectMapper getObjectMapper()
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(DatabaseCommand.class, new MongoDBQueryDeserializer());
        mapper.registerModule(module);
        return mapper;
    }
}
