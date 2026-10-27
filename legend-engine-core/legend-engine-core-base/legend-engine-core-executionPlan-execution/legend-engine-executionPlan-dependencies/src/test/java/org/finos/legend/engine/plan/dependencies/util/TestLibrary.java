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

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class TestLibrary
{
    @Test
    public void testPartition()
    {
        Pair<List<Integer>, List<Integer>> result = Library.partition(Stream.of(1, 2, 3, 4), i -> i > 2);
        Assert.assertEquals(Arrays.asList(3, 4), result.getOne());
        Assert.assertEquals(Arrays.asList(1, 2), result.getTwo());
    }

    @Test
    public void testPartitionAllMatch()
    {
        Pair<List<Integer>, List<Integer>> result = Library.partition(Stream.of(1, 2), i -> i > 0);
        Assert.assertEquals(Arrays.asList(1, 2), result.getOne());
        Assert.assertEquals(Arrays.asList(), result.getTwo());
    }

    @Test
    public void testPartitionNoneMatch()
    {
        Pair<List<Integer>, List<Integer>> result = Library.partition(Stream.of(1, 2), i -> i > 5);
        Assert.assertEquals(Arrays.asList(), result.getOne());
        Assert.assertEquals(Arrays.asList(1, 2), result.getTwo());
    }

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

    @Test
    public void testMinusOfLongList()
    {
        Assert.assertEquals(2L, Library.minus(Arrays.asList(7L, 5L)));
        Assert.assertEquals(-2L, Library.minus(Arrays.asList(7L, 5L, 4L)));
    }

    @Test
    public void testMinusOfDoubleList()
    {
        Assert.assertEquals(2.0, Library.minus(Arrays.asList(7.0, 5.0)));
    }

    @Test
    public void testMinusOfBigDecimalList()
    {
        Assert.assertEquals(BigDecimal.valueOf(2), Library.minus(Arrays.asList(BigDecimal.valueOf(7), BigDecimal.valueOf(5))));
    }

    @Test
    public void testMinusOfEmptyList()
    {
        Assert.assertEquals(0, Library.minus(Arrays.<Long>asList()));
    }

    @Test
    public void testMinusOfSingleElementList()
    {
        // generated code never takes this path for unary minus (it uses plain negation instead),
        // so this exercises the method's own fallback, which widens through double
        Assert.assertEquals(-7.0, Library.minus(Arrays.asList(7L)));
    }
}
