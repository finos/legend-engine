// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = DataSpacePackageableElementExecutable.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DataSpacePackageableElementExecutable.class, name = "dataSpacePackageableElementExecutable"),
        @JsonSubTypes.Type(value = DataSpaceTemplateExecutable.class, name = "dataSpaceTemplateExecutable"),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DataSpaceExecutable
{
    // NOTE: this is subjected to change depending on how we want to embed executables information
    public String id;
    public String executionContextKey;
    public String title;
    public String description;
    public SourceInformation sourceInformation;
}
