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
import org.finos.legend.engine.external.format.flatdata.read.IFlatDataDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataString;
import org.finos.legend.engine.external.format.test.fixture.app.meta.external.shared.format.executionPlan.tests.model.firm.Person;
import org.finos.legend.engine.external.format.test.fixture.plan.node.meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataBooleanAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataDoubleAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataObjectAdder;

import java.time.temporal.Temporal;

public class ReadPerson implements IFlatDataDeserializeExecutionNodeSpecifics<Person>
{

    public FlatDataSection flatDataSection_Person()
    {
        FlatDataRecordType recordType = new FlatDataRecordType().withField("firstName", new FlatDataString(false)).withField("lastName", new FlatDataString(false)).withField("dateOfBirth", new FlatDataDate(true)).withField("isAlive", new FlatDataBoolean(false)).withField("heightInMeters", new FlatDataDecimal(false));
        return new FlatDataSection("Person", "DelimitedWithHeadings").withProperty("scope.untilEof", true).withProperty("delimiter", ",").withRecordType(recordType);
    }

    public ParsedFlatDataToObject<meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl> flatDataSection_Factory_Person(FlatDataRecordType recordType)
    {
        FlatDataRecordField firstNameField = recordType.getFields().stream().filter((FlatDataRecordField f) -> f.getLabel().equals("firstName")).findFirst().get();
        FlatDataRecordField lastNameField = recordType.getFields().stream().filter((FlatDataRecordField f) -> f.getLabel().equals("lastName")).findFirst().get();
        FlatDataRecordField dateOfBirthField = recordType.getFields().stream().filter((FlatDataRecordField f) -> f.getLabel().equals("dateOfBirth")).findFirst().get();
        FlatDataRecordField isAliveField = recordType.getFields().stream().filter((FlatDataRecordField f) -> f.getLabel().equals("isAlive")).findFirst().get();
        FlatDataRecordField heightInMetersField = recordType.getFields().stream().filter((FlatDataRecordField f) -> f.getLabel().equals("heightInMeters")).findFirst().get();

        ExternalDataObjectAdder<meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl, String> firstNameAdder = (ExternalDataObjectAdder<meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl, String>) meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl._getAdderForProperty("firstName");
        ExternalDataObjectAdder<meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl, String> lastNameAdder = (ExternalDataObjectAdder<meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl, String>) meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl._getAdderForProperty("lastName");
        ExternalDataObjectAdder<meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl, Temporal> dateOfBirthAdder = (ExternalDataObjectAdder<meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl, Temporal>) meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl._getAdderForProperty("dateOfBirth");
        ExternalDataBooleanAdder<meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl> isAliveAdder = (ExternalDataBooleanAdder<meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl>) meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl._getAdderForProperty("isAlive");
        ExternalDataDoubleAdder<meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl> heightInMetersAdder = (ExternalDataDoubleAdder<meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl>) meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl._getAdderForProperty("heightInMeters");

        return new ParsedFlatDataToObject<meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl>()
        {
            public meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl make(ParsedFlatData parsedFlatData)
            {
                meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl result = new meta_external_shared_format_executionPlan_tests_model_firm_Person_Impl();
                if (parsedFlatData.hasStringValue(firstNameField))
                {
                    firstNameAdder.addTo(result, parsedFlatData.getString(firstNameField));
                }
                if (parsedFlatData.hasStringValue(lastNameField))
                {
                    lastNameAdder.addTo(result, parsedFlatData.getString(lastNameField));
                }
                if (parsedFlatData.hasLocalDateValue(dateOfBirthField))
                {
                    dateOfBirthAdder.addTo(result, parsedFlatData.getLocalDate(dateOfBirthField));
                }
                if (parsedFlatData.hasBooleanValue(isAliveField))
                {
                    isAliveAdder.addTo(result, parsedFlatData.getBoolean(isAliveField));
                }
                if (parsedFlatData.hasDoubleValue(heightInMetersField))
                {
                    heightInMetersAdder.addTo(result, parsedFlatData.getDouble(heightInMetersField));
                }
                return result;
            }

            @Override
            public boolean isReturnable()
            {
                return true;
            }
        };
    }

    public FlatDataContext<Person> createContext()
    {
        FlatData schema = new FlatData().withSection(this.flatDataSection_Person());
        return new FlatDataContext<Person>(schema, "test::gen::TestSchema").withSectionToObjectFactory("Person", this::flatDataSection_Factory_Person);
    }
}
