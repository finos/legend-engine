// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import java.util.Objects;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;

public class SourceInformationHelper
{
    public static SourceInformation fromM3SourceInformation(org.finos.legend.pure.m4.coreinstance.SourceInformation m3SourceInformation)
    {
        if (m3SourceInformation == null)
        {
            return SourceInformation.getUnknownSourceInformation();
        }
        else
        {
            return new SourceInformation(m3SourceInformation.getSourceId(), m3SourceInformation.getStartLine(), m3SourceInformation.getStartColumn(),
                    m3SourceInformation.getEndLine(), m3SourceInformation.getEndColumn());
        }
    }

    public static org.finos.legend.pure.m4.coreinstance.SourceInformation toM3SourceInformation(SourceInformation sourceInformation)
    {
        return sourceInformation != null
                ? new org.finos.legend.pure.m4.coreinstance.SourceInformation(sourceInformation.sourceId, sourceInformation.startLine, sourceInformation.startColumn, sourceInformation.endLine, sourceInformation.endColumn)
                : new org.finos.legend.pure.m4.coreinstance.SourceInformation("X", 0, 0, 0, 0);
    }

    public static boolean equals(SourceInformation first, SourceInformation second)
    {
        if ((first == null) || (second == null))
        {
            return first == second;
        }
        return Objects.equals(first.sourceId, second.sourceId) &&
                (first.startLine == second.startLine) &&
                (first.startColumn == second.startColumn) &&
                (first.endLine == second.endLine) &&
                (first.endColumn == second.endColumn);
    }

    public static int hashCode(SourceInformation sourceInformation)
    {
        return (sourceInformation == null)
                ? 0
                : Objects.hash(sourceInformation.sourceId, sourceInformation.startLine, sourceInformation.startColumn, sourceInformation.endLine, sourceInformation.endColumn);
    }
}
