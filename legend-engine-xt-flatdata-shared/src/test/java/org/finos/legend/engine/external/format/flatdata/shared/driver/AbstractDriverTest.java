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

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.*;
import org.finos.legend.engine.external.format.flatdata.shared.grammar.FlatDataSchemaParser;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

public class AbstractDriverTest
{
    private static List<FlatDataDriverDescription> descriptions = FlatDataDriverDescription.loadAll();

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

    protected InputStream resource(String name)
    {
        return Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(name), "Failed to find resource " + name);
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
        return deserialize(clazz, flatData, new ByteArrayInputStream(data.getBytes()));
    }

    protected <T> List<IChecked<T>> deserialize(Class<T> clazz, FlatData flatData, InputStream data)
    {
        Deserializer<T> deserializer =  new Deserializer<>(flatData, data);
        flatData.getSections().forEach(s -> deserializer.withSectionDetails(s.getName(), clazz, true));
        return deserializer.deserialize();
    }

    public static class Deserializer<T>
    {
        private final FlatData flatData;
        private final InputStream data;
        private Map<String, List<IChecked<?>>> result = new HashMap<>();
        private Map<String, Class<?>> clazzBySectionName = new HashMap<>();
        private Map<String, Boolean> returnableBySectionName = new HashMap<>();

        public Deserializer(FlatData flatData, InputStream data)
        {

            this.flatData = flatData;
            this.data = data;
        }

        public Deserializer<T> withSectionDetails(String sectionId, Class<?> clazz, boolean isReturnable)
        {
            clazzBySectionName.put(sectionId, clazz);
            returnableBySectionName.put(sectionId, isReturnable);
            return this;
        }

        public <X> List<X> recordsCreatedBy(String sectionId)
        {
            return (List<X>) result.get(sectionId);
        }

        public List<IChecked<T>> deserialize()
        {
            List<IChecked<T>> recordsRead = new ArrayList<>();
            FlatDataSection firstSection = flatData.getSections().get(0);
            FlatDataProcessor.Builder<T> builder = descriptionFor(firstSection).<T>getProcessorBuilderFactory().apply(flatData).withDefiningPath("test");
            flatData.getSections().forEach(s -> builder.withToObjectFactoryFactory(s.getName(), x -> reflectiveToObject(s.getName(), x)));
            builder.build().readData(data, recordsRead::add);
            return recordsRead;
        }

        private FlatDataDriverDescription descriptionFor(FlatDataSection section)
        {
            return descriptions.stream()
                    .filter(d -> d.getId().equals(section.getDriverId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No driver for: '" + section.getDriverId() + "'"));
        }

        ParsedFlatDataToObject<?> reflectiveToObject(String sectionId, FlatDataRecordType type)
        {
            Class<?> clazz = Objects.requireNonNull(clazzBySectionName.get(sectionId), "No class for section " + sectionId);
            List<IChecked<?>> records = new ArrayList<>();
            result.put(sectionId, records);

            return new ParsedFlatDataToObject()
            {
                @Override
                public IChecked makeChecked(ParsedFlatData parsedFlatData)
                {
                    IChecked parsed = ParsedFlatDataToObject.super.makeChecked(parsedFlatData);
                    records.add(parsed);
                    return parsed;
                }

                @Override
                public boolean isReturnable()
                {
                    return returnableBySectionName.get(sectionId);
                }

                @Override
                public Object make(ParsedFlatData parsedFlatData)
                {
                    try
                    {
                        Object result = clazz.getConstructor().newInstance();
                        for (Field field : clazz.getFields())
                        {
                            List<Predicate<String>> matchers = Arrays.asList(
                                    s -> s.equals(field.getName()),
                                    s -> s.equalsIgnoreCase(field.getName()),
                                    s -> s.replace("_", "").equals(field.getName()),
                                    s -> s.replace("_", "").equalsIgnoreCase(field.getName())
                            );
                            FlatDataRecordField fdField = null;
                            for (Predicate<String> matcher: matchers)
                            {
                                if (fdField == null)
                                {
                                    fdField = type.getFields().stream().filter(f -> matcher.test(f.getLabel())).findFirst().orElse(null);
                                }
                            }

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
                }
            };
        }
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
