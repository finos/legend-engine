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

package org.finos.legend.engine.ide.api.execution.function.manager;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class HttpInformation
{
    private final String contentType;
    private final String contentDisposition;

    public HttpInformation(String contentType, String contentDisposition)
    {
        this.contentType = contentType;
        this.contentDisposition = contentDisposition;
    }

    public String getContentType()
    {
        return this.contentType;
    }

    public String getContentDisposition()
    {
        return this.contentDisposition;
    }

    public static HttpInformation fromFunction(CoreInstance function, final ProcessorSupport processorSupport)
    {

        CoreInstance contentTypeObj = function.getValueForMetaPropertyToMany(M3Properties.taggedValues).detect(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance each)
            {
                return "contentType".equals(Instance.getValueForMetaPropertyToOneResolved(each, M3Properties.tag, processorSupport).getName());
            }
        });
        CoreInstance contentDisposition = function.getValueForMetaPropertyToMany(M3Properties.taggedValues).detect(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance each)
            {
                return "contentDisposition".equals(Instance.getValueForMetaPropertyToOneResolved(each, M3Properties.tag, processorSupport).getName());
            }
        });
        return new HttpInformation(
                contentTypeObj == null ? null : contentTypeObj.getValueForMetaPropertyToOne(M3Properties.value).getName(),
                contentDisposition == null ? null : contentDisposition.getValueForMetaPropertyToOne(M3Properties.value).getName()
        );
    }
}
