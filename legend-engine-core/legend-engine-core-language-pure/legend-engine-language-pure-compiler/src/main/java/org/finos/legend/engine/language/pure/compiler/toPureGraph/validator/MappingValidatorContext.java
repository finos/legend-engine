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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.validator;

import java.util.Map;

public class MappingValidatorContext
{
   private Map<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping, Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping>> mappingRootMappingAndClassMappingId;
   private Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> pureMappings;
   private Map<String, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping> protocolMappingsByName;

    public MappingValidatorContext(Map<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping, Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping>> mappingRootMappingAndClassMappingId, Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> pureMappings, Map<String, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping> protocolMappingsByName)
    {
        this.mappingRootMappingAndClassMappingId = mappingRootMappingAndClassMappingId;
        this.pureMappings = pureMappings;
        this.protocolMappingsByName = protocolMappingsByName;

    }

    public Map<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping, Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping>>  getMappingRootMappingAndClassMappingId()
    {
        return mappingRootMappingAndClassMappingId;
    }

    public Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> getPureMappings()
    {
        return pureMappings;
    }

    public org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping getProtocolMappingsByNameWithID(String mappingID)
    {
        if (protocolMappingsByName == null)
        {
            return null;
        }
        else
        {
            return protocolMappingsByName.get(mappingID);
        }
    }


}
