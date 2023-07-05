// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.h2.sqldom.schemaops.expresssions.table;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.TableLike;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;

public class CsvRead extends TableLike
{
    public static final String CSV_READ = "CSVREAD";

    private final String filePath;
    private final String csvColumnNames;
    private final String csvOptions;

    public CsvRead(String filePath, String csvColumnNames, String csvOptions)
    {
        this.filePath = filePath;
        this.csvColumnNames = csvColumnNames;
        this.csvOptions = csvOptions;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public String getCsvColumnNames()
    {
        return csvColumnNames;
    }

    public String getCsvOptions()
    {
        return csvOptions;
    }

    /*
        Syntax:
        CSVREAD('{FILE_PATH}','{CSV_COLUMN_NAMES}','{CSV_OPTIONS}')
     */

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(CSV_READ);
        builder.append(OPEN_PARENTHESIS);
        builder.append(SqlGenUtils.singleQuote(filePath));
        builder.append(COMMA);
        builder.append(SqlGenUtils.singleQuote(csvColumnNames));
        builder.append(COMMA);
        String csvOptionsString = empty(csvOptions) ? "NULL" : SqlGenUtils.singleQuote(csvOptions);
        builder.append(csvOptionsString);
        builder.append(CLOSING_PARENTHESIS);
    }

    void validate() throws SqlDomException
    {
        if (empty(filePath))
        {
            throw new SqlDomException("filePath is mandatory");
        }
        if (empty(csvColumnNames))
        {
            throw new SqlDomException("csvColumnNames is mandatory");
        }
    }

    // utility methods

    private static boolean empty(CharSequence cs)
    {
        return cs == null || cs.length() == 0;
    }
}
