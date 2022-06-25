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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.geometry.PositionedRectangle;

public class ClassView extends PositionedRectangle
{
    public String id;
    @JsonProperty(value = "class")
    public String _class;
    public Boolean hideProperties;
    public Boolean hideTaggedValues;
    public Boolean hideStereotypes;
    public SourceInformation sourceInformation;
    public SourceInformation classSourceInformation;
}
