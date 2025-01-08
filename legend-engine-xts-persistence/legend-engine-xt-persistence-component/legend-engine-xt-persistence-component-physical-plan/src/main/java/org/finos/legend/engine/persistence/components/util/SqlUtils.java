// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.util;

import org.slf4j.Logger;

import java.util.Map;
import java.util.regex.Pattern;

public class SqlUtils
{
    public static String getEnrichedSql(Map<String, PlaceholderValue> placeholderKeyValues, String sql, String batchIdPattern)
    {
        String enrichedSql = sql;
        for (Map.Entry<String, PlaceholderValue> entry : placeholderKeyValues.entrySet())
        {
            enrichedSql = replacePlaceholderWithActualValue(enrichedSql, entry.getKey(), entry.getValue().value(), batchIdPattern);
        }
        return enrichedSql;
    }

    private static String getEnrichedSqlWithMasking(Map<String, PlaceholderValue> placeholderKeyValues, String sql, String batchIdPattern)
    {
        String enrichedSql = sql;
        for (Map.Entry<String, PlaceholderValue> entry : placeholderKeyValues.entrySet())
        {
            if (!entry.getValue().isSensitive())
            {
                enrichedSql = replacePlaceholderWithActualValue(enrichedSql, entry.getKey(), entry.getValue().value(), batchIdPattern);
            }
        }
        return enrichedSql;
    }

    private static String replacePlaceholderWithActualValue(String enrichedSql, String placeholder, String actualValue, String batchIdPattern)
    {
        if (placeholder.equals(batchIdPattern))
        {
            // These are to address the issue of batch id patterns being quoted in multi-dataset flow
            String singleQuotedPattern = String.format("'%s'", placeholder);
            enrichedSql = enrichedSql.replaceAll(Pattern.quote(singleQuotedPattern), actualValue);

            String doubleQuotedPattern = String.format("\"%s\"", placeholder);
            enrichedSql = enrichedSql.replaceAll(Pattern.quote(doubleQuotedPattern), actualValue);
        }
        return enrichedSql.replaceAll(Pattern.quote(placeholder), actualValue);
    }

    public static void logSql(Logger logger, SqlLogging sqlLogging, String sqlBeforeReplacingPlaceholders, String sqlAfterReplacingPlaceholders, Map<String, PlaceholderValue> placeholderKeyValues, String batchIdPattern)
    {
        switch (sqlLogging)
        {
            case MASKED:
                String maskedSql = getEnrichedSqlWithMasking(placeholderKeyValues, sqlBeforeReplacingPlaceholders, batchIdPattern);
                logger.info(maskedSql);
                break;
            case UNMASKED:
                logger.info(sqlAfterReplacingPlaceholders);
                break;
            case DISABLED:
                break;
            default:
                throw new IllegalArgumentException("Unsupported sqlLogging: " + sqlLogging);
        }
    }

    public static void logSql(Logger logger, SqlLogging sqlLogging, String sql)
    {
        if (!sqlLogging.equals(SqlLogging.DISABLED))
        {
            logger.info(sql);
        }
    }
}
