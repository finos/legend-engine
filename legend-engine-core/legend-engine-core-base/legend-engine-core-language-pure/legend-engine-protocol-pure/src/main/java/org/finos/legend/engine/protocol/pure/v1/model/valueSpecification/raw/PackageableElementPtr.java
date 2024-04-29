//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;

import java.io.IOException;

public class PackageableElementPtr extends One
{
    protected static ObjectMapper om = PureProtocolObjectMapperFactory.getNewObjectMapper();

    public String fullPath;

    public PackageableElementPtr()
    {

    }

    public PackageableElementPtr(String fullPath)
    {
        this.fullPath = fullPath;
    }

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }


    protected static ValueSpecification convert(JsonNode node) throws IOException
    {
        JsonNode name = node.get("fullPath");
        ValueSpecification result = new PackageableElementPtr(name.asText());
        JsonNode sourceInformation = node.get("sourceInformation");
        if (sourceInformation != null)
        {
            result.sourceInformation = om.treeToValue(sourceInformation, SourceInformation.class);
        }
        return result;
    }
}
