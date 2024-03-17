//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public class Warning
{
    public SourceInformation sourceInformation;
    public String message;

    public Warning(SourceInformation sourceInformation, String message)
    {
        this.sourceInformation = sourceInformation;
        this.message = message;
    }

    /**
     * Only used for testing, the backend should return just the error message.
     */
    public String buildPrettyWarningMessage()
    {
        return ("COMPILATION error" + (sourceInformation == SourceInformation.getUnknownSourceInformation() || sourceInformation == null ? "" : " at " + sourceInformation.getMessage() + "") + (message == null ? "" : ": " + message));
    }
}
