// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.h2;

import org.apache.commons.codec.digest.DigestUtils;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;


public class H2DigestUtil
{
    public static void registerMD5Udf(JdbcHelper sink, String UdfName)
    {
        sink.executeStatement("CREATE ALIAS " + UdfName + " FOR \"org.finos.legend.engine.persistence.components.relational.h2.H2DigestUtil.MD5\";");
    }

    public static String MD5(String[] columnNameList, String[] columnValueList)
    {
        String columnNames = String.join("", columnNameList);
        String columnValues = String.join("", columnValueList);
        String columnNamesAndColumnValues = columnNames + columnValues;
        return DigestUtils.md5Hex(columnNamesAndColumnValues).toUpperCase();
    }
}
