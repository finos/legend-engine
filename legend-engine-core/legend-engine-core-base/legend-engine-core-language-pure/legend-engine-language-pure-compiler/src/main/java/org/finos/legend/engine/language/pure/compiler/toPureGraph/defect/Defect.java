// Copyright 2025 Goldman Sachs
////
//// Licensed under the Apache License, Version 2.0 (the "License");
//// you may not use this file except in compliance with the License.
//// You may obtain a copy of the License at
////
////      http://www.apache.org/licenses/LICENSE-2.0
////
//// Unless required by applicable law or agreed to in writing, software
//// distributed under the License is distributed on an "AS IS" BASIS,
//// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//// See the License for the specific language governing permissions and
//// limitations under the License.

package org.finos.legend.engine.language.pure.compiler.toPureGraph.defect;

import org.finos.legend.engine.protocol.pure.m3.SourceInformation;

public class Defect
{
    public DefectSeverityLevel defectSeverityLevel;
    public SourceInformation sourceInformation;
    public String message;
    public String defectTypeId;

    public Defect(DefectSeverityLevel defectSeverityLevel, SourceInformation sourceInformation, String message, String defectTypeId)
    {
        this.defectSeverityLevel = defectSeverityLevel;
        this.sourceInformation = sourceInformation;
        this.message = message;
        this.defectTypeId = defectTypeId;
    }

    public Object applyQuickFix()
    {
        return null;
    }

    /**
     * Only used for testing, the backend should return just the error message.
     */
    public String buildPrettyDefectMessage()
    {
        return ("COMPILATION " + defectSeverityLevel.level + (sourceInformation == SourceInformation.getUnknownSourceInformation() || sourceInformation == null ? "" : " at " + sourceInformation.getMessage() + "") + (message == null ? "" : ": " + message));
    }
}
