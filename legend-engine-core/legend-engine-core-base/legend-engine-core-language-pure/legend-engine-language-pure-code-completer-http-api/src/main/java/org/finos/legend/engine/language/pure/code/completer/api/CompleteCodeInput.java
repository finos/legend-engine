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

import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;

public class CompleteCodeInput
{
    public PureModelContext model;
    public String codeBlock;
    public Integer offset;

    public CompleteCodeInput()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }
}