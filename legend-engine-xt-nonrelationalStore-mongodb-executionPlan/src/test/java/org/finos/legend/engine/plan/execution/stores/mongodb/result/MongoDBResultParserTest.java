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

package org.finos.legend.engine.plan.execution.stores.mongodb.result;

import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;


public class MongoDBResultParserTest
{
    @Test
    public void testParsingMongoDBDate() throws IOException
    {
        // Test to validate the code that we generate as part of Mongo Parsing
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = "{\"firstName\": \"Coralie\", \"lastName\": \"Batie\", \"age\": 23, \"birthDate\": {\"$date\": 323699375000}}";
        JsonNode dataNode = objectMapper.readValue(jsonString, JsonNode.class);
        JsonNode dateNode = dataNode.get("birthDate");
        if (dateNode.has("$date") && dateNode.get("$date").isLong())
        {
            long inputVal1 = dateNode.get("$date").longValue();
            PureDate pureDate = PureDate.fromDate(new Date(inputVal1));
            Assert.assertEquals("PureDate instant should match input date", Instant.ofEpochMilli(323699375000L), pureDate.toInstant());
        }
        else
        {
            Assert.fail("Date is serialized as object in Mongo result json");
        }
    }
}
