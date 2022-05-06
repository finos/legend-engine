// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public class ServiceParameter
{
    public String name;
    public TypeReference type;
    public Location location;

    public Boolean allowReserved;
    public Boolean required;
    @JsonAlias({"_enum"})
    public String enumeration;
    public SerializationFormat serializationFormat;

    public SourceInformation sourceInformation;
}
