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

package org.finos.legend.engine.language.pure.modelManager.sdlc.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.codecs.pojo.annotations.BsonIgnore;

public class MetaDataServerConfiguration
{
    @Deprecated
    public String host;
    @Deprecated
    public Integer port;
    // NOTE: since the getters for the following properties are ignored, we must annotating these with @JsonProperty
    // to avoid total ignorance
    // See https://fasterxml.github.io/jackson-annotations/javadoc/2.9/com/fasterxml/jackson/annotation/JsonIgnore.html
    @JsonProperty
    public ServerConnectionConfiguration alloy;
    @JsonProperty
    public ServerConnectionConfiguration pure;
    @JsonProperty
    public ServerConnectionConfiguration sdlc;

    public MetaDataServerConfiguration()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public MetaDataServerConfiguration(ServerConnectionConfiguration pure)
    {
        this(pure, null, null);
    }

    public MetaDataServerConfiguration(ServerConnectionConfiguration pure, ServerConnectionConfiguration alloy)
    {
        this(pure, alloy, null);
    }

    public MetaDataServerConfiguration(ServerConnectionConfiguration pure, ServerConnectionConfiguration alloy, ServerConnectionConfiguration sdlc)
    {
        this.pure = pure;
        this.alloy = alloy;
        this.sdlc = sdlc;
    }

    @JsonIgnore
    @BsonIgnore
    public ServerConnectionConfiguration getSdlc()
    {
        return sdlc;
    }

    @JsonIgnore
    @BsonIgnore
    public ServerConnectionConfiguration getAlloy()
    {
        return alloy;
    }

    @JsonIgnore
    @BsonIgnore
    public ServerConnectionConfiguration getPure()
    {
        if (pure == null)
        {
            return new ServerConnectionConfiguration(this.host, this.port);
        }
        return pure;
    }
}
