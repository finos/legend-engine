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

package org.finos.legend.engine.repl.autocomplete;

public class CompletionItem
{
    private final String display;
    private String completion;

    public CompletionItem(String completion)
    {
        this(completion, completion);
    }

    public CompletionItem(String display, String completion)
    {
        this.display = display;
        this.completion = completion;
    }

    public void setCompletion(String completion)
    {
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

    @Override
    public String toString()
    {
        return "[" + display + " , " + completion + "]";
    }
}
