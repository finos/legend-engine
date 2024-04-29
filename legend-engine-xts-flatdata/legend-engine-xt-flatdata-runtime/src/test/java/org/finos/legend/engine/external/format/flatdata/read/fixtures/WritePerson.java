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

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.external.format.flatdata.FlatDataContext;
import org.finos.legend.engine.external.format.flatdata.driver.spi.ObjectToParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.driver.spi.ParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatData;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataString;
import org.finos.legend.engine.external.format.flatdata.write.IFlatDataSerializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.test.fixture.app.meta.external.format.shared.executionPlan.tests.model.firm.Person;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;

public class WritePerson implements IFlatDataSerializeExecutionNodeSpecifics<Person>
{
    public FlatDataSection flatDataSection_Person()
    {
        FlatDataSection section = new FlatDataSection();
        section.name = "Person";
        section.driverId = "DelimitedWithHeadings";
        FlatDataProperty flatDataProperty_0 = new FlatDataProperty();
        flatDataProperty_0.name = "scope.untilEof";
        flatDataProperty_0.values = Arrays.asList(true);
        FlatDataProperty flatDataProperty_1 = new FlatDataProperty();
        flatDataProperty_1.name = "delimiter";
        flatDataProperty_1.values = Arrays.asList(",");
        section.sectionProperties = Arrays
                .<FlatDataProperty>asList(flatDataProperty_0,
                        flatDataProperty_1);
        FlatDataRecordType recordType = new FlatDataRecordType();
        recordType.fields = Lists.mutable.empty();
        FlatDataRecordField flatDataRecordField_0 = new FlatDataRecordField();
        flatDataRecordField_0.label = "firstName";
        FlatDataString flatDataRecordField_0Type = new FlatDataString();
        flatDataRecordField_0Type.optional = false;
        flatDataRecordField_0.type = flatDataRecordField_0Type;
        recordType.fields.add(flatDataRecordField_0);
        FlatDataRecordField flatDataRecordField_1 = new FlatDataRecordField();
        flatDataRecordField_1.label = "lastName";
        FlatDataString flatDataRecordField_1Type = new FlatDataString();
        flatDataRecordField_1Type.optional = false;
        flatDataRecordField_1.type = flatDataRecordField_1Type;
        recordType.fields.add(flatDataRecordField_1);
        FlatDataRecordField flatDataRecordField_2 = new FlatDataRecordField();
        flatDataRecordField_2.label = "dateOfBirth";
        FlatDataDate flatDataRecordField_2Type = new FlatDataDate();
        flatDataRecordField_2Type.optional = true;
        flatDataRecordField_2Type.format = Arrays.<String>asList();
        flatDataRecordField_2.type = flatDataRecordField_2Type;
        recordType.fields.add(flatDataRecordField_2);
        FlatDataRecordField flatDataRecordField_3 = new FlatDataRecordField();
        flatDataRecordField_3.label = "isAlive";
        FlatDataBoolean flatDataRecordField_3Type = new FlatDataBoolean();
        flatDataRecordField_3Type.optional = false;
        flatDataRecordField_3.type = flatDataRecordField_3Type;
        recordType.fields.add(flatDataRecordField_3);
        FlatDataRecordField flatDataRecordField_4 = new FlatDataRecordField();
        flatDataRecordField_4.label = "heightInMeters";
        FlatDataDecimal flatDataRecordField_4Type = new FlatDataDecimal();
        flatDataRecordField_4Type.optional = false;
        flatDataRecordField_4.type = flatDataRecordField_4Type;
        recordType.fields.add(flatDataRecordField_4);
        section.recordType = recordType;
        return section;
    }

    public ObjectToParsedFlatData<Person> flatDataSection_Factory_Person(FlatDataRecordType recordType)
    {
        FlatDataRecordField field0 = recordType.fields
                .stream()
                .filter((FlatDataRecordField f) -> f.label.equals("firstName"))
                .findFirst()
                .get();
        FlatDataRecordField field1 = recordType.fields
                .stream()
                .filter((FlatDataRecordField f) -> f.label.equals("lastName"))
                .findFirst()
                .get();
        FlatDataRecordField field2 = recordType.fields
                .stream()
                .filter((FlatDataRecordField f) -> f.label.equals("dateOfBirth"))
                .findFirst()
                .get();
        FlatDataRecordField field3 = recordType.fields
                .stream()
                .filter((FlatDataRecordField f) -> f.label.equals("isAlive"))
                .findFirst()
                .get();
        FlatDataRecordField field4 = recordType.fields
                .stream()
                .filter((FlatDataRecordField f) -> f.label.equals("heightInMeters"))
                .findFirst()
                .get();
        return new ObjectToParsedFlatData<Person>()
        {
            public ParsedFlatData make(Person value)
            {
                return new ParsedFlatData()
                {
                    public boolean hasStringValue(FlatDataRecordField field)
                    {
                        if (field.equals(field0))
                        {
                            return true;
                        }
                        else if (field.equals(field1))
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
                        if (field.equals(field3))
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
                        if (field.equals(field4))
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
                        if (field.equals(field2))
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
                        if (field.equals(field0))
                        {
                            return value.getFirstName();
                        }
                        else if (field.equals(field1))
                        {
                            return value.getLastName();
                        }
                        else
                        {
                            throw new IllegalArgumentException("Cannot get String value for field '" + field.label + "'");
                        }
                    }

                    public boolean getBoolean(FlatDataRecordField field)
                    {
                        if (field.equals(field3))
                        {
                            return value.getIsAlive();
                        }
                        else
                        {
                            throw new IllegalArgumentException("Cannot get boolean value for field '" + field.label + "'");
                        }
                    }

                    public long getLong(FlatDataRecordField field)
                    {
                        throw new IllegalArgumentException("Cannot get long value for field '" + field.label + "'");
                    }

                    public double getDouble(FlatDataRecordField field)
                    {
                        if (field.equals(field4))
                        {
                            return value.getHeightInMeters();
                        }
                        else
                        {
                            throw new IllegalArgumentException("Cannot get double value for field '" + field.label + "'");
                        }
                    }

                    public BigDecimal getBigDecimal(FlatDataRecordField field)
                    {
                        throw new IllegalArgumentException("Cannot get BigDecimal value for field '" + field.label + "'");
                    }

                    public LocalDate getLocalDate(FlatDataRecordField field)
                    {
                        if (field.equals(field2))
                        {
                            return value.getDateOfBirth().toLocalDate();
                        }
                        else
                        {
                            throw new IllegalArgumentException("Cannot get LocalDate value for field '" + field.label + "'");
                        }
                    }

                    public Instant getInstant(FlatDataRecordField field)
                    {
                        throw new IllegalArgumentException("Cannot get Instant value for field '" + field.label + "'");
                    }
                };
            }
        };
    }

    public FlatDataContext<Person> createContext()
    {
        FlatData schema = new FlatData();
        schema.sections = Arrays.<FlatDataSection>asList(this.flatDataSection_Person());
        return new FlatDataContext<Person>(schema, "test::gen::TestSchemaSet")
                .withSectionFromObjectFactory("Person",
                        this::flatDataSection_Factory_Person);
    }
}
