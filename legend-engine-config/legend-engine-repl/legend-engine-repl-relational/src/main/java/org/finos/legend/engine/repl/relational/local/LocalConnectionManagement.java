// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl.relational.local;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.RelationalGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;

public class LocalConnectionManagement
{
    private Client client;
    private MutableList<String> connectionsForDynamicDBs = Lists.mutable.empty();

    public LocalConnectionManagement(Client client)
    {
        this.client = client;
    }

    public void addLocalConnection(LocalConnectionType type, String name)
    {
        this.client.getModelState().addElement(
                type == LocalConnectionType.DuckDB ?
                        "###Connection\n" +
                                "RelationalDatabaseConnection local::" + name + "Connection\n" +
                                "{\n" +
                                "   specification: DuckDB{path:" + PureGrammarComposerUtility.convertString(this.client.getHomeDir().resolve("duck").toString(), true) + ";};" +
                                "   type: DuckDB;" +
                                "   auth: Test;" +
                                "}\n" :
                        "###Connection\n" +
                                "RelationalDatabaseConnection local::" + name + "Connection\n" +
                                "{\n" +
                                "   specification: LocalH2{};" +
                                "   type: H2;" +
                                "   auth: DefaultH2;" +
                                "}\n"
        );

        connectionsForDynamicDBs.add(name);
    }

    public MutableList<String> generateDynamicContent(String code)
    {
        PureModelContextData pureModelContextData = client.getLegendInterface().parse(code);
        return this.connectionsForDynamicDBs.flatCollect(conn ->
        {
            String connectionPath = "local::" + conn + "Connection";
            RelationalDatabaseConnection db = ConnectionHelper.getDatabaseConnection(pureModelContextData, connectionPath);
            MutableList<String> res = Lists.mutable.empty();

            Database database = ConnectionHelper.getDatabase(db, "local", conn + "Database", this.client.getPlanExecutor());

            String dbElement = "###Relational\n" + PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build())
                    .render(database, RelationalGrammarParserExtension.NAME);

            res.add(dbElement);

            res.add("###Runtime\n" +
                    "Runtime local::" + conn + "Runtime\n" +
                    "{\n" +
                    "   mappings : [];" +
                    "   connections:\n" +
                    "   [\n" +
                    "       local::" + conn + "Database : [connection: " + connectionPath + "]\n" +
                    "   ];\n" +
                    "}\n");
            return res;
        });
    }
}