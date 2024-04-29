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
import org.finos.legend.engine.external.format.flatdata.driver.spi.ParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatData;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataString;
import org.finos.legend.engine.external.format.flatdata.read.IFlatDataDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.test.fixture.app.meta.external.format.shared.executionPlan.tests.model.firm.Person;
import org.finos.legend.engine.external.format.test.fixture.plan.node.meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataBooleanAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataDoubleAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataObjectAdder;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.EnforcementLevel;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ReadPerson implements IFlatDataDeserializeExecutionNodeSpecifics<Person>
{
    private long maximumSchemaObjectSize = 0L;

    public void setMaximumSchemaObjectSize(long maximumSchemaObjectSize)
    {
        this.maximumSchemaObjectSize = maximumSchemaObjectSize;
    }

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

    public ParsedFlatDataToObject<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl> flatDataSection_Factory_Person(FlatDataRecordType recordType)
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
        ExternalDataObjectAdder<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl, String> adder0 = (ExternalDataObjectAdder<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl, String>) meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl._getAdderForProperty("firstName");
        ExternalDataObjectAdder<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl, String> adder1 = (ExternalDataObjectAdder<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl, String>) meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl._getAdderForProperty("lastName");
        ExternalDataObjectAdder<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl, Temporal> adder2 = (ExternalDataObjectAdder<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl, Temporal>) meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl._getAdderForProperty("dateOfBirth");
        ExternalDataBooleanAdder<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl> adder3 = (ExternalDataBooleanAdder<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl>) meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl._getAdderForProperty("isAlive");
        ExternalDataDoubleAdder<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl> adder4 = (ExternalDataDoubleAdder<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl>) meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl._getAdderForProperty("heightInMeters");

        return new ParsedFlatDataToObject<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl>()
        {
            public meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl make(ParsedFlatData parsedFlatData)
            {
                IChecked<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl> checked = this.makeChecked(parsedFlatData);
                if (checked.getDefects()
                        .stream()
                        .anyMatch((IDefect d) -> d.getEnforcementLevel() == EnforcementLevel.Critical))
                {
                    throw new IllegalStateException(checked.getDefects()
                            .stream()
                            .map((IDefect d) -> d.getMessage())
                            .filter((String x) -> x != null)
                            .collect(Collectors.joining("\n")));
                }
                return checked.getValue();
            }

            public IChecked<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl> makeChecked(ParsedFlatData parsedFlatData)
            {
                List<IDefect> defects = new ArrayList<IDefect>();
                meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl result = new meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl();
                if (parsedFlatData.hasStringValue(field0))
                {
                    try
                    {
                        adder0.addTo(result, parsedFlatData.getString(field0));
                    }
                    catch (Exception e)
                    {
                        defects.add(BasicDefect.newClassStructureDefect(e.getMessage(),
                                "test::firm::model::Person"));
                    }
                }
                if (parsedFlatData.hasStringValue(field1))
                {
                    try
                    {
                        adder1.addTo(result, parsedFlatData.getString(field1));
                    }
                    catch (Exception e)
                    {
                        defects.add(BasicDefect.newClassStructureDefect(e.getMessage(),
                                "test::firm::model::Person"));
                    }
                }
                if (parsedFlatData.hasLocalDateValue(field2))
                {
                    try
                    {
                        adder2.addTo(result, parsedFlatData.getLocalDate(field2));
                    }
                    catch (Exception e)
                    {
                        defects.add(BasicDefect.newClassStructureDefect(e.getMessage(),
                                "test::firm::model::Person"));
                    }
                }
                if (parsedFlatData.hasBooleanValue(field3))
                {
                    try
                    {
                        adder3.addTo(result, parsedFlatData.getBoolean(field3));
                    }
                    catch (Exception e)
                    {
                        defects.add(BasicDefect.newClassStructureDefect(e.getMessage(),
                                "test::firm::model::Person"));
                    }
                }
                if (parsedFlatData.hasDoubleValue(field4))
                {
                    try
                    {
                        adder4.addTo(result, parsedFlatData.getDouble(field4));
                    }
                    catch (Exception e)
                    {
                        defects.add(BasicDefect.newClassStructureDefect(e.getMessage(),
                                "test::firm::model::Person"));
                    }
                }
                return new IChecked<meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl>()
                {
                    public List<IDefect> getDefects()
                    {
                        return defects;
                    }

                    public Object getSource()
                    {
                        return parsedFlatData;
                    }

                    public meta_external_format_shared_executionPlan_tests_model_firm_Person_Impl getValue()
                    {
                        return result;
                    }
                };
            }

            public boolean isReturnable()
            {
                return true;
            }
        };
    }

    public FlatDataContext<Person> createContext()
    {
        FlatData schema = new FlatData();
        schema.sections = Arrays.<FlatDataSection>asList(this.flatDataSection_Person());
        return new FlatDataContext<Person>(schema, "test::gen::TestSchemaSet")
                .withSectionToObjectFactory("Person",
                        this::flatDataSection_Factory_Person);
    }
}
