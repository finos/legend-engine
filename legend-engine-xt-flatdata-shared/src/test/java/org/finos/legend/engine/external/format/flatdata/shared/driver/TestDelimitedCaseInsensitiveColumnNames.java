package org.finos.legend.engine.external.format.flatdata.shared.driver;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TestDelimitedCaseInsensitiveColumnNames extends AbstractDriverTest
{
    @Test
    public void allUpperCase()
    {
        test("AGE,NAME,TITLE");
    }

    @Test
    public void allLowerCase()
    {
        test("age,name,title");
    }

    @Test
    public void mixedCases()
    {
        test("Age,Name,tItLe");
    }

    private void test(String headings)
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "  columnsHeadingsAreCaseInsensitive;\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME          : STRING;\n" +
                "    AGE           : INTEGER;\n" +
                "    TITLE         : STRING;\n" +
                "  }\n" +
                "}\n");

        String data = data("\n", headings, "25,Alex,Other");

        List<IChecked<TestDelimitedMultiCharacterDelimiter.Person>> records = deserialize(TestDelimitedMultiCharacterDelimiter.Person.class, flatData, data);

        records.forEach(this::assertNoDefects);

        List<ExpectedRecordValue> expectedRecordValues1 = Arrays.asList(
                rValue("AGE", "25"),
                rValue("NAME", "Alex"),
                rValue("TITLE", "Other")
        );

        assertSource(1, 2, "25,Alex,Other", expectedRecordValues1, records.get(0));
        TestDelimitedMultiCharacterDelimiter.Person p1 = records.get(0).getValue();
        Assert.assertEquals(25L, p1.AGE);
        Assert.assertEquals("Alex", p1.NAME);
        Assert.assertEquals("Other", p1.TITLE);
    }

    public static class Person
    {
        public String NAME;
        public long AGE;
        public String TITLE;
    }
}
