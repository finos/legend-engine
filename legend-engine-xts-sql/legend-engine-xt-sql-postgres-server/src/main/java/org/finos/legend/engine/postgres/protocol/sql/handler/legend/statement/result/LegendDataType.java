// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result;

public class LegendDataType
{
    public static final String STRICT_DATE = "StrictDate";
    public static final String DATE = "Date";
    public static final String DATE_TIME = "DateTime";
    public static final String INTEGER = "Integer";
    public static final String FLOAT = "Float";
    public static final String NUMBER = "Number";
    public static final String DECIMAL = "Decimal";
    public static final String BOOLEAN = "Boolean";
    public static final String STRING = "String";
    public static final String TINY_INT = "meta::pure::precisePrimitives::TinyInt";
    public static final String U_TINY_INT = "meta::pure::precisePrimitives::UTinyInt";
    public static final String SMALL_INT = "meta::pure::precisePrimitives::SmallInt";
    public static final String U_SMALL_INT = "meta::pure::precisePrimitives::USmallInt";
    public static final String INT = "meta::pure::precisePrimitives::Int";
    public static final String U_INT = "meta::pure::precisePrimitives::UInt";
    public static final String BIG_INT = "meta::pure::precisePrimitives::BigInt";
    public static final String U_BIG_INT = "meta::pure::precisePrimitives::UBigInt";
    public static final String VARCHAR = "meta::pure::precisePrimitives::Varchar";
    public static final String TIMESTAMP = "meta::pure::precisePrimitives::Timestamp";
    public static final String FLOAT4 = "meta::pure::precisePrimitives::Float4";
    public static final String DOUBLE = "meta::pure::precisePrimitives::Double";
    public static final String NUMERIC = "meta::pure::precisePrimitives::Numeric";
    public static final String VARIANT = "meta::pure::metamodel::variant::Variant";



    public static final String ARRAY = "Array";
}
