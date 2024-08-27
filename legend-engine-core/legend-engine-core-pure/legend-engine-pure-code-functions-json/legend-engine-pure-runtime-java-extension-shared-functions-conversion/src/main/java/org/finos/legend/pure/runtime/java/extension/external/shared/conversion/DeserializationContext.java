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

package org.finos.legend.pure.runtime.java.extension.external.shared.conversion;

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class DeserializationContext extends ConversionContext
{
    private final ObjectFactory objectFactory;

    public DeserializationContext(ConversionCache conversionCache, SourceInformation sourceInformation, ProcessorSupport processorSupport, ObjectFactory objectFactory)
    {
        super(conversionCache, sourceInformation, processorSupport);
        this.objectFactory = objectFactory;
    }

    public ObjectFactory getObjectFactory()
    {
        return this.objectFactory;
    }
}
