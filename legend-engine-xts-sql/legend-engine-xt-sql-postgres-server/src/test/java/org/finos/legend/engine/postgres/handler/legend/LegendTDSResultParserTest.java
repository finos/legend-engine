// Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.postgres.handler.legend;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendTDSResultParserTest
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendTDSResultParserTest.class);


    @Test
    public void testParseDataValidateResults() throws IOException
    {
        try (InputStream pureProjectInputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("org/finos/legend/engine/postgres/handler/legend/legendTdsResult.json");)
        {
            LegendTdsResultParser parser = new LegendTdsResultParser(pureProjectInputStream);
            List<LegendColumn> legendColumns = parser.getLegendColumns();
            int row = 0;
            int column = 0;
            while (parser.hasNext())
            {
                row++;
                column = 0;
                List<Object> nextRow = parser.next();
                for (int i = 0; i < legendColumns.size(); i++)
                {
                    Object value = nextRow.get(i);
                    LOGGER.info("Row: {}, Column: {}: Value: {}", row, column, value);
                    column++;
                }
                Assert.assertEquals("Verify number of columns", 8, column);
            }
            Assert.assertEquals("Verify number of rows", 2, row);
        }
    }

    @Test
    public void testParseDataNoRelationalTypeValidateResults() throws IOException
    {
        try (InputStream pureProjectInputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("org/finos/legend/engine/postgres/handler/legend/legendTdsResultNoRelationalType.json");)
        {
            LegendTdsResultParser parser = new LegendTdsResultParser(pureProjectInputStream);
            List<LegendColumn> legendColumns = parser.getLegendColumns();
            int row = 0;
            int column = 0;
            while (parser.hasNext())
            {
                row++;
                column = 0;
                List<Object> nextRow = parser.next();
                for (int i = 0; i < legendColumns.size(); i++)
                {
                    Object value = nextRow.get(i);
                    LOGGER.info("Row: {}, Column: {}: Value: {}", row, column, value);
                    column++;
                }
                Assert.assertEquals("Verify number of columns", 2, column);
            }
            Assert.assertEquals("Verify number of rows", 2, row);
        }
    }

    @Test(expected = RuntimeException.class)
    public void testParseDataInvalidateResults() throws IOException
    {
        try (InputStream pureProjectInputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("legendTdsInvalidResult.json");)
        {
            LegendTdsResultParser parser = new LegendTdsResultParser(pureProjectInputStream);
        }
    }
}
