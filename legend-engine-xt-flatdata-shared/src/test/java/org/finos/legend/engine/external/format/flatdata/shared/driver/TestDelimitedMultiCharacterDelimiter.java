package org.finos.legend.engine.external.format.flatdata.shared.driver;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TestDelimitedMultiCharacterDelimiter extends AbstractDriverTest {

    @Test
    public void canReadErrorFree()
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : '~!@';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME          : STRING;\n" +
                "    AGE           : INTEGER;\n" +
                "    TITLE         : STRING;\n" +
                "  }\n" +
                "}\n");

        String data = data("\n",
                "AGE~!@NAME~!@TITLE",
                "25~!@Alex~!@Other",
                "26~!@Brad~!@Vice President",
                "27~!@Karl~!@Managing Director"
        );

        List<IChecked<Person>> records = deserialize(Person.class, flatData, data);

        records.forEach(this::assertNoDefects);


        List<ExpectedRecordValue> expectedRecordValues1 = Arrays.asList(
                rValue("AGE", "25"),
                rValue("NAME", "Alex"),
                rValue("TITLE", "Other")
        );

        List<ExpectedRecordValue> expectedRecordValues2 = Arrays.asList(
                rValue("AGE", "26"),
                rValue("NAME", "Brad"),
                rValue("TITLE", "Vice President")
        );

        List<ExpectedRecordValue> expectedRecordValues3 = Arrays.asList(
                rValue("AGE", "27"),
                rValue("NAME", "Karl"),
                rValue("TITLE", "Managing Director")
        );

        assertSource(1, 2, "25~!@Alex~!@Other", expectedRecordValues1, records.get(0));
        Person p1 = records.get(0).getValue();
        Assert.assertEquals(25L, p1.AGE);
        Assert.assertEquals("Alex", p1.NAME);
        Assert.assertEquals("Other", p1.TITLE);

        assertSource(2, 3, "26~!@Brad~!@Vice President", expectedRecordValues2, records.get(1));
        Person p2 = records.get(1).getValue();
        Assert.assertEquals(26L, p2.AGE);
        Assert.assertEquals("Brad", p2.NAME);
        Assert.assertEquals("Vice President", p2.TITLE);

        assertSource(3, 4, "27~!@Karl~!@Managing Director", expectedRecordValues3, records.get(2));
        Person p3 = records.get(2).getValue();
        Assert.assertEquals(27L, p3.AGE);
        Assert.assertEquals("Karl", p3.NAME);
        Assert.assertEquals("Managing Director", p3.TITLE);
    }

    public static class Person
    {
        public String NAME;
        public long AGE;
        public String TITLE;
    }
}
