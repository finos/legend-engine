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

import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.sink.RelationalPersistenceTarget;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Bitemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Nontemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Temporality;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.TemporalityVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Unitemporal;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperPersistenceRelationalComposer
{
    private HelperPersistenceRelationalComposer()
    {
    }

    public static String renderRelationalPersistenceTarget(RelationalPersistenceTarget persistenceTarget, int indentLevel, PureGrammarComposerContext context)
    {
        return getTabString(indentLevel) + "Relational\n" +
            getTabString(indentLevel) + "#{\n" +
            renderTable(persistenceTarget.table, indentLevel + 1) +
            renderTemporality(persistenceTarget.temporality, indentLevel + 1) +
            getTabString(indentLevel) + "}#";
    }

    public static String renderTable(String table, int indentLevel)
    {
        return getTabString(indentLevel) + "table: " + table + ";\n";
    }

    public static String renderTemporality(Temporality temporality, int indentLevel)
    {
        return getTabString(indentLevel) + "temporality: " + temporality.accept(new TemporalityComposer(indentLevel));
    }

    private static class TemporalityComposer implements TemporalityVisitor<String>
    {
        private final int indentLevel;

        private TemporalityComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visitNontemporal(Nontemporal val)
        {
            return "None;\n";
        }

        @Override
        public String visitUnitemporal(Unitemporal val)
        {
            return "Unitemporal" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visitBitemporal(Bitemporal val)
        {
            return "Bitemporal\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel) + "}\n";
        }
    }
}
