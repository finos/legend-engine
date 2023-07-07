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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

import java.util.List;

public class EnumerationMapping
{
    public String id;
    public String enumeration;
    public List<EnumValueMapping> enumValueMappings;
    @Deprecated
    // source type was introduced in v1_10_0 and have been deprecated since, but we have to keep it around for backward compatibility
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String sourceType;
    public SourceInformation sourceInformation;
}
