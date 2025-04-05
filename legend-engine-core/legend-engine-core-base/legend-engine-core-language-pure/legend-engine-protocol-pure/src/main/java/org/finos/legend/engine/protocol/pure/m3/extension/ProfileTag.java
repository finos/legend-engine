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

package org.finos.legend.engine.protocol.pure.m3.extension;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;

public class ProfileTag
{
    public String value;
    public SourceInformation sourceInformation;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ProfileTag(@JsonProperty("value") String value, @JsonProperty("sourceInformation") SourceInformation sourceInformation)
    {
        this.value = value;
        this.sourceInformation = sourceInformation;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ProfileTag(String value)
    {
        this.value = value;
        this.sourceInformation = null;
    }
}
