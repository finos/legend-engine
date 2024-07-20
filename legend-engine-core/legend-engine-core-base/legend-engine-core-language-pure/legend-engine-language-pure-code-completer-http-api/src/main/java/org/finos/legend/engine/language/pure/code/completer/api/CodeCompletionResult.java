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

package org.finos.legend.engine.language.pure.code.completer.api;

import org.finos.legend.engine.language.pure.code.completer.CompletionResult;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CodeCompletionResult
{
    private final List<Completion> completions;
    private final String exception;

    public CodeCompletionResult(CompletionResult completionResult)
    {
        if (completionResult.getEngineException() != null)
        {
            this.completions = Collections.emptyList();
            this.exception = completionResult.getEngineException().getMessage();
        }
        else
        {
            this.completions = completionResult.getCompletion().stream().map(c -> new Completion(c.getDisplay(), c.getCompletion())).collect(Collectors.toList());
            this.exception = null;
        }
    }

    public List<Completion> getCompletions()
    {
        return completions;
    }

    public String getException()
    {
        return exception;
    }

    public static final class Completion
    {
        private final String display;
        private final String completion;

        Completion(String display, String completion)
        {
            this.display = display;
            this.completion = completion;
        }

        public String getDisplay()
        {
            return display;
        }

        public String getCompletion()
        {
            return completion;
        }
    }
}
