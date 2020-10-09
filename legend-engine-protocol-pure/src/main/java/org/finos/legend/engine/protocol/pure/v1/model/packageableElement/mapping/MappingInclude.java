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

public class MappingInclude
{
    // NOTE: we have 2 ways of expressing includedMapping path, `includedMapping` is the fullPath and we also split it into package and name
    // the latter is for backward compatibility
    private String includedMapping; // set the field as private so Jackson has to use getter and setter
    @Deprecated
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // exclude these fields during serialization
    public String includedMappingName;
    @Deprecated
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // exclude these fields during serialization
    public String includedMappingPackage;
    public String sourceDatabasePath;
    public String targetDatabasePath;
    public SourceInformation sourceInformation;

    /**
     * There are cases when the model is serialized and not processed, hence this method provides a backward compatible
     * way to extract the includedMapping path.
     */
    public String getIncludedMapping()
    {
        return this.includedMapping != null ? this.includedMapping : this.includedMappingPackage + "::" + this.includedMappingName;
    }

    public void setIncludedMapping(String includedMapping)
    {
        this.includedMapping = includedMapping;
    }
}
