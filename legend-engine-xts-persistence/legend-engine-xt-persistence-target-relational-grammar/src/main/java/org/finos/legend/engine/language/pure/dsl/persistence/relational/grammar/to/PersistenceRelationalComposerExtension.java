// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.persistence.relational.grammar.to;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.to.IPersistenceComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.sink.RelationalPersistenceTarget;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.sink.PersistenceTarget;

import java.util.Collections;
import java.util.List;

public class PersistenceRelationalComposerExtension implements IPersistenceComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Persistence", "Relational");
    }

    @Override
    public List<Function3<PersistenceTarget, Integer, PureGrammarComposerContext, String>> getExtraPersistenceTargetComposers()
    {
        return Collections.singletonList((persistenceTarget, indentLevel, context) ->
            persistenceTarget instanceof RelationalPersistenceTarget
                ? HelperPersistenceRelationalComposer.renderRelationalPersistenceTarget((RelationalPersistenceTarget) persistenceTarget, indentLevel, context)
                : null);
    }
}
