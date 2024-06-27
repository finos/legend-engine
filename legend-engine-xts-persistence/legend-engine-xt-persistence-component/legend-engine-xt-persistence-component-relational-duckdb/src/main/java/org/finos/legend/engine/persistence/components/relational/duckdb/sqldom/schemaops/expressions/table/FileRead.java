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

package org.finos.legend.engine.persistence.components.relational.duckdb.sqldom.schemaops.expressions.table;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.TableLike;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.ASSIGNMENT_OPERATOR;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_CURLY_BRACKET;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_SQUARE_BRACKET;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COLON;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_CURLY_BRACKET;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_SQUARE_BRACKET;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.SINGLE_QUOTE;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class FileRead extends TableLike
{
    public enum FileType
    {
        CSV,
        JSON,
        PARQUET
    }

    private final FileType fileType;
    private final List<String> files;
    private final List<String> columns;
    private final List<DataType> columnTypes;
    private final Map<String, Object> options;

    public FileRead(FileType fileType, List<String> files, List<String> columns, List<DataType> columnTypes, Map<String, Object> options)
    {
        this.fileType = fileType;
        this.files = files;
        this.columns = columns;
        this.columnTypes = columnTypes;
        this.options = options;
    }

    /*
        Syntax:
        FILE_READ_FUNCTION(['{FILE_PATH}'],{OTHER_OPTIONS})
    */
    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        builder.append("READ_" + fileType.name());
        builder.append(OPEN_PARENTHESIS);

        builder.append(OPEN_SQUARE_BRACKET);
        for (int ctr = 0; ctr < files.size(); ctr++)
        {
            builder.append(SqlGenUtils.singleQuote(files.get(ctr)));
            if (ctr < (files.size() - 1))
            {
                builder.append(COMMA);
            }
        }
        builder.append(CLOSING_SQUARE_BRACKET);

        // Print columns explicitly for CSV only
        if (fileType == FileType.CSV && columns != null && columns.size() > 0)
        {
            builder.append(COMMA);
            builder.append(WHITE_SPACE);
            builder.append("COLUMNS = ");
            builder.append(OPEN_CURLY_BRACKET);
            genSqlForColumnAndTypeMapping(builder);
            builder.append(CLOSING_CURLY_BRACKET);
            builder.append(COMMA);
            builder.append(WHITE_SPACE);
            builder.append("AUTO_DETECT = FALSE");
        }

        genSqlForOptions(builder);

        builder.append(CLOSING_PARENTHESIS);
    }
    
    private void genSqlForColumnAndTypeMapping(StringBuilder builder)
    {
        for (int i = 0; i < columns.size(); i++)
        {
            builder.append(SqlGenUtils.singleQuote(columns.get(i)));
            builder.append(COLON);
            builder.append(SINGLE_QUOTE);
            columnTypes.get(i).genSql(builder);
            builder.append(SINGLE_QUOTE);
            if (i < columns.size() - 1)
            {
                builder.append(COMMA + WHITE_SPACE);
            }
        }
    }

    private void genSqlForOptions(StringBuilder builder)
    {
        if (options != null && options.size() > 0)
        {
            builder.append(COMMA);
            builder.append(WHITE_SPACE);
            int ctr = 0;
            for (String option : options.keySet().stream().sorted().collect(Collectors.toList()))
            {
                ctr++;
                builder.append(option);
                builder.append(ASSIGNMENT_OPERATOR);
                if (options.get(option) instanceof String)
                {
                    builder.append(SqlGenUtils.singleQuote(options.get(option)));
                }
                else
                {
                    // number
                    builder.append(options.get(option));
                }

                if (ctr < options.size())
                {
                    builder.append(COMMA + WHITE_SPACE);
                }
            }
        }
    }
}
