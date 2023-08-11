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

package org.finos.legend.engine.shared.core.operational.errorManagement;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;

public class CodeFixException extends EngineException
{
    private Boolean isCodeFixSuggestion;
    private MutableList<SourceInformation> candidates;

    public CodeFixException(String message, SourceInformation sourceInformation, EngineErrorType type, MutableList<SourceInformation> candidates)
    {
        super(message, sourceInformation, type);
        this.isCodeFixSuggestion = true;
        this.candidates = candidates;
    }

    public Boolean getIsCodeFixSuggestion()
    {
        return this.isCodeFixSuggestion;
    }

    public MutableList<SourceInformation> getCandidates()
    {
        return this.candidates;
    }
}
