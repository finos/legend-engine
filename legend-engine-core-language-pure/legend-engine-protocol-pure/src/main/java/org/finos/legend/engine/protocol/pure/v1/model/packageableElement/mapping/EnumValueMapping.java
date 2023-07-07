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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

public class EnumValueMapping
{
    public String enumValue;
    /**
     * NOTE: we have this custom deserializer because we have a few different format for enum value mapping source value.
     * In the later version of the protocol, we have formalized it and make this into a structured object, as such we need to account for that here
     */
    @JsonDeserialize(contentUsing = EnumValueMappingSourceValueDeserializer.class)
    @JsonSerialize(contentUsing = EnumValueMappingSourceValueSerializer.class)
    public List<Object> sourceValues;
}
