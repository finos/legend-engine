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

package org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.statements;

import org.finos.legend.engine.persistence.components.common.FileFormatType;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.expressions.table.StagedFilesTable;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.TableLike;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.DMLStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.SelectStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.ASSIGNMENT_OPERATOR;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;

public class CopyStatement implements DMLStatement
{
    private Table table;
    private final List<Field> columns;
    private TableLike srcTable;
    private List<String> filePatterns;
    private List<String> filePaths;
    private String userDefinedFileFormatName;
    private FileFormatType fileFormatType;
    private Map<String, Object> fileFormatOptions;
    private Map<String, Object> copyOptions;

    private String validationMode;

    public CopyStatement()
    {
        this.columns = new ArrayList<>();
    }

    public CopyStatement(Table table, List<Field> columns, TableLike srcTable)
    {
        this.table = table;
        this.columns = columns;
        this.srcTable = srcTable;
    }

    /*
     Copy GENERIC PLAN for Snowflake:

     Standard data load
     --------------------------------
        COPY INTO [<namespace>.]<table_name>
        FROM { internalStage | externalStage | externalLocation }
        [ FILES = ( '<file_name>' [ , '<file_name>' ] [ , ... ] ) ]
        [ PATTERN = '<regex_pattern>' ]
        [ FILE_FORMAT = ( { FORMAT_NAME = '[<namespace>.]<file_format_name>' |
        TYPE = { CSV | JSON | AVRO | ORC | PARQUET | XML } [ formatTypeOptions ] } ) ]
        [ copyOptions ]
        [ VALIDATION_MODE = RETURN_<n>_ROWS | RETURN_ERRORS | RETURN_ALL_ERRORS ]

     Data load with transformation
     --------------------------------
        COPY INTO [<namespace>.]<table_name> [ ( <col_name> [ , <col_name> ... ] ) ]
        FROM ( SELECT [<alias>.]$<file_col_num>[.<element>] [ , [<alias>.]$<file_col_num>[.<element>] ... ]
            FROM { internalStage | externalStage } )
        [ FILES = ( '<file_name>' [ , '<file_name>' ] [ , ... ] ) ]
        [ PATTERN = '<regex_pattern>' ]
        [ FILE_FORMAT = ( { FORMAT_NAME = '[<namespace>.]<file_format_name>' |
        TYPE = { CSV | JSON | AVRO | ORC | PARQUET | XML } [ formatTypeOptions ] } ) ]
        [ copyOptions ]
        --------------------------------
     */

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        boolean dataLoadWithTransformation = srcTable instanceof SelectStatement;
        validate();
        builder.append("COPY INTO ");

        // Add table name
        table.genSqlWithoutAlias(builder);
        builder.append(WHITE_SPACE);

        // Add column names
        if (columns != null && columns.size() > 0)
        {
            builder.append(OPEN_PARENTHESIS);
            for (int i = 0; i < columns.size(); i++)
            {
                columns.get(i).genSqlWithNameOnly(builder);
                if (i < (columns.size() - 1))
                {
                    builder.append(COMMA + WHITE_SPACE);
                }
            }
            builder.append(CLOSING_PARENTHESIS);
        }

        builder.append(WHITE_SPACE + Clause.FROM.get() + WHITE_SPACE);

        if (dataLoadWithTransformation)
        {
            builder.append(OPEN_PARENTHESIS);
        }
        srcTable.genSql(builder);
        if (dataLoadWithTransformation)
        {
            builder.append(CLOSING_PARENTHESIS);
        }

        // File Paths
        if (filePaths != null && !filePaths.isEmpty())
        {
            String filePathsStr = filePaths.stream().map(path -> SqlGenUtils.singleQuote(path)).collect(Collectors.joining(", "));
            builder.append(String.format(" FILES = (%s)", filePathsStr));
        }
        // File Patterns
        else if (filePatterns != null && !filePatterns.isEmpty())
        {
            String filePatternStr = filePatterns.stream().map(s -> '(' + s + ')').collect(Collectors.joining("|"));
            builder.append(String.format(" PATTERN = '%s'", filePatternStr));
        }

        // FILE_FORMAT
        if (StringUtils.notEmpty(userDefinedFileFormatName))
        {
            builder.append(String.format(" FILE_FORMAT = (FORMAT_NAME = '%s')", userDefinedFileFormatName));
        }
        else if (fileFormatType != null)
        {
            builder.append(" FILE_FORMAT = ");
            builder.append(OPEN_PARENTHESIS);
            fileFormatOptions = new HashMap<>(fileFormatOptions);
            fileFormatOptions.put("TYPE", fileFormatType.name());
            addOptions(fileFormatOptions, builder);
            builder.append(CLOSING_PARENTHESIS);
        }

        // Add copy Options
        if (copyOptions != null && !copyOptions.isEmpty())
        {
            builder.append(WHITE_SPACE);
            addOptions(copyOptions, builder);
        }
        // Add validation mode
        if (StringUtils.notEmpty(validationMode))
        {
            builder.append(WHITE_SPACE);
            builder.append(String.format("VALIDATION_MODE = '%s'", validationMode));
        }
    }


    private void addOptions(Map<String, Object> options, StringBuilder builder)
    {
        if (options != null && options.size() > 0)
        {
            int ctr = 0;
            for (String option : options.keySet().stream().sorted().collect(Collectors.toList()))
            {
                ctr++;
                builder.append(option);
                builder.append(WHITE_SPACE + ASSIGNMENT_OPERATOR + WHITE_SPACE);
                if (options.get(option) instanceof String)
                {
                    builder.append(SqlGenUtils.singleQuote(options.get(option)));
                }
                else
                {
                    builder.append(options.get(option));
                }
                if (ctr < options.size())
                {
                    builder.append(COMMA + WHITE_SPACE);
                }
            }
        }
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Table)
        {
            table = (Table) node;
        }
        else if (node instanceof Field)
        {
            columns.add((Field) node);
        }
        else if (node instanceof SelectStatement)
        {
            srcTable = (SelectStatement) node;
        }
        else if (node instanceof StagedFilesTable)
        {
            srcTable = (StagedFilesTable) node;
        }
    }

    void validate() throws SqlDomException
    {
        if (srcTable == null)
        {
            throw new SqlDomException("selectStatement is mandatory for Copy Table Command");
        }

        if (table == null)
        {
            throw new SqlDomException("table is mandatory for Copy Table Command");
        }

        if (StringUtils.notEmpty(validationMode) && srcTable instanceof SelectStatement)
        {
            throw new SqlDomException("VALIDATION_MODE is not supported for Data load with transformation");
        }
    }

    public void setFilePatterns(List<String> filePatterns)
    {
        this.filePatterns = filePatterns;
    }

    public void setFilePaths(List<String> filePaths)
    {
        this.filePaths = filePaths;
    }

    public void setUserDefinedFileFormatName(String userDefinedFileFormatName)
    {
        this.userDefinedFileFormatName = userDefinedFileFormatName;
    }

    public void setFileFormatType(FileFormatType fileFormatType)
    {
        this.fileFormatType = fileFormatType;
    }

    public void setFileFormatOptions(Map<String, Object> fileFormatOptions)
    {
        this.fileFormatOptions = fileFormatOptions;
    }

    public void setValidationMode(String validationMode)
    {
        this.validationMode = validationMode;
    }

    public void setCopyOptions(Map<String, Object> copyOptions)
    {
        this.copyOptions = copyOptions;
    }
}