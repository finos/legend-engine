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

package org.finos.legend.engine.language.pure.grammar.to.data;

import org.finos.legend.engine.language.pure.grammar.from.data.ServiceStoreEmbeddedDataParser;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.ContentWithType;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ServiceStoreEmbeddedData;

public class ServiceStoreEmbeddedDataComposer
{
    public static ContentWithType composeServiceStoreEmbeddedData(EmbeddedData embeddedData, PureGrammarComposerContext context)
    {
        if (embeddedData instanceof ServiceStoreEmbeddedData)
        {
            ServiceStoreEmbeddedData serviceStoreEmbeddedData = (ServiceStoreEmbeddedData) embeddedData;
            String content = new HelperServiceStoreEmbeddedDataComposer(context).visitServiceStoreEmbeddedData(serviceStoreEmbeddedData);
            return new ContentWithType(ServiceStoreEmbeddedDataParser.TYPE, content);
        }
        else
        {
            return null;
        }
    }
}
