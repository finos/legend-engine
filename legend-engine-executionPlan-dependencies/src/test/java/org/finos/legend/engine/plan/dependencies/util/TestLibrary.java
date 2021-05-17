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
}

