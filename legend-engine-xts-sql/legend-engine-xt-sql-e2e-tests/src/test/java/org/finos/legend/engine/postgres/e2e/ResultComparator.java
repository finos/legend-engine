// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.postgres.e2e;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Compares two ResultMatrix instances and produces a ComparisonResult.
 */
public class ResultComparator
{
    private static final double ABSOLUTE_EPSILON = 1e-9;
    private static final double RELATIVE_EPSILON = 1e-6;

    public static ComparisonResult compare(ResultMatrix expected, ResultMatrix actual)
    {
        List<String> diffs = new ArrayList<>();

        if (expected.getColumnCount() != actual.getColumnCount())
        {
            diffs.add("Column count mismatch: expected " + expected.getColumnCount() + " but got " + actual.getColumnCount());
            return new ComparisonResult(false, diffs);
        }

        // Compare column names (case-insensitive)
        for (int i = 0; i < expected.getColumnCount(); i++)
        {
            if (!expected.getColumnNames().get(i).equalsIgnoreCase(actual.getColumnNames().get(i)))
            {
                diffs.add("Column name mismatch at index " + i + ": expected '" + expected.getColumnNames().get(i) + "' but got '" + actual.getColumnNames().get(i) + "'");
            }
        }

        if (expected.getRowCount() != actual.getRowCount())
        {
            diffs.add("Row count mismatch: expected " + expected.getRowCount() + " but got " + actual.getRowCount());
            return new ComparisonResult(false, diffs);
        }

        // Compare cell by cell
        int maxDiffs = 10; // limit reported diffs
        for (int row = 0; row < expected.getRowCount() && diffs.size() < maxDiffs; row++)
        {
            List<Object> expectedRow = expected.getRows().get(row);
            List<Object> actualRow = actual.getRows().get(row);
            for (int col = 0; col < expected.getColumnCount() && diffs.size() < maxDiffs; col++)
            {
                Object ev = expectedRow.get(col);
                Object av = actualRow.get(col);
                if (!cellEquals(ev, av))
                {
                    diffs.add("Row " + row + ", column '" + expected.getColumnNames().get(col) + "': expected '" + ev + "' but got '" + av + "'");
                }
            }
        }

        return new ComparisonResult(diffs.isEmpty(), diffs);
    }

    private static boolean cellEquals(Object expected, Object actual)
    {
        if (expected == null && actual == null)
        {
            return true;
        }
        if (expected == null || actual == null)
        {
            return false;
        }

        // Numeric comparison with epsilon for floating point
        if (expected instanceof Number && actual instanceof Number)
        {
            double ev = ((Number) expected).doubleValue();
            double av = ((Number) actual).doubleValue();
            if (Double.isNaN(ev) && Double.isNaN(av))
            {
                return true;
            }
            if (Double.isInfinite(ev) && Double.isInfinite(av))
            {
                return ev == av;
            }
            // For BigDecimal, compare with scale awareness
            if (expected instanceof BigDecimal && actual instanceof BigDecimal)
            {
                return ((BigDecimal) expected).compareTo((BigDecimal) actual) == 0;
            }
            // Float/double: relative + absolute epsilon comparison
            if (expected instanceof Float || expected instanceof Double || actual instanceof Float || actual instanceof Double)
            {
                double diff = Math.abs(ev - av);
                double maxAbs = Math.max(Math.abs(ev), Math.abs(av));
                return diff <= Math.max(ABSOLUTE_EPSILON, RELATIVE_EPSILON * maxAbs);
            }
            // Integer types: exact comparison
            return ev == av;
        }

        // Boolean comparison
        if (expected instanceof Boolean && actual instanceof Boolean)
        {
            return expected.equals(actual);
        }

        // Default: string comparison
        return expected.toString().equals(actual.toString());
    }

    public static class ComparisonResult
    {
        private final boolean match;
        private final List<String> diffs;

        public ComparisonResult(boolean match, List<String> diffs)
        {
            this.match = match;
            this.diffs = diffs;
        }

        public boolean isMatch()
        {
            return match;
        }

        public List<String> getDiffs()
        {
            return diffs;
        }
    }
}

