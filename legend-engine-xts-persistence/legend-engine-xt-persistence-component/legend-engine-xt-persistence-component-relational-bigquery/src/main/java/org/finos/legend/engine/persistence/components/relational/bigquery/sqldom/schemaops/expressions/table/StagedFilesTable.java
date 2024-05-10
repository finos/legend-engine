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

package org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schemaops.expressions.table;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.TableLike;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.ASSIGNMENT_OPERATOR;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_SQUARE_BRACKET;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_SQUARE_BRACKET;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class StagedFilesTable extends TableLike
{
    private List<String> files;
    private Map<String, Object> loadOptions;

    public StagedFilesTable(List<String> files, Map<String, Object> loadOptions)
    {
        this.files = files;
        this.loadOptions = loadOptions;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();

        builder.append(OPEN_PARENTHESIS);
        builder.append("uris");
        builder.append(ASSIGNMENT_OPERATOR);
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

        if (loadOptions != null && loadOptions.size() > 0)
        {
            builder.append(COMMA);
            builder.append(WHITE_SPACE);
            int ctr = 0;
            for (String option : loadOptions.keySet().stream().sorted().collect(Collectors.toList()))
            {
                ctr++;
                builder.append(option);
                builder.append(ASSIGNMENT_OPERATOR);
                if (loadOptions.get(option) instanceof String)
                {
                    builder.append(SqlGenUtils.singleQuote(loadOptions.get(option)));
                }
                else
                {
                    // number
                    builder.append(loadOptions.get(option));
                }

                if (ctr < loadOptions.size())
                {
                    builder.append(COMMA + WHITE_SPACE);
                }
            }
        }
        builder.append(CLOSING_PARENTHESIS);
    }

    @Override
    public void push(Object node)
    {
    }

    void validate() throws SqlDomException
    {
        if (files == null || files.isEmpty())
        {
            throw new SqlDomException("files are mandatory for loading from files");
        }
        if (!loadOptions.containsKey("format"))
        {
            throw new SqlDomException("format is mandatory for loading from files");
        }
    }
}
