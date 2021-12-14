// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.driver;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.InputStreamConnection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.ProcessingVariables;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.VariablesProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.Connection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.Cursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataDriverDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataReadDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ObjectToParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatDataValue;
import org.finos.legend.engine.external.format.flatdata.shared.grammar.FlatDataSchemaParser;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AbstractDriverTest
{
    private List<FlatDataDriverDescription> descriptions = FlatDataDriverDescription.loadAll();

    protected FlatData parseFlatData(String flatDataGrammar)
    {
        return new FlatDataSchemaParser(flatDataGrammar).parse();
    }

    protected String data(String eol, String... lines)
    {
        return data(eol, false, lines);
    }

    protected String data(String eol, boolean addTrailing, String... lines)
    {
        String trailing = addTrailing ? eol : "";
        return String.join(eol, lines) + trailing;
    }

    protected void assertNoDefects(IChecked<?> record)
    {
        Assert.assertTrue("Expected no defects but found: " + record, record.getDefects().isEmpty());
    }

    protected void assertHasDefect(String level, String message, IChecked<?> record)
    {
        if (record.getDefects().stream().noneMatch(d -> level.equals(d.getEnforcementLevel().toString()) && message.equals(d.getMessage())))
        {
            Assert.fail("Missing " + level + " defect: " + message + "; actual defects found: " + record.getDefects());
        }
    }

    protected <T> List<IChecked<T>> deserialize(Class<T> clazz, FlatData flatData, String data)
    {
        List<IChecked<T>> records = Lists.mutable.empty();
        try
        {
            Connection connection = new InputStreamConnection(new ByteArrayInputStream(data.getBytes()));
            connection.open();

            ProcessingVariables variables = new ProcessingVariables(flatData);
            List<FlatDataReadDriver<T>> drivers = new LinkedList<>();
            FlatDataReadDriver<T> drv = null;
            for (int i = flatData.getSections().size() - 1; i >= 0; i--)
            {
                FlatDataSection section = flatData.getSections().get(i);
                FlatDataDriverDescription description = descriptions.stream().filter(d -> d.getId().equals(section.getDriverId())).findFirst().orElseThrow(() -> new RuntimeException("No driver for: '" + section.getDriverId() + "'"));
                drv = description.newReadDriver(section, new SectionProcessingContext(variables, description.getDeclares(), connection, drv, clazz));
                drivers.add(0, drv);
            }

            for (FlatDataReadDriver<T> driver: drivers)
            {
                driver.start();
                while (!driver.isFinished())
                {
                    driver.readCheckedObjects().forEach(records::add);
                }
                driver.stop();
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return records;
    }

    private class SectionProcessingContext<T> extends VariablesProcessingContext
    {
        private final Connection connection;
        private final FlatDataDriver nextDriver;
        private final Class<T> clazz;

        public SectionProcessingContext(ProcessingVariables variables, List<FlatDataVariable> declared, Connection connection, FlatDataDriver nextDriver, Class<T> clazz)
        {
            super(variables, declared);
            this.connection = connection;
            this.nextDriver = nextDriver;
            this.clazz = clazz;
        }

        @Override
        public String getDefiningPath()
        {
            return "test::Schema";
        }

        @Override
        public Connection getConnection()
        {
            return connection;
        }

        @Override
        public boolean isNextSectionReadyToStartAt(Cursor cursor)
        {
            return (nextDriver == null) ? cursor.isEndOfData() : nextDriver.canStartAt(cursor);
        }

        @Override
        public <T> ParsedFlatDataToObject createToObjectFactory(FlatDataRecordType recordType)
        {
            return reflectiveToObject(clazz, recordType);
        }

        @Override
        public <T> ObjectToParsedFlatData createFromObjectFactory(FlatDataRecordType recordType)
        {
            return null;
        }
    }

    protected <T> ParsedFlatDataToObject<T> reflectiveToObject(Class<T> clazz, FlatDataRecordType type)
    {
        return (ParsedFlatData parsedFlatData) ->
        {
            try
            {
                T result = clazz.getConstructor().newInstance();
                for (Field field : clazz.getDeclaredFields())
                {
                    FlatDataRecordField fdField = type.getFields().stream().filter(f -> f.getLabel().equalsIgnoreCase(field.getName())).findFirst().orElse(null);
                    if (fdField == null)
                    {
                        // Ignore
                    }
                    else if (field.getType().equals(String.class))
                    {
                        if (parsedFlatData.hasStringValue(fdField))
                        {
                            field.set(result, parsedFlatData.getString(fdField));
                        }
                    }
                    else if (field.getType().equals(Boolean.TYPE) || field.getType().equals(Boolean.class))
                    {
                        if (parsedFlatData.hasBooleanValue(fdField))
                        {
                            field.set(result, parsedFlatData.getBoolean(fdField));
                        }
                    }
                    else if (field.getType().equals(Long.TYPE) || field.getType().equals(Long.class))
                    {
                        if (parsedFlatData.hasLongValue(fdField))
                        {
                            field.set(result, parsedFlatData.getLong(fdField));
                        }
                    }
                    else if (field.getType().equals(Double.TYPE) || field.getType().equals(Double.class))
                    {
                        if (parsedFlatData.hasDoubleValue(fdField))
                        {
                            field.set(result, parsedFlatData.getDouble(fdField));
                        }
                    }
                    else if (field.getType().equals(BigDecimal.class))
                    {
                        if (parsedFlatData.hasBigDecimalValue(fdField))
                        {
                            field.set(result, parsedFlatData.getBigDecimal(fdField));
                        }
                    }
                    else if (field.getType().equals(LocalDate.class))
                    {
                        if (parsedFlatData.hasLocalDateValue(fdField))
                        {
                            field.set(result, parsedFlatData.getLocalDate(fdField));
                        }
                    }
                    else if (field.getType().equals(Instant.class))
                    {
                        if (parsedFlatData.hasInstantValue(fdField))
                        {
                            field.set(result, parsedFlatData.getInstant(fdField));
                        }
                    }
                }
                return result;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        };
    }

    protected static ExpectedRecordValue rValue(Object address, String raw)
    {
        return new ExpectedRecordValue(address, raw);
    }

    protected void assertSource(long expectedRecordNumber, long expectedLineNumber, String expectedRecord, List<ExpectedRecordValue> expectedValues, IChecked<?> record)
    {
        RawFlatData source = (RawFlatData) record.getSource();
        Assert.assertEquals(expectedRecordNumber, source.getNumber());
        Assert.assertEquals("Line number", expectedLineNumber, source.getLineNumber());
        Assert.assertEquals("Raw Record", expectedRecord, source.getRecord());

        source.getRecordValues().forEach(a ->
                                         {
                                             Optional<ExpectedRecordValue> expected = expectedValues.stream().filter(e -> e.address.equals(a.getAddress())).findFirst();
                                             if (expected.isPresent())
                                             {
                                                 Assert.assertEquals("Raw value for " + a.getAddress(), expected.get().value, a.getRawValue());
                                             }
                                             else
                                             {
                                                 Assert.fail("Unexpected raw value found for " + a.getAddress());
                                             }
                                         });
        expectedValues.forEach(e ->
                               {
                                   Optional<RawFlatDataValue> actual = source.getRecordValues().stream().filter(a -> a.getAddress().equals(e.address)).findFirst();
                                   if (!actual.isPresent())
                                   {
                                       Assert.fail("Missing raw value found for " + e.address);
                                   }
                               });
    }

    protected static class ExpectedRecordValue
    {

        private final Object address;
        private final String value;

        private ExpectedRecordValue(Object address, String value)
        {
            this.address = Objects.requireNonNull(address);
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public String toString()
        {
            return "ExpectedRecordValue{address=" + address + ", value=" + value + '}';
        }
    }
}
