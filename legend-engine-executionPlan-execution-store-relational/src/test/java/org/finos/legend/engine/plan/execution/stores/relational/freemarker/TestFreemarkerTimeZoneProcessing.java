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

package org.finos.legend.engine.plan.execution.stores.relational.freemarker;

import freemarker.core.TemplateDateFormatFactory;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.result.freemarker.PlanDateParameterDateFormatFactory;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

public class TestFreemarkerTimeZoneProcessing
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static String getTemplateFunctions()
    {
        return "<#function UTCtoTZ tz paramDate>" +
                "    <#return (tz+\" \"+paramDate)?date.@alloyDate>" +
                "</#function>" +
                "<#function renderCollectionWithTz collection timeZone separator prefix suffix defaultValue>"+
                "<#assign result = [] />"+
                "<#list collection as c>"+
                "<#assign result = [prefix + (timeZone+\" \"+c)?date.@alloyDate + suffix] + result>"+
                "</#list>"+
                "<#return result?reverse?join(separator, defaultValue)>"+
                "</#function>";
    }

    private static Template getTemplateConfiguredForDateProcessing(String textToProcess) throws IOException
    {
        Template t = new Template("sqlTemplate", new StringReader(getTemplateFunctions() + textToProcess));
        Map<String, TemplateDateFormatFactory> customDateFormats = Maps.mutable.with("alloyDate", PlanDateParameterDateFormatFactory.INSTANCE);
        t.setCustomDateFormats(customDateFormats);
        t.setDateFormat("@alloyDate");
        return t;
    }

    private static String processTemplate(String textToProcess, MutableMap<String, Object> freeMarkerParameters) throws IOException, TemplateException
    {
        StringWriter stringWriter = new StringWriter();
        Template t = getTemplateConfiguredForDateProcessing(textToProcess);
        t.process(freeMarkerParameters, stringWriter);
        return stringWriter.toString();
    }

    @Test
    public void testDateConstantTimeZoneNoConversion() throws Exception
    {
        MutableMap<String, Object> freeMarkerDateParameters = Maps.mutable.with("dateParam", "2018-10-15");
        Assert.assertEquals("2018-10-15", processDateConstantTimeZoneNoConversion("America/New_York", freeMarkerDateParameters));
        Assert.assertEquals("2018-10-15", processDateConstantTimeZoneNoConversion("UTC", freeMarkerDateParameters));
        Assert.assertEquals("2018-10-15", processDateConstantTimeZoneNoConversion("GMT", freeMarkerDateParameters));
    }

    @Test
    public void testConstantDateCollectionTimeZoneNoConversion() throws Exception
    {
        MutableMap<String, Object> freeMarkerDateParameters = Maps.mutable.with("dateParam", Lists.mutable.with("2018-10-15", "2018-10-16"));
        Assert.assertEquals("convert(DATE, '2018-10-15', 101),convert(DATE, '2018-10-16', 101)", processDateCollectionWithTimeZone("America/New_York", freeMarkerDateParameters));
        Assert.assertEquals("convert(DATE, '2018-10-15', 101),convert(DATE, '2018-10-16', 101)", processDateCollectionWithTimeZone("UTC", freeMarkerDateParameters));
        Assert.assertEquals("convert(DATE, '2018-10-15', 101),convert(DATE, '2018-10-16', 101)", processDateCollectionWithTimeZone("GMT", freeMarkerDateParameters));
    }

    @Test
    public void testDateTimeConstantTimeZoneConversion() throws Exception
    {
        MutableMap<String, Object> freeMarkerDateParameters = Maps.mutable.with("dateParam", "2018-10-15T20:00:00");
        Assert.assertEquals("2018-10-15T16:00:00", processDateConstantTimeZoneNoConversion("America/New_York", freeMarkerDateParameters));
        Assert.assertEquals("2018-10-15T20:00:00", processDateConstantTimeZoneNoConversion("GMT", freeMarkerDateParameters));
        Assert.assertEquals("2018-10-15T20:00:00", processDateConstantTimeZoneNoConversion("UTC", freeMarkerDateParameters));
    }

    @Test
    public void testConstantDateTimeCollectionTimeZoneConversion() throws Exception
    {
        MutableMap<String, Object> freeMarkerDateParameters = Maps.mutable.with("dateParam", Lists.mutable.with("2018-10-15T20:00:00", "2018-10-16T20:00:00"));
        Assert.assertEquals("convert(DATETIME, '2018-10-15T16:00:00', 101),convert(DATETIME, '2018-10-16T16:00:00', 101)", processDateTimeCollectionWithTimeZone("America/New_York", freeMarkerDateParameters));
        Assert.assertEquals("convert(DATETIME, '2018-10-15T20:00:00', 101),convert(DATETIME, '2018-10-16T20:00:00', 101)", processDateTimeCollectionWithTimeZone("GMT", freeMarkerDateParameters));
        Assert.assertEquals("convert(DATETIME, '2018-10-15T20:00:00', 101),convert(DATETIME, '2018-10-16T20:00:00', 101)", processDateTimeCollectionWithTimeZone("UTC", freeMarkerDateParameters));
    }

    @Test
    public void testDateTimeConstantWithMsTimeZoneConversion() throws Exception
    {
        MutableMap<String, Object> freeMarkerDateParameters = Maps.mutable.with("dateParam", "2018-10-15T20:00:00.123");
        Assert.assertEquals("2018-10-15T16:00:00.123", processDateConstantTimeZoneNoConversion("America/New_York", freeMarkerDateParameters));
        Assert.assertEquals("2018-10-15T20:00:00.123", processDateConstantTimeZoneNoConversion("GMT", freeMarkerDateParameters));
        Assert.assertEquals("2018-10-15T20:00:00.123", processDateConstantTimeZoneNoConversion("UTC", freeMarkerDateParameters));
    }

    @Test
    public void testConstantDateTimeCollectionWithMsTimeZoneConversion() throws Exception
    {
        MutableMap<String, Object> freeMarkerDateParameters = Maps.mutable.with("dateParam", Lists.mutable.with("2018-10-15T20:00:00.123", "2018-10-16T20:00:00.123"));
        Assert.assertEquals("convert(DATETIME, '2018-10-15T16:00:00.123', 101),convert(DATETIME, '2018-10-16T16:00:00.123', 101)", processDateTimeCollectionWithTimeZone("America/New_York", freeMarkerDateParameters));
        Assert.assertEquals("convert(DATETIME, '2018-10-15T20:00:00.123', 101),convert(DATETIME, '2018-10-16T20:00:00.123', 101)", processDateTimeCollectionWithTimeZone("GMT", freeMarkerDateParameters));
        Assert.assertEquals("convert(DATETIME, '2018-10-15T20:00:00.123', 101),convert(DATETIME, '2018-10-16T20:00:00.123', 101)", processDateTimeCollectionWithTimeZone("UTC", freeMarkerDateParameters));
    }

    @Test
    public void testNoTimeZoneSpecifiedThrowsException() throws Exception
    {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Plan parsing error; unable to process Date: [] 2018-10-15T20:00:00.123, expecting: \\[(\\S+)\\]\\s(\\S+([\\sT]\\S+)?) e.g.: '[EST] + $date', where $date is of format: [yyyy-MM-dd, yyyy-MM-dd'T'HH:mm:ss, yyyy-MM-dd'T'HH:mm:ss.SSS, yyyy-MM-dd HH:mm:ss.SSS, yyyy-MM-dd HH:mm:ss] , e.g. : [EST] 2018-10-15T20:00:00.123");
        MutableMap<String, Object> freeMarkerDateParameters = Maps.mutable.with("dateParam", "2018-10-15T20:00:00.123");
        processDateConstantTimeZoneNoConversion("", freeMarkerDateParameters);
    }

    @Test
    public void testEmptyListInput() throws Exception
    {
        MutableMap<String, Object> freeMarkerDateParameters = Maps.mutable.with("dateParam", Lists.mutable.empty());
        Assert.assertEquals("null", processDateTimeCollectionWithTimeZone("America/New_York", freeMarkerDateParameters));
    }

    private String processDateConstantTimeZoneNoConversion(String tz, MutableMap<String, Object> freeMarkerDateParameters) throws Exception
    {
        String sql = "${UTCtoTZ( \"["+tz+"]\" dateParam )}";
        return processTemplate(sql, freeMarkerDateParameters);
    }

    private String processDateCollectionWithTimeZone(String tz, MutableMap<String, Object> freeMarkerDateParameters) throws Exception
    {
        String sql = "${renderCollectionWithTz(dateParam \"["+tz+"]\" \",\" \"convert(DATE, '\" \"', 101)\" \"null\")}";
        return processTemplate(sql, freeMarkerDateParameters);
    }

    private String processDateTimeCollectionWithTimeZone(String tz, MutableMap<String, Object> freeMarkerDateParameters) throws Exception
    {
        String sql = "${renderCollectionWithTz(dateParam \"["+tz+"]\" \",\" \"convert(DATETIME, '\" \"', 101)\" \"null\")}";
        return processTemplate(sql, freeMarkerDateParameters);
    }
}

