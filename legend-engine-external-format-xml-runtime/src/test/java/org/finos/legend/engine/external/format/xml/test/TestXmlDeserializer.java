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

package org.finos.legend.engine.external.format.xml.test;

import org.finos.legend.engine.external.format.xml.fixtures.ReadFirmWithoutSchema;
import org.finos.legend.engine.external.format.xml.read.DeserializeContext;
import org.finos.legend.engine.external.format.xml.read.XmlDataRecord;
import org.finos.legend.engine.external.format.xml.shared.XmlReader;
import org.finos.legend.engine.external.shared.runtime.fixtures.firmModel.Firm;
import org.finos.legend.engine.external.shared.runtime.fixtures.firmModel.Person;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TestXmlDeserializer
{
    @Test
    public void testDeserializeOneFirmWithAttributes()
    {
        XmlReader reader = reader("<firm name=\"Acme Inc.\" ranking=\"2\"></firm>");

        List<IChecked<Firm>> firms = new ArrayList<>();
        DeserializeContext<Firm> context = new DeserializeContext<>(reader, firms::add);
        ReadFirmWithoutSchema firmReader = new ReadFirmWithoutSchema();
        firmReader.read(context);

        Assert.assertFalse(reader.hasNext());
        Assert.assertEquals(1, firms.size());
        Assert.assertEquals(Collections.singletonList("Acme Inc."), firms.stream().map(IChecked::getValue).map(Firm::getName).collect(Collectors.toList()));
        Assert.assertEquals(Collections.singletonList(2L), firms.stream().map(IChecked::getValue).map(Firm::getRanking).collect(Collectors.toList()));

        List<XmlDataRecord> sources = firms.stream().map(IChecked::getSource).map(XmlDataRecord.class::cast).collect(Collectors.toList());
        Assert.assertEquals(Collections.singletonList("<firm name=\"Acme Inc.\" ranking=\"2\"/>"), sources.stream().map(XmlDataRecord::getRecord).collect(Collectors.toList()));
        Assert.assertEquals(Collections.singletonList(1L), sources.stream().map(XmlDataRecord::getNumber).collect(Collectors.toList()));
    }

    @Test
    public void testDeserializeFirmsWithAttributes()
    {
        XmlReader reader = reader(
                "<world>",
                "  <firms>",
                "    <firm name=\"Acme Inc.\" ranking=\"2\"/>",
                "    <firm name=\"Widget Engineering\" ranking=\"1\"/>",
                "    <firm name=\"Globex Corp\"/>",
                "  </firms>",
                "</world>"
        );

        List<IChecked<Firm>> firms = new ArrayList<>();
        DeserializeContext<Firm> context = new DeserializeContext<>(reader, firms::add);
        ReadFirmWithoutSchema firmReader = new ReadFirmWithoutSchema();
        firmReader.read(context);

        Assert.assertFalse(reader.hasNext());
        Assert.assertEquals(3, firms.size());
        Assert.assertEquals(Arrays.asList("Acme Inc.", "Widget Engineering", "Globex Corp"), firms.stream().map(IChecked::getValue).map(Firm::getName).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(2L, 1L, null), firms.stream().map(IChecked::getValue).map(Firm::getRanking).collect(Collectors.toList()));

        List<XmlDataRecord> sources = firms.stream().map(IChecked::getSource).map(XmlDataRecord.class::cast).collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("<firm name=\"Acme Inc.\" ranking=\"2\"/>", "<firm name=\"Widget Engineering\" ranking=\"1\"/>", "<firm name=\"Globex Corp\"/>"), sources.stream().map(XmlDataRecord::getRecord).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(1L, 2L, 3L), sources.stream().map(XmlDataRecord::getNumber).collect(Collectors.toList()));
    }

    @Test
    public void testDeserializeFirmsWithAttributesInError()
    {
        XmlReader reader = reader(
                "<world>",
                "  <firms>",
                "    <firm name=\"Acme Inc.\" ranking=\"2\"/>",
                "    <firm name=\"Widget Engineering\" ranking=\"A\"/>",
                "    <firm name=\"Globex Corp\"/>",
                "  </firms>",
                "</world>"
        );

        List<IChecked<Firm>> firms = new ArrayList<>();
        DeserializeContext<Firm> context = new DeserializeContext<>(reader, firms::add);
        ReadFirmWithoutSchema firmReader = new ReadFirmWithoutSchema();
        firmReader.read(context);

        Assert.assertFalse(reader.hasNext());
        Assert.assertEquals(3, firms.size());
        Assert.assertEquals(Arrays.asList("Acme Inc.", "Widget Engineering", "Globex Corp"), firms.stream().map(IChecked::getValue).map(Firm::getName).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(2L, null, null), firms.stream().map(IChecked::getValue).map(Firm::getRanking).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(0, 1, 0), firms.stream().map(IChecked::getDefects).map(List::size).collect(Collectors.toList()));

        IDefect defect = firms.get(1).getDefects().get(0);
        Assert.assertEquals("Invalid long value: 'A' at /world[1]/firms[1]/firm[2][@ranking]", defect.getMessage());
        Assert.assertEquals("meta::external::shared::testpack::simple::Firm", defect.getRuleDefinerPath());

        List<XmlDataRecord> sources = firms.stream().map(IChecked::getSource).map(XmlDataRecord.class::cast).collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("<firm name=\"Acme Inc.\" ranking=\"2\"/>", "<firm name=\"Widget Engineering\" ranking=\"A\"/>", "<firm name=\"Globex Corp\"/>"), sources.stream().map(XmlDataRecord::getRecord).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(1L, 2L, 3L), sources.stream().map(XmlDataRecord::getNumber).collect(Collectors.toList()));
    }

    @Test
    public void testDeserializeFirmsWithElements()
    {
        XmlReader reader = reader(
                "<world>",
                "  <firms>",
                "    <firm>",
                "      <name>Acme Inc.</name>",
                "      <ranking>2</ranking>",
                "    </firm>",
                "    <firm>",
                "      <ranking>1</ranking>",
                "      <name>Widget Engineering</name>",
                "    </firm>",
                "    <firm>",
                "      <name>Globex Corp</name>",
                "    </firm>",
                "  </firms>",
                "</world>"
        );

        List<IChecked<Firm>> firms = new ArrayList<>();
        DeserializeContext<Firm> context = new DeserializeContext<>(reader, firms::add);
        ReadFirmWithoutSchema firmReader = new ReadFirmWithoutSchema();
        firmReader.read(context);

        Assert.assertFalse(reader.hasNext());
        Assert.assertEquals(3, firms.size());
        Assert.assertEquals(Arrays.asList("Acme Inc.", "Widget Engineering", "Globex Corp"), firms.stream().map(IChecked::getValue).map(Firm::getName).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(2L, 1L, null), firms.stream().map(IChecked::getValue).map(Firm::getRanking).collect(Collectors.toList()));

        List<XmlDataRecord> sources = firms.stream().map(IChecked::getSource).map(XmlDataRecord.class::cast).collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("<firm><name>Acme Inc.</name><ranking>2</ranking></firm>", "<firm><ranking>1</ranking><name>Widget Engineering</name></firm>", "<firm><name>Globex Corp</name></firm>"), sources.stream().map(XmlDataRecord::getRecord).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(1L, 2L, 3L), sources.stream().map(XmlDataRecord::getNumber).collect(Collectors.toList()));
    }

    @Test
    public void testDeserializeMalformedDocument()
    {
        XmlReader reader = reader(
                "<world>",
                "  <firms>",
                "    <firm>",
                "      <name>Acme Inc.</name>",
                "      <ranking>2</ranking>",
                "    </soft>",
                "    <firm>",
                "      <ranking>1</ranking>",
                "      <name>Widget Engineering</name>",
                "    </firm>",
                "    <firm>",
                "      <name>Globex Corp</name>",
                "    </firm>",
                "  </firms>",
                "</world>"
        );

        List<IChecked<Firm>> firms = new ArrayList<>();
        DeserializeContext<Firm> context = new DeserializeContext<>(reader, firms::add);
        ReadFirmWithoutSchema firmReader = new ReadFirmWithoutSchema();
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () -> firmReader.read(context));
        Assert.assertEquals(String.format("Unexpected close tag </soft>; expected </firm>.%n at [row,col,system-id]: [6,10,\"executor:test\"]"), e.getMessage());
        Assert.assertEquals(Collections.emptyList(), firms);
    }

    @Test
    public void testDeserializeFirmWithEmployeesWithAttributes()
    {
        XmlReader reader = reader(
                "<firm name=\"Acme Inc.\" ranking=\"2\">",
                "  <employees firstName=\"John\" lastName=\"Doe\" dateOfBirth=\"1991-02-10\" isAlive=\"true\" heightInMeters=\"1.78\"/>",
                "  <employees firstName=\"Fred\" lastName=\"Bloggs\" dateOfBirth=\"1983-12-13\" isAlive=\"false\" heightInMeters=\"1.65\"/>",
                "</firm>"
        );

        List<IChecked<Firm>> firms = new ArrayList<>();
        DeserializeContext<Firm> context = new DeserializeContext<>(reader, firms::add);
        ReadFirmWithoutSchema firmReader = new ReadFirmWithoutSchema();
        firmReader.read(context);

        Assert.assertFalse(reader.hasNext());
        Assert.assertEquals(1, firms.size());
        Firm firm = firms.get(0).getValue();
        Assert.assertEquals("Acme Inc.", firm.getName());
        Assert.assertEquals(2L, firm.getRanking().longValue());
        Assert.assertEquals(2, firm.getEmployees().size());
        Assert.assertEquals(Arrays.asList("John","Fred"), firm.getEmployees().stream().map(Person::getFirstName).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList("Doe","Bloggs"), firm.getEmployees().stream().map(Person::getLastName).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList("1991-02-10", "1983-12-13"), firm.getEmployees().stream().map(Person::getDateOfBirth).map(Object::toString).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(true, false), firm.getEmployees().stream().map(Person::getIsAlive).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(1.78, 1.65), firm.getEmployees().stream().map(Person::getHeightInMeters).collect(Collectors.toList()));

        List<XmlDataRecord> sources = firms.stream().map(IChecked::getSource).map(XmlDataRecord.class::cast).collect(Collectors.toList());
        Assert.assertEquals(Collections.singletonList("<firm name=\"Acme Inc.\" ranking=\"2\"><employees firstName=\"John\" lastName=\"Doe\" dateOfBirth=\"1991-02-10\" isAlive=\"true\" heightInMeters=\"1.78\"/><employees firstName=\"Fred\" lastName=\"Bloggs\" dateOfBirth=\"1983-12-13\" isAlive=\"false\" heightInMeters=\"1.65\"/></firm>"), sources.stream().map(XmlDataRecord::getRecord).collect(Collectors.toList()));
        Assert.assertEquals(Collections.singletonList(1L), sources.stream().map(XmlDataRecord::getNumber).collect(Collectors.toList()));
    }

    @Test
    public void testDeserializeFirmWithEmployeesWithElements()
    {
        XmlReader reader = reader(
                "<firm name=\"Acme Inc.\" ranking=\"2\">",
                "  <name>Acme Inc.</name>",
                "  <ranking>2</ranking>",
                "  <employees>",
                "    <firstName>John</firstName>",
                "    <lastName>Doe</lastName>",
                "    <dateOfBirth>1991-02-10</dateOfBirth>",
                "    <isAlive>true</isAlive>",
                "    <heightInMeters>1.78</heightInMeters>",
                "  </employees>",
                "  <employees>",
                "    <firstName>Fred</firstName>",
                "    <lastName>Bloggs</lastName>",
                "    <dateOfBirth>1983-12-13</dateOfBirth>",
                "    <isAlive>false</isAlive>",
                "    <heightInMeters>1.65</heightInMeters>",
                "  </employees>",
                "</firm>"
        );

        List<IChecked<Firm>> firms = new ArrayList<>();
        DeserializeContext<Firm> context = new DeserializeContext<>(reader, firms::add);
        ReadFirmWithoutSchema firmReader = new ReadFirmWithoutSchema();
        firmReader.read(context);

        Assert.assertFalse(reader.hasNext());
        Assert.assertEquals(1, firms.size());
        Firm firm = firms.get(0).getValue();
        Assert.assertEquals("Acme Inc.", firm.getName());
        Assert.assertEquals(2L, firm.getRanking().longValue());
        Assert.assertEquals(2, firm.getEmployees().size());
        Assert.assertEquals(Arrays.asList("John","Fred"), firm.getEmployees().stream().map(Person::getFirstName).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList("Doe","Bloggs"), firm.getEmployees().stream().map(Person::getLastName).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList("1991-02-10", "1983-12-13"), firm.getEmployees().stream().map(Person::getDateOfBirth).map(Object::toString).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(true, false), firm.getEmployees().stream().map(Person::getIsAlive).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(1.78, 1.65), firm.getEmployees().stream().map(Person::getHeightInMeters).collect(Collectors.toList()));
    }

    @Test
    public void testDeserializeFirmWithEmployeesWithElementsNested()
    {
        XmlReader reader = reader(
                "<firm name=\"Acme Inc.\" ranking=\"2\">",
                "  <name>Acme Inc.</name>",
                "  <ranking>2</ranking>",
                "  <employees>",
                "    <employee>",
                "      <firstName>John</firstName>",
                "      <lastName>Doe</lastName>",
                "      <dateOfBirth>1991-02-10</dateOfBirth>",
                "      <isAlive>true</isAlive>",
                "      <heightInMeters>1.78</heightInMeters>",
                "    </employee>",
                "    <employee>",
                "      <firstName>Fred</firstName>",
                "      <lastName>Bloggs</lastName>",
                "      <dateOfBirth>1983-12-13</dateOfBirth>",
                "      <isAlive>false</isAlive>",
                "      <heightInMeters>1.65</heightInMeters>",
                "    </employee>",
                "  </employees>",
                "</firm>"
        );

        List<IChecked<Firm>> firms = new ArrayList<>();
        DeserializeContext<Firm> context = new DeserializeContext<>(reader, firms::add);
        ReadFirmWithoutSchema firmReader = new ReadFirmWithoutSchema();
        firmReader.read(context);

        Assert.assertFalse(reader.hasNext());
        Assert.assertEquals(1, firms.size());
        Firm firm = firms.get(0).getValue();
        Assert.assertEquals("Acme Inc.", firm.getName());
        Assert.assertEquals(2L, firm.getRanking().longValue());
        Assert.assertEquals(2, firm.getEmployees().size());
        Assert.assertEquals(Arrays.asList("John","Fred"), firm.getEmployees().stream().map(Person::getFirstName).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList("Doe","Bloggs"), firm.getEmployees().stream().map(Person::getLastName).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList("1991-02-10", "1983-12-13"), firm.getEmployees().stream().map(Person::getDateOfBirth).map(Object::toString).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(true, false), firm.getEmployees().stream().map(Person::getIsAlive).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(1.78, 1.65), firm.getEmployees().stream().map(Person::getHeightInMeters).collect(Collectors.toList()));
    }

    private XmlReader reader(String... lines)
    {
        InputStream stream = new ByteArrayInputStream(String.join("\n", lines).getBytes());
        return XmlReader.newReader(stream, "executor:test");
    }
}