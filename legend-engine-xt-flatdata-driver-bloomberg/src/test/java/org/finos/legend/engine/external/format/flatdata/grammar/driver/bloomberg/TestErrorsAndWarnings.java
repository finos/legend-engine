//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.grammar.driver.bloomberg;

import org.finos.legend.engine.external.format.flatdata.grammar.driver.AbstractDriverTest;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

public class TestErrorsAndWarnings extends AbstractDriverTest
{

    private static final String FLAT_DATA_GRAMMAR = "section default: BloombergData\n" +
            "{\n" +
            "  mustBePresent;\n" +
            "\n" +
            "  Record\n" +
            "  {\n" +
            "    Test: STRING;\n" +
            "  }\n" +
            "}\n" +
            "section metadata: BloombergMetadata\n" +
            "{\n" +
            "  Record\n" +
            "  {\n" +
            "    PROGRAMNAME : STRING;\n" +
            "    DATEFORMAT  : STRING;\n" +
            "    REGION      : STRING(optional);\n" +
            "  }\n" +
            "}";

    @Test
    public void canIgnoreSomeRubbish()
    {
        FlatData flatData = parseFlatData(FLAT_DATA_GRAMMAR);

        InputStream data = new ByteArrayInputStream((
                "START-OF-FILE\n" +
                        "PROGRAMNAME=getdata\n" +
                        "blah\n" +
                        "DATEFORMAT=yyyy-mm-dd\n" +
                        "REGION=euro\n" +
                        "START-OF-FIELDS\n" +
                        "Test\n" +
                        "END-OF-FIELDS\n" +
                        "START-OF-DATA\n" +
                        "END-OF-DATA\n" +
                        "END-OF-FILE"
        ).getBytes());

        List<IChecked<Object>> records = new Deserializer<>(flatData, data)
                .withSectionDetails("default", TestClass.class, true)
                .withSectionDetails("metadata", BloombergFixtures.BloombergMetadata.class, true)
                .deserialize();

        assertHasDefect("Error", "Ignoring malformed line: 'blah'", records.get(0));
    }

    @Test
    public void failsForMissingMandatoryMetadata()
    {
        FlatData flatData = parseFlatData(FLAT_DATA_GRAMMAR);

        InputStream data = new ByteArrayInputStream((
                "START-OF-FILE\n" +
                        "PROGRAMNAME=getdata\n" +
                        "REGION=euro\n" +
                        "START-OF-FIELDS\n" +
                        "Test\n" +
                        "END-OF-FIELDS\n" +
                        "START-OF-DATA\n" +
                        "testData\n" +
                        "END-OF-DATA\n" +
                        "END-OF-FILE"
        ).getBytes());

        List<IChecked<Object>> records = new Deserializer<>(flatData, data)
                .withSectionDetails("default", TestClass.class, true)
                .withSectionDetails("metadata", BloombergFixtures.BloombergMetadata.class, true)
                .deserialize();

        assertHasDefect("Critical", "Failed to read 'DATEFORMAT' not present in the source", records.get(1));
    }

    @Test
    public void succeedsForMissingOptionalData()
    {
        FlatData flatData = parseFlatData(FLAT_DATA_GRAMMAR);

        InputStream data = new ByteArrayInputStream((
                "START-OF-FILE\n" +
                        "PROGRAMNAME=getdata\n" +
                        "DATEFORMAT=yyyy-mm-dd\n" +
                        "START-OF-FIELDS\n" +
                        "Test\n" +
                        "END-OF-FIELDS\n" +
                        "START-OF-DATA\n" +
                        "testData\n" +
                        "END-OF-DATA\n" +
                        "END-OF-FILE"
        ).getBytes());

        List<IChecked<Object>> records = new Deserializer<>(flatData, data)
                .withSectionDetails("default", TestClass.class, true)
                .withSectionDetails("metadata", BloombergFixtures.BloombergMetadata.class, true)
                .deserialize();

        assertNoDefects(records.get(1));
    }

    @Test(expected = IllegalStateException.class)
    public void canFailIfNoStartOfFile()
    {
        FlatData flatData = parseFlatData(FLAT_DATA_GRAMMAR);

        InputStream data = new ByteArrayInputStream((
                "PROGRAMNAME=test\n" +
                        "DATEFORMAT=yyyy-mm-dd\n" +
                        "START-OF-FIELDS\n" +
                        "Test\n" +
                        "END-OF-FIELDS\n" +
                        "START-OF-DATA\n" +
                        "END-OF-DATA\n" +
                        "END-OF-FILE"
        ).getBytes());

        new Deserializer<>(flatData, data)
                .withSectionDetails("default", TestClass.class, true)
                .withSectionDetails("metadata", BloombergFixtures.BloombergMetadata.class, true)
                .deserialize();
    }

    @Test(expected = IllegalStateException.class)
    public void canFailsIfNoStartOfFields()
    {
        FlatData flatData = parseFlatData(FLAT_DATA_GRAMMAR);

        InputStream data = new ByteArrayInputStream((
                "START-OF-FILE\n" +
                        "PROGRAMNAME=test\n" +
                        "DATEFORMAT=yyyy-mm-dd\n" +
                        "Test\n" +
                        "END-OF-FIELDS\n" +
                        "START-OF-DATA\n" +
                        "END-OF-DATA\n" +
                        "END-OF-FILE"
        ).getBytes());

        new Deserializer<>(flatData, data)
                .withSectionDetails("default", TestClass.class, true)
                .withSectionDetails("metadata", BloombergFixtures.BloombergMetadata.class, true)
                .deserialize();
    }

    @Test(expected = IllegalStateException.class)
    public void canFailsIfNoEndOfFields()
    {
        FlatData flatData = parseFlatData(FLAT_DATA_GRAMMAR);

        InputStream data = new ByteArrayInputStream((
                "START-OF-FILE\n" +
                        "PROGRAMNAME=test\n" +
                        "DATEFORMAT=yyyy-mm-dd\n" +
                        "START-OF-FIELDS\n" +
                        "Test\n" +
                        "START-OF-DATA\n" +
                        "END-OF-DATA\n" +
                        "END-OF-FILE"
        ).getBytes());

        new Deserializer<>(flatData, data)
                .withSectionDetails("default", TestClass.class, true)
                .withSectionDetails("metadata", BloombergFixtures.BloombergMetadata.class, true)
                .deserialize();
    }

    @Test(expected = IllegalStateException.class)
    public void canFailsIfNoStartOfData()
    {
        FlatData flatData = parseFlatData(FLAT_DATA_GRAMMAR);

        InputStream data = new ByteArrayInputStream((
                "START-OF-FILE\n" +
                        "PROGRAMNAME=test\n" +
                        "DATEFORMAT=yyyy-mm-dd\n" +
                        "START-OF-FIELDS\n" +
                        "Test\n" +
                        "END-OF-FIELDS\n" +
                        "END-OF-DATA\n" +
                        "END-OF-FILE").getBytes());

        new Deserializer<>(flatData, data)
                .withSectionDetails("default", TestClass.class, true)
                .withSectionDetails("metadata", BloombergFixtures.BloombergMetadata.class, true)
                .deserialize();
    }

    @Test(expected = IllegalStateException.class)
    public void canFailsIfNoEndOfData()
    {
        FlatData flatData = parseFlatData(FLAT_DATA_GRAMMAR);

        InputStream data = new ByteArrayInputStream((
                "START-OF-FILE\n" +
                        "PROGRAMNAME=test\n" +
                        "DATEFORMAT=yyyy-mm-dd\n" +
                        "START-OF-FIELDS\n" +
                        "Test\n" +
                        "END-OF-FIELDS\n" +
                        "START-OF-DATA\n" +
                        "END-OF-FILE"
        ).getBytes());

        new Deserializer<>(flatData, data)
                .withSectionDetails("default", TestClass.class, true)
                .withSectionDetails("metadata", BloombergFixtures.BloombergMetadata.class, true)
                .deserialize();
    }

    @Test(expected = IllegalStateException.class)
    public void canFailsIfNoEndOfFile()
    {
        FlatData flatData = parseFlatData(FLAT_DATA_GRAMMAR);

        InputStream data = new ByteArrayInputStream((
                "START-OF-FILE\n" +
                        "PROGRAMNAME=test\n" +
                        "DATEFORMAT=yyyy-mm-dd\n" +
                        "START-OF-FIELDS\n" +
                        "Test\n" +
                        "END-OF-FIELDS\n" +
                        "START-OF-DATA\n" +
                        "END-OF-DATA"
        ).getBytes());

        new Deserializer<>(flatData, data)
                .withSectionDetails("default", TestClass.class, true)
                .withSectionDetails("metadata", BloombergFixtures.BloombergMetadata.class, true)
                .deserialize();
    }

    public static class TestClass
    {
        public String test;
    }
}
