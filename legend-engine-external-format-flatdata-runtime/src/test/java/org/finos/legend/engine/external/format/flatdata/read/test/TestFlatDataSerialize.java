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

package org.finos.legend.engine.external.format.flatdata.read.test;

import org.finos.legend.engine.external.format.flatdata.FlatDataContext;
import org.finos.legend.engine.external.format.flatdata.read.fixtures.WritePerson;
import org.finos.legend.engine.external.format.flatdata.write.FlatDataWriter;
import org.finos.legend.engine.external.format.test.fixture.app.meta.external.shared.format.executionPlan.testing.model.firm.Person;
import org.finos.legend.engine.external.format.test.fixture.plan.node.meta_external_shared_format_executionPlan_testing_model_firm_Person_Impl;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;

public class TestFlatDataSerialize
{
    @Test
    public void testSerializePeopleCsv()
    {
        meta_external_shared_format_executionPlan_testing_model_firm_Person_Impl jason = new meta_external_shared_format_executionPlan_testing_model_firm_Person_Impl();
        jason._firstNameAdd("Jason");
        jason._lastNameAdd("Schlichting");
        jason._dateOfBirthAdd(LocalDate.of(1968, 3, 5));
        jason._isAliveAdd(true);
        jason._heightInMetersAdd(1.82);

        meta_external_shared_format_executionPlan_testing_model_firm_Person_Impl nancy = new meta_external_shared_format_executionPlan_testing_model_firm_Person_Impl();
        nancy._firstNameAdd("Nancy");
        nancy._lastNameAdd("Fraher");
        nancy._dateOfBirthAdd(LocalDate.of(1970, 12, 13));
        nancy._isAliveAdd(false);
        nancy._heightInMetersAdd(1.71);

        Stream<Person> people = Stream.of(jason, nancy);
        FlatDataContext<Person> context = new WritePerson().createContext();
        FlatDataWriter<Person> serializer = new FlatDataWriter<>(context, people);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        serializer.writeData(stream);

        Assert.assertEquals(resourceAsString("queries/peopleWithExactHeadings.csv"), stream.toString());
    }

    private String resourceAsString(String path)
    {
        try
        {
            return new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(path), "Failed to get resource " + path).toURI())));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
