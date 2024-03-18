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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PureServerConnectionConfiguration extends ServerConnectionConfiguration
{
    public List<String> allowedOverrideUrls;

    public PureServerConnectionConfiguration()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public PureServerConnectionConfiguration(String host, Integer port)
    {
        this(host, port, null);
    }

    public PureServerConnectionConfiguration(String host, Integer port, List<String> allowedOverrideUrls)
    {
        super(host, port);
        this.allowedOverrideUrls = allowedOverrideUrls;
    }
}
