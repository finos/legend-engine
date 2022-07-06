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

package org.finos.legend.engine.language.pure.dsl.persistence.cloud.grammar.to;

import org.eclipse.collections.api.block.function.Function3;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.to.IPersistenceComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.cloud.context.AwsGluePersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.PersistencePlatform;

import java.util.Collections;
import java.util.List;

public class PersistenceCloudComposerExtension implements IPersistenceComposerExtension
{
    @Override
    public List<Function3<PersistencePlatform, Integer, PureGrammarComposerContext, String>> getExtraPersistencePlatformComposers()
    {
        return Collections.singletonList(((persistencePlatform, indentLevel, context) ->
        {
            if (persistencePlatform instanceof AwsGluePersistencePlatform)
            {
                return HelperPersistenceCloudComposer.renderAwsGluePersistencePlatform((AwsGluePersistencePlatform) persistencePlatform, indentLevel, context);
            }
            return null;
        }));
    }
}
