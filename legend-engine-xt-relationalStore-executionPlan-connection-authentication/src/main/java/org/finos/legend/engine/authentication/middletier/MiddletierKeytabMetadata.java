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

package org.finos.legend.engine.authentication.middletier;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MiddletierKeytabMetadata
{
    private String[] usageContexts;
    private String keytabReference;

    public MiddletierKeytabMetadata()
    {
        // jackson
    }

    public MiddletierKeytabMetadata(String keytabReference, String[] usageContexts)
    {
        this.keytabReference = keytabReference;
        this.usageContexts = usageContexts;
    }

    public String[] getUsageContexts()
    {
        return usageContexts;
    }

    public String getKeytabReference()
    {
        return keytabReference;
    }

    public void setKeytabReference(String keytabReference)
    {
        this.keytabReference = keytabReference;
    }

    public String toJSON() throws Exception
    {
        return new ObjectMapper().writeValueAsString(this);
    }
}