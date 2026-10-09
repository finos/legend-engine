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

    @Test
    public void testFormatDate()
    {
        PureDate date = PureDate.parsePureDate("2014-03-10T13:07:44.07");
        Assert.assertEquals("on 2014-03-10 13:07", Library.format("on %t{yyyy-MM-dd?[\" \"HH:mm]}", Arrays.asList(date)));

        // a date pattern ends at the first brace outside quotes, and a backslash inside quotes makes text of the
        // character after it, which is the only way to write a quote there, including as a sub-second fill
        Assert.assertEquals("on 2014} at x", Library.format("on %t{yyyy\"}\"} at %s", Arrays.asList(date, "x")));
        Assert.assertEquals("on 2014a\"b at x", Library.format("on %t{yyyy\"a\\\"b\"} at %s", Arrays.asList(date, "x")));
        Assert.assertEquals("on 07\"\" at x", Library.format("on %t{S(4,4,\"\\\"\")} at %s", Arrays.asList(date, "x")));
    }
}
