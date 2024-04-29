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

package org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.values;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.StringUtils;

public class StagedFilesField extends Value
{
    private Integer columnNumber;
    private String datasetReferenceAlias;
    private String elementPath;

    public StagedFilesField(String quoteIdentifier, Integer columnNumber)
    {
        super(quoteIdentifier);
        this.columnNumber = columnNumber;
    }

    public StagedFilesField(String quoteIdentifier, Integer columnNumber, String datasetReferenceAlias, String alias)
    {
        super(alias, quoteIdentifier);
        this.datasetReferenceAlias = datasetReferenceAlias;
        this.columnNumber = columnNumber;
    }

    public StagedFilesField(String quoteIdentifier, Integer columnNumber, String datasetReferenceAlias, String elementPath, String alias)
    {
        super(alias, quoteIdentifier);
        this.datasetReferenceAlias = datasetReferenceAlias;
        this.columnNumber = columnNumber;
        this.elementPath = elementPath;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        genSqlWithoutAlias(builder);
        super.genSql(builder);
    }

    @Override
    public void genSqlWithoutAlias(StringBuilder builder) throws SqlDomException
    {
        validate();
        if (StringUtils.notEmpty(datasetReferenceAlias))
        {
            builder.append(datasetReferenceAlias).append(".");
        }
        builder.append(String.format("$%d", columnNumber));
        if (StringUtils.notEmpty(elementPath))
        {
            builder.append(":").append(elementPath);
        }
    }

    @Override
    public void push(Object node)
    {
    }

    void validate() throws SqlDomException
    {
        if (columnNumber == null)
        {
            throw new SqlDomException("fileColumnNumber is empty");
        }
    }

    public void setElementPath(String elementPath)
    {
        this.elementPath = elementPath;
    }

    public void setDatasetReferenceAlias(String datasetReferenceAlias)
    {
        this.datasetReferenceAlias = datasetReferenceAlias;
    }
}
