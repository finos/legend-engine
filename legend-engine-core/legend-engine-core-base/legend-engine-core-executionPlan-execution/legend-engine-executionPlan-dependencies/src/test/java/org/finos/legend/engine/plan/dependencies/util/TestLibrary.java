//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.dependencies.util;

import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TestLibrary
{
    @Test
    public void testSplitEmptyToken()
    {
        List<String> res = Library.split("abc", "");
        List<String> expected = Arrays.asList("abc");
        Assert.assertEquals(expected, res);
    }

    @Test
    public void testSplitSingleCharToken()
    {
        List<String> res = Library.split("abc", "b");
        List<String> expected = Arrays.asList("a", "c");
        Assert.assertEquals(expected, res);
    }

    @Test
    public void testSplitMultiCharToken()
    {
        List<String> res = Library.split("abcdefabcdefabc", "def");
        List<String> expected = Arrays.asList("abc", "abc", "abc");
        Assert.assertEquals(expected, res);
    }

    @Test
    public void testAt()
    {
        Assert.assertEquals("a", Library.at("a", 0));
        Assert.assertEquals("a", Library.at(Arrays.asList("a", "b"), 0));

        try
        {
            Library.at("a", 1);
        }
        catch (Exception e)
        {
            String expectedErrorMsg = "The system is trying to get an element at offset 1 where the collection is of size 1";
            Assert.assertEquals(expectedErrorMsg, e.getMessage());
        }
        try
        {
            Library.at("a", -1);
        }
        catch (Exception e)
        {
            String expectedErrorMsg = "The system is trying to get an element at offset -1 where the collection is of size 1";
            Assert.assertEquals(expectedErrorMsg, e.getMessage());
        }
        try
        {
            Library.at(null, 0);
        }
        catch (Exception e)
        {
            String expectedErrorMsg = "The system is trying to get an element at offset 0 where the collection is of size 0";
            Assert.assertEquals(expectedErrorMsg, e.getMessage());
        }
    }

    /**
     * Weeks are numbered as ISO 8601 does it, whatever the JVM is set to: a week runs Monday to
     * Sunday, and week 1 is the one holding the year's first Thursday. The dates here are ones where
     * that differs from numbering weeks from Sunday, which is what a US default locale used to give,
     * so the locales below would disagree if the numbering still followed them.
     */
    @Test
    public void testWeekOfYearIsISO8601WhateverTheLocale()
    {
        Locale before = Locale.getDefault();
        try
        {
            for (Locale locale : new Locale[]{Locale.US, Locale.UK, Locale.FRANCE, Locale.JAPAN, Locale.forLanguageTag("ar-EG")})
            {
                Locale.setDefault(locale);
                String context = locale.toString();

                // a week starts on Monday, so the Sunday closing 2020 is not yet week 1 of 2021
                Assert.assertEquals(context, 52, Library.weekOfYear(PureDate.newPureDate(2020, 12, 27)));
                Assert.assertEquals(context, 53, Library.weekOfYear(PureDate.newPureDate(2020, 12, 28)));

                // 2021 opens on a Friday, so its first days finish 2020's last week
                Assert.assertEquals(context, 53, Library.weekOfYear(PureDate.newPureDate(2021, 1, 1)));
                Assert.assertEquals(context, 53, Library.weekOfYear(PureDate.newPureDate(2021, 1, 3)));
                Assert.assertEquals(context, 1, Library.weekOfYear(PureDate.newPureDate(2021, 1, 4)));

                // 2000 opens on a Saturday, so its first two days belong to the year before
                Assert.assertEquals(context, 52, Library.weekOfYear(PureDate.newPureDate(2000, 1, 1)));
                Assert.assertEquals(context, 1, Library.weekOfYear(PureDate.newPureDate(2000, 1, 3)));

                // 2015 opens on a Thursday, so that week is its own week 1
                Assert.assertEquals(context, 1, Library.weekOfYear(PureDate.newPureDate(2015, 1, 1)));
                Assert.assertEquals(context, 16, Library.weekOfYear(PureDate.newPureDate(2015, 4, 15)));
            }
        }
        finally
        {
            Locale.setDefault(before);
        }
    }

    @Test
    public void testIndexOf()
    {
        Integer testIndexOfOneElement = Library.indexOf("a", "a");
        Integer expectedTestIndexOfOneElement = 0;
        Assert.assertEquals(expectedTestIndexOfOneElement, testIndexOfOneElement);

        Integer expected = -1;
        Assert.assertEquals(expected, Library.indexOf(null, "b"));
        Assert.assertEquals(expected, Library.indexOf("a", "b"));
        Assert.assertEquals(expected, Library.indexOf(Arrays.asList("a", "b"), "c"));
    }
}
