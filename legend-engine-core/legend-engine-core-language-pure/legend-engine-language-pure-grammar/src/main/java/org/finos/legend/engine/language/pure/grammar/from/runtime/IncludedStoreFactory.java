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

package org.finos.legend.engine.language.pure.grammar.from.runtime;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.IncludedStoreCarrier;

import java.util.List;
import java.util.Objects;

public interface IncludedStoreFactory
{

    String getIncludedStoreCarrierType();

    IncludedStoreCarrier create(String path, SourceInformation sourceInformation);

    static String parseIncludedStoreCarrierIdentifier(ParserRuleContext embeddedDataContext)
    {
        if (Objects.isNull(embeddedDataContext))
        {
            return "store";
        }
        else
        {
            List<ParseTree> children = embeddedDataContext.children;
            if (children.size() < 4 || !children.get(0).getClass().getSimpleName().equals("GetStoresFromContext") || !(children.get(1) instanceof TerminalNode))
            {
                throw new IllegalStateException("Unrecognized included store carrier pattern");
            }
            return children.get(1).getText().toLowerCase();
        }
    }
}