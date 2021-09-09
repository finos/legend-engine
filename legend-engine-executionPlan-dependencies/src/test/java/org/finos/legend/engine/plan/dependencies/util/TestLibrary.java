package org.finos.legend.engine.plan.dependencies.util;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TestLibrary {
    @Test
    public void testSplitEmptyToken() {
        List<String> res = Library.split("abc", "");
        List<String> expected = Arrays.asList("abc");
        Assert.assertEquals(expected, res);
    }

    @Test
    public void testSplitSingleCharToken() {
        List<String> res = Library.split("abc", "b");
        List<String> expected = Arrays.asList("a", "c");
        Assert.assertEquals(expected, res);
    }

    @Test
    public void testSplitMultiCharToken() {
        List<String> res = Library.split("abcdefabcdefabc", "def");
        List<String> expected = Arrays.asList("abc", "abc", "abc");
        Assert.assertEquals(expected, res);
    }

    @Test
    public void testAt() {
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
    public void testIndexOf() {
        Integer testIndexOfOneElement = Library.indexOf("a", "a");
        Integer expectedTestIndexOfOneElement = 0;
        Assert.assertEquals(expectedTestIndexOfOneElement, testIndexOfOneElement);

        Integer expected = -1;
        Assert.assertEquals(expected, Library.indexOf(null, "b"));
        Assert.assertEquals(expected, Library.indexOf("a", "b"));
        Assert.assertEquals(expected, Library.indexOf(Arrays.asList("a", "b"), "c"));
    }
}
