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

public class CodeFixExceptionError extends ExceptionError
{
    private Boolean isCodeFixSuggestion = false;
    private MutableList<SourceInformation> candidates = Lists.mutable.empty();

    CodeFixExceptionError(int code, Throwable t)
    {
        super(code, t);
        if (t instanceof CodeFixException)
        {
            this.isCodeFixSuggestion = ((CodeFixException) t).getIsCodeFixSuggestion();
            this.candidates = ((CodeFixException) t).getCandidates();
        }
    }

    public MutableList<SourceInformation> getCandidates()
    {
        return candidates;
    }

    public Boolean getIsCodeFixSuggestion()
    {
        return isCodeFixSuggestion;
    }
}
