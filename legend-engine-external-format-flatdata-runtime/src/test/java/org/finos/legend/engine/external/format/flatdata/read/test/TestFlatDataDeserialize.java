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
import org.finos.legend.engine.external.format.flatdata.read.FlatDataReader;
import org.finos.legend.engine.external.format.flatdata.read.fixtures.ReadPerson;
import org.finos.legend.engine.external.format.test.fixture.app.meta.external.shared.format.executionPlan.testing.model.firm.Person;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestFlatDataDeserialize
{
    @Test
    public void testDeserializePeopleCsv()
    {
        List<IChecked<Person>> people = new ArrayList();
        FlatDataContext<Person> context = new ReadPerson().createContext();
        FlatDataReader<Person> deserializer = new FlatDataReader<>(context, resourceStream("queries/peopleWithExactHeadings.csv"));
        deserializer.readData(people::add);

        Assert.assertEquals(3, people.size());
        Assert.assertEquals(0, people.get(0).getDefects().size());
        Assert.assertEquals(0, people.get(1).getDefects().size());
        Assert.assertEquals(0, people.get(2).getDefects().size());

        Person p1 = people.get(0).getValue();
        Assert.assertEquals("Jason", p1.getFirstName());
        Assert.assertEquals("Schlichting", p1.getLastName());
        Assert.assertEquals("1968-03-05", p1.getDateOfBirth().toString());
        Assert.assertTrue(p1.getIsAlive());
        Assert.assertEquals(1.82, p1.getHeightInMeters(), 0.000000001);

        Person p2 = people.get(1).getValue();
        Assert.assertEquals("Nancy", p2.getFirstName());
        Assert.assertEquals("Fraher", p2.getLastName());
        Assert.assertEquals("1970-12-13", p2.getDateOfBirth().toString());
        Assert.assertFalse(p2.getIsAlive());
        Assert.assertEquals(1.71, p2.getHeightInMeters(), 0.000000001);

        Person p3 = people.get(2).getValue();
        Assert.assertEquals("Müller", p3.getFirstName());
        Assert.assertEquals("Weiß", p3.getLastName());
        Assert.assertEquals("1985-01-12", p3.getDateOfBirth().toString());
        Assert.assertTrue(p3.getIsAlive());
        Assert.assertEquals(1.9, p3.getHeightInMeters(), 0.000000001);
    }

    private InputStream resourceStream(String path)
    {
        try
        {
            return Objects.requireNonNull(TestFlatDataDeserialize.class.getClassLoader().getResource(path), "Failed to get URL for resource " + path).openStream();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
