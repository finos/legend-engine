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
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.StoreProviderPointer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public interface StoreProviderPointerFactory
{
    static StoreProviderPointer parseStoreProviderType(TerminalNode type)
    {
        StoreProviderPointer storeProviderPointer = new StoreProviderPointer();
        if (Objects.isNull(type))
        {
            storeProviderPointer.type = PackageableElementType.STORE;
        }
        else
        {
            storeProviderPointer.type = PackageableElementType.valueOf(type.getText().toUpperCase());
        }
        return storeProviderPointer;
    }

    static StoreProviderPointer create(ParserRuleContext storeProviderPointerContext, SourceInformation sourceInformation)
    {
        List<ParseTree> children = storeProviderPointerContext.children;
        TerminalNode typeContext;
        ParserRuleContext qualifiedNameContext;

        if (children.size() == 2 && children.get(0).getClass().getSimpleName().equals("StoreProviderPointerTypeContext") && children.get(1).getClass().getSimpleName().equals("PackageableElementPointerContext"))
        {
            typeContext = (TerminalNode) ((ParserRuleContext) children.get(0)).children.get(1);
            qualifiedNameContext = (ParserRuleContext) ((ParserRuleContext) children.get(1)).children.get(0);
        }
        else if (children.size() == 1 && children.get(0).getClass().getSimpleName().equals("PackageableElementPointerContext"))
        {
            typeContext = null;
            qualifiedNameContext = (ParserRuleContext) ((ParserRuleContext) children.get(0)).children.get(0);
        }
        else
        {
            throw new IllegalStateException("Unrecognized store provider pointer");
        }
        if (!qualifiedNameContext.getClass().getSimpleName().equals("QualifiedNameContext"))
        {
            throw new IllegalStateException("Unrecognized packageableElementPointer context");
        }
        ParserRuleContext packagePathContext;
        ParserRuleContext identifierContext;
        if (qualifiedNameContext.children.size() == 3)
        {
            packagePathContext = (ParserRuleContext) qualifiedNameContext.children.get(0);
            identifierContext = (ParserRuleContext) qualifiedNameContext.children.get(2);
        }
        else if (qualifiedNameContext.children.size() == 1)
        {
            packagePathContext = null;
            identifierContext = (ParserRuleContext) qualifiedNameContext.children.get(0);
        }
        else
        {
            throw new IllegalStateException("Unrecognized packageableElementPointer context");
        }
        String path = qualifiedNameContext.getText().equals("ModelStore")
                ? "ModelStore"
                : PureGrammarParserUtility.fromQualifiedName(
                        (
                                packagePathContext == null
                                ? Collections.emptyList()
                                : ListIterate
                                        .select(
                                                packagePathContext.children,
                                                ctx -> ctx.getClass().getSimpleName().equals("IdentifierContext"))
                                        .collect(ctx -> (ParserRuleContext) ctx)
                        ), identifierContext);
        StoreProviderPointer storeProviderPointer = StoreProviderPointerFactory.parseStoreProviderType(typeContext);
        storeProviderPointer.path = path;
        storeProviderPointer.sourceInformation = sourceInformation;
        return storeProviderPointer;
    }
}