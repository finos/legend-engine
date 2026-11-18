// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.perf;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Byte-for-byte comparison of a generated plan against a reference plan written by an earlier run
 * with --dumpplan, so a performance change can be shown not to alter the plan it produces.
 */
public final class PlanComparison
{
    private static final int CONTEXT = 120;

    private PlanComparison()
    {
    }

    public static void assertSameAsFile(String referencePath, String actual)
    {
        String expected;
        try
        {
            expected = new String(Files.readAllBytes(Paths.get(referencePath)), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to read reference plan " + referencePath, e);
        }
        if (!expected.equals(actual))
        {
            throw new PlanMismatchException("Plan differs from " + referencePath + ": " + describeDifference(expected, actual));
        }
    }

    public static String describeDifference(String expected, String actual)
    {
        int limit = Math.min(expected.length(), actual.length());
        int at = 0;
        while (at < limit && expected.charAt(at) == actual.charAt(at))
        {
            at++;
        }
        return "lengths " + expected.length() + " vs " + actual.length() + ", first difference at char " + at
                + "\n  expected: ..." + window(expected, at) + "...\n  actual:   ..." + window(actual, at) + "...";
    }

    private static String window(String text, int at)
    {
        return text.substring(Math.max(0, at - CONTEXT / 2), Math.min(text.length(), at + CONTEXT / 2));
    }

    public static class PlanMismatchException extends RuntimeException
    {
        public PlanMismatchException(String message)
        {
            super(message);
        }
    }
}
