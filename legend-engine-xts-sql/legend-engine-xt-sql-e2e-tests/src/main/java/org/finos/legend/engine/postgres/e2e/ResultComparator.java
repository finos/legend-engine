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

        // Canonicalise temporal values to epoch millis before the Number branch below: the JDBC
        // reference side yields java.sql.Date/Timestamp, Legend's TDS JSON yields epoch-millis Long.
        Object e = toEpochMillisIfTemporal(expected);
        Object a = toEpochMillisIfTemporal(actual);

        // Numeric comparison with epsilon for floating point
        if (e instanceof Number && a instanceof Number)
        {
            double ev = ((Number) e).doubleValue();
            double av = ((Number) a).doubleValue();
            if (Double.isNaN(ev) && Double.isNaN(av))
            {
                return true;
            }
            if (Double.isInfinite(ev) && Double.isInfinite(av))
            {
                return ev == av;
            }
            // For BigDecimal, compare with scale awareness
            if (e instanceof BigDecimal && a instanceof BigDecimal)
            {
                return ((BigDecimal) e).compareTo((BigDecimal) a) == 0;
            }
            // Float/double: relative + absolute epsilon comparison
            if (e instanceof Float || e instanceof Double || a instanceof Float || a instanceof Double)
            {
                double diff = Math.abs(ev - av);
                double maxAbs = Math.max(Math.abs(ev), Math.abs(av));
                return diff <= Math.max(ABSOLUTE_EPSILON, RELATIVE_EPSILON * maxAbs);
            }
            // Integer types: exact comparison
            return ev == av;
        }

        // Boolean comparison
        if (e instanceof Boolean && a instanceof Boolean)
        {
            return e.equals(a);
        }

        // Default: string comparison
        return e.toString().equals(a.toString());
    }

    /**
     * java.sql.Date/Time/Timestamp all extend java.util.Date, so this covers every JDBC temporal
     * type in one check.
     */
    private static Object toEpochMillisIfTemporal(Object value)
    {
        if (value instanceof java.util.Date)
        {
            return ((java.util.Date) value).getTime();
        }
        if (value instanceof java.time.Instant)
        {
            return ((java.time.Instant) value).toEpochMilli();
        }
        if (value instanceof java.time.LocalDate)
        {
            return ((java.time.LocalDate) value).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
        }
        if (value instanceof java.time.LocalDateTime)
        {
            return ((java.time.LocalDateTime) value).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        }
        if (value instanceof java.time.OffsetDateTime)
        {
            return ((java.time.OffsetDateTime) value).toInstant().toEpochMilli();
        }
        return value;
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

