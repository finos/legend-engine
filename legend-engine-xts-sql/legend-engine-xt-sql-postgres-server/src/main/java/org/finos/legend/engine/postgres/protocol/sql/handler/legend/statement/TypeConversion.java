// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;

import java.sql.Types;

import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.*;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.BIG_INT;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.DATE;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.DATE_TIME;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.DECIMAL;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.DOUBLE;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.FLOAT;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.FLOAT4;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.INT;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.NUMBER;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.NUMERIC;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.SMALL_INT;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.STRICT_DATE;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.STRING;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.TIMESTAMP;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.TINY_INT;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.U_BIG_INT;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.U_INT;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.U_SMALL_INT;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.U_TINY_INT;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.VARCHAR;
import static org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendDataType.VARIANT;

public class TypeConversion
{
    public static MutableMap<String, Integer> _typeConversions = registerTypeConversion();

    private static MutableMap<String, Integer> registerTypeConversion()
    {
        MutableMap<String, Integer> typeConversions = Maps.mutable.empty();

        registerDBType(BOOLEAN, Types.BOOLEAN, typeConversions);
        registerDBType(INTEGER, Types.BIGINT, typeConversions);
        registerDBType(FLOAT, Types.DOUBLE, typeConversions);
        registerDBType(NUMBER, Types.DECIMAL, typeConversions);
        registerDBType(STRING, Types.VARCHAR, typeConversions);
        registerDBType(DATE_TIME, Types.TIMESTAMP, typeConversions);
        registerDBType(DATE, Types.TIMESTAMP, typeConversions);
        registerDBType(STRICT_DATE, Types.DATE, typeConversions);

        registerDBType(DECIMAL, Types.DECIMAL, typeConversions);
        registerDBType(BIG_INT, Types.BIGINT, typeConversions);
        registerDBType(NUMERIC, Types.DECIMAL, typeConversions);
        registerDBType(DOUBLE, Types.DOUBLE, typeConversions);
        registerDBType(FLOAT4, Types.FLOAT, typeConversions);
        registerDBType(INT, Types.INTEGER, typeConversions);
        registerDBType(SMALL_INT, Types.SMALLINT, typeConversions);
        registerDBType(TIMESTAMP, Types.TIMESTAMP, typeConversions);
        registerDBType(TINY_INT, Types.TINYINT, typeConversions);
        registerDBType(VARCHAR, Types.VARCHAR, typeConversions);
        registerDBType(VARIANT, Types.JAVA_OBJECT, typeConversions);

        registerDBType(U_TINY_INT, Types.TINYINT, typeConversions);
        registerDBType(U_SMALL_INT, Types.SMALLINT, typeConversions);
        registerDBType(U_INT, Types.INTEGER, typeConversions);
        registerDBType(U_BIG_INT, Types.BIGINT, typeConversions);

        return typeConversions;
    }

    private static void registerDBType(String pureType, int dbType, MutableMap<String, Integer> typeConversions)
    {
        typeConversions.put(pureType, dbType);
    }
}
