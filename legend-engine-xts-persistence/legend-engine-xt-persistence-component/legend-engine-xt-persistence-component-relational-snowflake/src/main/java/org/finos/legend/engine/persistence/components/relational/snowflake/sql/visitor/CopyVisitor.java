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

package org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor;

import org.finos.legend.engine.persistence.components.common.FileFormatType;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Copy;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.snowflake.logicalplan.datasets.FileFormat;
import org.finos.legend.engine.persistence.components.relational.snowflake.logicalplan.datasets.SnowflakeStagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.relational.snowflake.logicalplan.datasets.StandardFileFormat;
import org.finos.legend.engine.persistence.components.relational.snowflake.logicalplan.datasets.UserDefinedFileFormat;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.statements.CopyStatement;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CopyVisitor implements LogicalPlanVisitor<Copy>
{
    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Copy current, VisitorContext context)
    {
        SnowflakeStagedFilesDatasetProperties properties = (SnowflakeStagedFilesDatasetProperties) current.stagedFilesDatasetProperties();
        CopyStatement copyStatement = new CopyStatement();
        setCopyStatementProperties(properties, copyStatement, current);
        prev.push(copyStatement);

        List<LogicalPlanNode> logicalPlanNodes = new ArrayList<>();
        logicalPlanNodes.add(current.sourceDataset());
        logicalPlanNodes.add(current.targetDataset());
        if (!current.fields().isEmpty())
        {
            logicalPlanNodes.addAll(current.fields());
        }
        return new VisitorResult(copyStatement, logicalPlanNodes);
    }

    private static void setCopyStatementProperties(SnowflakeStagedFilesDatasetProperties properties, CopyStatement copyStatement, Copy current)
    {
        copyStatement.setFilePatterns(properties.filePatterns());
        copyStatement.setFilePaths(properties.filePaths());

        if (current.dryRun())
        {
            copyStatement.setValidationMode("RETURN_ERRORS");
        }

        // Add default option into the map
        Map<String, Object> copyOptions = new HashMap<>(properties.copyOptions());
        if (!copyOptions.containsKey("ON_ERROR") && !copyOptions.containsKey("on_error"))
        {
            copyOptions.put("ON_ERROR", "ABORT_STATEMENT");
        }
        copyStatement.setCopyOptions(copyOptions);

        Optional<FileFormat> fileFormat = properties.fileFormat();
        if (fileFormat.isPresent())
        {
            FileFormat format = properties.fileFormat().get();
            if (format instanceof UserDefinedFileFormat)
            {
                UserDefinedFileFormat userDefinedFileFormat = (UserDefinedFileFormat) format;
                copyStatement.setUserDefinedFileFormatName(userDefinedFileFormat.formatName());
            }
            else if (format instanceof StandardFileFormat)
            {
                StandardFileFormat standardFileFormat = (StandardFileFormat) format;
                Map<String, Object> formatOptions = new HashMap<>(standardFileFormat.formatOptions());
                if (current.dryRun() && standardFileFormat.formatType().equals(FileFormatType.CSV))
                {
                    formatOptions.put("ERROR_ON_COLUMN_COUNT_MISMATCH", false);
                }
                copyStatement.setFileFormatType(standardFileFormat.formatType());
                copyStatement.setFileFormatOptions(formatOptions);
            }
        }
    }
}
