// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.postgres.handler.legend;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

import java.util.stream.Collectors;
import org.finos.legend.engine.postgres.PostgresServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendTdsResultParser
{
    public static final String BUILDER = "builder";
    public static final String TYPE = "type";
    public static final String COLUMNS = "columns";
    public static final String NAME = "name";
    public static final String RESULT = "result";
    public static final String ROWS = "rows";
    public static final String VALUES = "values";
    private static final List<JsonToken> SKIP_CHILDREN_TOKEN
            = Collections.unmodifiableList(Arrays.asList(JsonToken.START_ARRAY, JsonToken.START_OBJECT));

    private static final Logger LOGGER = LoggerFactory.getLogger(LegendTdsResultParser.class);

    public static final String _TYPE = "_type";
    private final JsonParser parser;
    private List<LegendColumn> legendColumns;
    private List<Object> currentRow;

    private boolean finishedReading = false;

    public LegendTdsResultParser(InputStream inputStream) throws IOException
    {
        requireNonNull(inputStream, "TDS InputStream can't be null");
        this.parser = new MappingJsonFactory().createParser(inputStream);
        startParsing();
    }


    public List<LegendColumn> getLegendColumns()
    {
        return Collections.unmodifiableList(legendColumns);
    }

    public synchronized boolean hasNext() throws IOException
    {
        return readNextDataRow();
    }

    public synchronized List<Object> next()
    {
        return Collections.unmodifiableList(currentRow);
    }

    public synchronized void close() throws IOException
    {
        if (!parser.isClosed())
        {
            parser.close();
        }
    }

    private void startParsing() throws IOException
    {
        acceptNextToken(JsonToken.START_OBJECT);
        acceptNextToken(BUILDER);
        parserHeader();
        parseActivities();
        moveCursorToDataResults();
    }

    private void parserHeader() throws IOException
    {
        legendColumns = new ArrayList<>();
        acceptNextToken(JsonToken.START_OBJECT);
        parseNextTextField(_TYPE);
        acceptNextToken(COLUMNS);
        acceptNextToken(JsonToken.START_ARRAY);
        while (parser.nextToken() != JsonToken.END_ARRAY)
        {
            //parse column
            acceptCurrent(JsonToken.START_OBJECT);
            String columnName = parseNextTextField(NAME);
            String type = parseNextTextField(TYPE);
            skipUntilToken(JsonToken.END_OBJECT);
            legendColumns.add(new LegendColumn(columnName, type));
        }
        acceptNextToken(JsonToken.END_OBJECT);
    }

    private void moveCursorToDataResults() throws IOException
    {
        acceptNextToken(RESULT);
        acceptNextToken(JsonToken.START_OBJECT);
        acceptNextToken(COLUMNS);
        skipChildren();
        acceptNextToken(ROWS);
        acceptNextToken(JsonToken.START_ARRAY);
    }

    private synchronized boolean readNextDataRow() throws IOException
    {
        if (finishedReading)
        {
            return false;
        }

        if (parser.nextToken() == JsonToken.END_ARRAY)
        {
            finishedReading = true;
            if (!parser.isClosed())
            {
                parser.close();
            }
            return false;
        }
        acceptCurrent(JsonToken.START_OBJECT);
        acceptNextToken(VALUES);
        acceptNextToken(JsonToken.START_ARRAY);
        currentRow = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY)
        {
            JsonToken currentToken = parser.currentToken();
            Object value = null;
            switch (currentToken)
            {
                case VALUE_NULL:
                    value = null;
                    break;
                case VALUE_NUMBER_INT:
                    value = parser.getLongValue();
                    break;
                case VALUE_NUMBER_FLOAT:
                    value = parser.getDoubleValue();
                    break;
                case VALUE_TRUE:
                    value = parser.getBooleanValue();
                    break;
                case VALUE_FALSE:
                    value = parser.getBooleanValue();
                    break;
                default:
                    value = parser.getText();
            }
            currentRow.add(value);
        }
        acceptNextToken(JsonToken.END_OBJECT);
        return true;
    }


    private void parseActivities() throws IOException
    {
        acceptNextToken("activities");
        skipChildren();
    }

    private String parseNextTextField(String fieldName) throws IOException
    {
        acceptNextToken(fieldName);
        return parser.nextTextValue();
    }


    private void skipChildren() throws IOException
    {
        JsonToken jsonToken = parser.nextToken();
        validate(SKIP_CHILDREN_TOKEN, jsonToken);
        parser.skipChildren();
    }

    private void skipUntilToken(JsonToken expectedToken) throws IOException
    {
        parser.nextToken();
        while (!expectedToken.equals(parser.currentToken()))
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Skipping token :" + parser.currentToken());
            }
            parser.nextToken();
        }
    }

    private void acceptNextToken(JsonToken expectedToken) throws IOException
    {
        JsonToken jsonToken = parser.nextToken();
        validate(Arrays.asList(expectedToken), jsonToken);
    }

    private void acceptCurrent(JsonToken expectedToken) throws IOException
    {
        JsonToken jsonToken = parser.currentToken();
        validate(Arrays.asList(expectedToken), jsonToken);
    }

    private void acceptNextToken(String expectedField) throws IOException
    {
        String fieldName = parser.nextFieldName();
        validate(expectedField, fieldName);
    }

    private void validate(List<JsonToken> expectedTokens, JsonToken actualToken)
    {
        if (actualToken == null || !expectedTokens.contains(actualToken))
        {
            throw new PostgresServerException("Expected: '" +
                    expectedTokens.stream().map(o -> Objects.toString(o)).collect(Collectors.joining(", "))
                    + "', Found: '" + actualToken + "'");
        }
    }


    private void validate(String expectedFieldName, String actualFieldName)
    {
        if (actualFieldName == null || !expectedFieldName.equals(actualFieldName))
        {
            throw new PostgresServerException("Failed to parse JSON expected '" + expectedFieldName + "', Found '" + actualFieldName);
        }
    }
}
