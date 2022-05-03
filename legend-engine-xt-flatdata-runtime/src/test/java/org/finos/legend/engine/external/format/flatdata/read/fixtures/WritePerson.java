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

package org.finos.legend.engine.external.format.flatdata.read.fixtures;

import org.finos.legend.engine.external.format.flatdata.FlatDataContext;
import org.finos.legend.engine.external.format.flatdata.write.IFlatDataSerializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ObjectToParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataString;
import org.finos.legend.engine.external.format.test.fixture.app.meta.external.shared.format.executionPlan.testing.model.firm.Person;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class WritePerson implements IFlatDataSerializeExecutionNodeSpecifics<Person>
{
    public FlatDataSection flatDataSection_Person()
    {
        FlatDataRecordType recordType = new FlatDataRecordType().withField("firstName", new FlatDataString(false)).withField("lastName", new FlatDataString(false)).withField("dateOfBirth", new FlatDataDate(true)).withField("isAlive", new FlatDataBoolean(false)).withField("heightInMeters", new FlatDataDecimal(false));
        return new FlatDataSection("Person", "DelimitedWithHeadings").withProperty("scope.untilEof", true).withProperty("delimiter", ",").withRecordType(recordType);
    }

    public ObjectToParsedFlatData<Person> flatDataSection_Factory_Person(FlatDataRecordType recordType)
    {
        FlatDataRecordField firstNameField = recordType.getFields().stream().filter((FlatDataRecordField f) -> f.getLabel().equals("firstName")).findFirst().get();
        FlatDataRecordField lastNameField = recordType.getFields().stream().filter((FlatDataRecordField f) -> f.getLabel().equals("lastName")).findFirst().get();
        FlatDataRecordField dateOfBirthField = recordType.getFields().stream().filter((FlatDataRecordField f) -> f.getLabel().equals("dateOfBirth")).findFirst().get();
        FlatDataRecordField isAliveField = recordType.getFields().stream().filter((FlatDataRecordField f) -> f.getLabel().equals("isAlive")).findFirst().get();
        FlatDataRecordField heightInMetersField = recordType.getFields().stream().filter((FlatDataRecordField f) -> f.getLabel().equals("heightInMeters")).findFirst().get();
        return new ObjectToParsedFlatData<Person>()
        {
            public ParsedFlatData make(Person value)
            {
                return new ParsedFlatData()
                {
                    public boolean hasStringValue(FlatDataRecordField field)
                    {
                        if (field.equals(firstNameField))
                        {
                            return true;
                        }
                        else if (field.equals(lastNameField))
                        {
                            return true;
                        }
                        else
                        {
                            return false;
                        }
                    }

                    public boolean hasBooleanValue(FlatDataRecordField field)
                    {
                        if (field.equals(isAliveField))
                        {
                            return true;
                        }
                        else
                        {
                            return false;
                        }
                    }

                    public boolean hasLongValue(FlatDataRecordField field)
                    {
                        return false;
                    }

                    public boolean hasDoubleValue(FlatDataRecordField field)
                    {
                        if (field.equals(heightInMetersField))
                        {
                            return true;
                        }
                        else
                        {
                            return false;
                        }
                    }

                    public boolean hasBigDecimalValue(FlatDataRecordField field)
                    {
                        return false;
                    }

                    public boolean hasLocalDateValue(FlatDataRecordField field)
                    {
                        if (field.equals(dateOfBirthField))
                        {
                            return value.getDateOfBirth() != null;
                        }
                        else
                        {
                            return false;
                        }
                    }

                    public boolean hasInstantValue(FlatDataRecordField field)
                    {
                        return false;
                    }

                    public String getString(FlatDataRecordField field)
                    {
                        if (field.equals(firstNameField))
                        {
                            return value.getFirstName();
                        }
                        else if (field.equals(lastNameField))
                        {
                            return value.getLastName();
                        }
                        else
                        {
                            throw new IllegalArgumentException("Cannot get String value for field '" + field.getLabel() + "'");
                        }
                    }

                    public boolean getBoolean(FlatDataRecordField field)
                    {
                        if (field.equals(isAliveField))
                        {
                            return value.getIsAlive();
                        }
                        else
                        {
                            throw new IllegalArgumentException("Cannot get boolean value for field '" + field.getLabel() + "'");
                        }
                    }

                    public long getLong(FlatDataRecordField field)
                    {
                        throw new IllegalArgumentException("Cannot get long value for field '" + field.getLabel() + "'");
                    }

                    public double getDouble(FlatDataRecordField field)
                    {
                        if (field.equals(heightInMetersField))
                        {
                            return value.getHeightInMeters();
                        }
                        else
                        {
                            throw new IllegalArgumentException("Cannot get double value for field '" + field.getLabel() + "'");
                        }
                    }

                    public BigDecimal getBigDecimal(FlatDataRecordField field)
                    {
                        throw new IllegalArgumentException("Cannot get BigDecimal value for field '" + field.getLabel() + "'");
                    }

                    public LocalDate getLocalDate(FlatDataRecordField field)
                    {
                        if (field.equals(dateOfBirthField))
                        {
                            return value.getDateOfBirth().toLocalDate();
                        }
                        else
                        {
                            throw new IllegalArgumentException("Cannot get LocalDate value for field '" + field.getLabel() + "'");
                        }
                    }

                    public Instant getInstant(FlatDataRecordField field)
                    {
                        throw new IllegalArgumentException("Cannot get Instant value for field '" + field.getLabel() + "'");
                    }
                };
            }
        };
    }

    public FlatDataContext<Person> createContext()
    {
        FlatData schema = new FlatData().withSection(this.flatDataSection_Person());
        return new FlatDataContext<Person>(schema, "test::gen::TestSchema").withSectionFromObjectFactory("Person", this::flatDataSection_Factory_Person);
    }
//
//    public FlatDataContext<Person> createContext()
//    {
//        FlatDataRecordType recordType = new FlatDataRecordType()
//                .withField("firstName", new FlatDataString(false))
//                .withField("lastName", new FlatDataString(false))
//                .withField("dateOfBirth", new FlatDataDate(true))
//                .withField("isAlive", new FlatDataBoolean(false))
//                .withField("heightInMeters", new FlatDataDecimal(false));
//
//        FlatDataSection personSection = new FlatDataSection("default", "DelimitedWithHeadings")
//                .withProperty("delimiter", ",")
//                .withRecordType(recordType);
//
//        FlatData schema = new FlatData().withSection(personSection);
//
//        return new FlatDataContext<Person>(schema, Person.FACTORY.getPureClassName())
//                .withSectionFromObjectFactory("default", PersonFactory::new);
//    }
//
//    private static class PersonFactory implements ObjectToParsedFlatData<Person>
//    {
//        private final FlatDataRecordField firstNameField;
//        private final FlatDataRecordField lastNameField;
//        private final FlatDataRecordField dateOfBirthField;
//        private final FlatDataRecordField isAliveField;
//        private final FlatDataRecordField heightInMetersField;
//
//        PersonFactory(FlatDataRecordType recordType)
//        {
//            firstNameField = findField(recordType, "firstName");
//            lastNameField = findField(recordType, "lastName");
//            dateOfBirthField = findField(recordType, "dateOfBirth");
//            isAliveField = findField(recordType, "isAlive");
//            heightInMetersField = findField(recordType, "heightInMeters");
//        }
//
//        public ParsedFlatData make(Person person)
//        {
//            return new ParsedFlatData()
//            {
//                @Override
//                public boolean hasStringValue(FlatDataRecordField field)
//                {
//                    if (field == firstNameField)
//                    {
//                        return person.getFirstName() != null;
//                    }
//                    else if (field == lastNameField)
//                    {
//                        return person.getLastName() != null;
//                    }
//                    return false;
//                }
//
//                @Override
//                public boolean hasBooleanValue(FlatDataRecordField field)
//                {
//                    if (field == isAliveField)
//                    {
//                        return true;
//                    }
//                    return false;
//                }
//
//                @Override
//                public boolean hasLongValue(FlatDataRecordField field)
//                {
//                    return false;
//                }
//
//                @Override
//                public boolean hasDoubleValue(FlatDataRecordField field)
//                {
//                    if (field == heightInMetersField)
//                    {
//                        return true;
//                    }
//                    return false;
//                }
//
//                @Override
//                public boolean hasBigDecimalValue(FlatDataRecordField field)
//                {
//                    return false;
//                }
//
//                @Override
//                public boolean hasLocalDateValue(FlatDataRecordField field)
//                {
//                    if (field == dateOfBirthField)
//                    {
//                        return person.getDateOfBirth() != null;
//                    }
//                    return false;
//                }
//
//                @Override
//                public boolean hasInstantValue(FlatDataRecordField field)
//                {
//                    return false;
//                }
//
//                @Override
//                public String getString(FlatDataRecordField field)
//                {
//                    if (field == firstNameField)
//                    {
//                        return person.getFirstName();
//                    }
//                    else if (field == lastNameField)
//                    {
//                        return person.getLastName();
//                    }
//                    throw new IllegalArgumentException("Cannot get String value for field '" + field.getLabel() + "'");
//                }
//
//                @Override
//                public boolean getBoolean(FlatDataRecordField field)
//                {
//                    if (field == isAliveField)
//                    {
//                        return person.getIsAlive();
//                    }
//                    throw new IllegalArgumentException("Cannot get boolean value for field '" + field.getLabel() + "'");
//                }
//
//                @Override
//                public long getLong(FlatDataRecordField field)
//                {
//                    throw new IllegalArgumentException("Cannot get long value for field '" + field.getLabel() + "'");
//                }
//
//                @Override
//                public double getDouble(FlatDataRecordField field)
//                {
//                    if (field == heightInMetersField)
//                    {
//                        return person.getHeightInMeters();
//                    }
//                    throw new IllegalArgumentException("Cannot get double value for field '" + field.getLabel() + "'");
//                }
//
//                @Override
//                public BigDecimal getBigDecimal(FlatDataRecordField field)
//                {
//                    throw new IllegalArgumentException("Cannot get BigDecimal value for field '" + field.getLabel() + "'");
//                }
//
//                @Override
//                public LocalDate getLocalDate(FlatDataRecordField field)
//                {
//                    if (field == dateOfBirthField)
//                    {
//                        return LocalDate.of(person.getDateOfBirth().getYear(), person.getDateOfBirth().getMonth(), person.getDateOfBirth().getDay());
//                    }
//                    throw new IllegalArgumentException("Cannot get LocalDate value for field '" + field.getLabel() + "'");
//                }
//
//                @Override
//                public Instant getInstant(FlatDataRecordField field)
//                {
//                    throw new IllegalArgumentException("Cannot get Instant value for field '" + field.getLabel() + "'");
//                }
//            };
//        }
//
//        private static FlatDataRecordField findField(FlatDataRecordType recordType, String name)
//        {
//            return recordType.getFields().stream().filter(f -> f.getLabel().equals(name)).findFirst().get();
//        }
//    }
}
