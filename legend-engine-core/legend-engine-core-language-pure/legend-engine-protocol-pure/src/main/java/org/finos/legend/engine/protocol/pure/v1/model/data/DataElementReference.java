// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.data;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;

@JsonPropertyOrder({"sourceInformation", "dataElement", "dataElementPointer"})
public class DataElementReference extends EmbeddedData
{
    public PackageableElementPointer dataElementPointer;

    @JsonSetter("dataElement")
    public void setDataElement(String dataElement)
    {
        this.dataElementPointer = new PackageableElementPointer(PackageableElementType.DATA, dataElement, this.sourceInformation);
    }

    @JsonSetter("dataElementPointer")
    public void setDataElementPointer(PackageableElementPointer dataElementPointer)
    {
        this.dataElementPointer = dataElementPointer;
    }

}
