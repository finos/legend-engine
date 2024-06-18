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

package org.finos.legend.pure.runtime.java.extension.external.json.shared;

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionCache;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.DeserializationContext;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ObjectFactory;

import java.util.Map;

public class JsonDeserializationContext extends DeserializationContext
{
    private final boolean failOnUnknownProperties;
    private final String typeKeyName;
    private final Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class> typeLookup;

    public JsonDeserializationContext(ConversionCache conversionCache, SourceInformation sourceInformation, ProcessorSupport processorSupport, String typeKeyName, Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class> typeLookup, Boolean failOnUnknownProperties, ObjectFactory objectFactory)
    {
        super(conversionCache, sourceInformation, processorSupport, objectFactory);
        this.failOnUnknownProperties = failOnUnknownProperties;
        this.typeKeyName = typeKeyName;
        this.typeLookup = typeLookup;
    }

    public boolean isFailOnUnknownProperties()
    {
        return this.failOnUnknownProperties;
    }

    public String getTypeKeyName()
    {
        return this.typeKeyName;
    }

    public Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class> getTypeLookup()
    {
        return this.typeLookup;
    }
}
