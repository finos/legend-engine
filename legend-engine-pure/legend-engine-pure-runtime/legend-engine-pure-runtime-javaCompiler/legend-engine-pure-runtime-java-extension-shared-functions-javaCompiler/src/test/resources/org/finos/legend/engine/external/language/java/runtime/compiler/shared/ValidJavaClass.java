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

package org.finos.legend.engine.external.language.java.runtime.compiler.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidJavaClass
{
    private final String source;

    private ValidJavaClass(String source)
    {
        this.source = source;
    }

    public String replaceNewlines()
    {
        return this.source.replaceAll("\\R", " ");
    }

    public String[] splitLines()
    {
        return this.source.split("\\R");
    }

    public int countLines()
    {
        return splitLines().length;
    }

    public int[] lineLengths()
    {
        String[] lines = splitLines();
        int[] lengths = new int[lines.length];
        for (int i = 0; i < lines.length; i++)
        {
            lengths[i] = lines[i].length();
        }
        return lengths;
    }

    public static void succeed()
    {
        String source = "the quick\r\nbrown fox\njumps over\rthe lazy dog";
        new ValidJavaClass(source).replaceNewlines();
    }

    public static String succeedReturnString()
    {
        String source = "the quick\r\nbrown fox\njumps over\rthe lazy dog";
        return new ValidJavaClass(source).replaceNewlines();
    }

    public static int succeedReturnInt()
    {
        String source = "the quick\r\nbrown fox\njumps over\rthe lazy dog";
        return new ValidJavaClass(source).countLines();
    }

    public static String[] succeedReturnArray()
    {
        String source = "the quick\r\nbrown fox\njumps over\rthe lazy dog";
        return new ValidJavaClass(source).splitLines();
    }

    public static int[] succeedReturnIntArray()
    {
        String source = "the quick\r\nbrown fox\njumps over\rthe lazy dog";
        return new ValidJavaClass(source).lineLengths();
    }

    public static List<Object> succeedReturnList()
    {
        return Arrays.asList(succeedReturnArray());
    }

    public static Map<Character, List<Integer>> succeedReturnMap()
    {
        String source = "the quick\r\nbrown fox\njumps over\rthe lazy dog";
        String converted = new ValidJavaClass(source).replaceNewlines();
        Map<Character, List<Integer>> map = new HashMap<>();
        for (int i = 0; i < converted.length(); i++)
        {
            char c = converted.charAt(i);
            if (!Character.isWhitespace(c))
            {
                map.computeIfAbsent(c, x -> new ArrayList<>()).add(i);
            }
        }
        return map;
    }

    public static String fail()
    {
        throw new RuntimeException("Oh no! A failure!");
    }
}