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

package org.finos.legend.engine.repl.dataCube.commands;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Table;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.commands.Execute;
import org.finos.legend.engine.repl.dataCube.server.REPLServer;
import org.finos.legend.engine.repl.dataCube.shared.DataCubeSampleData;
import org.finos.legend.engine.repl.relational.RelationalReplExtension;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;
import org.finos.legend.engine.repl.shared.DocumentationHelper;
import static org.finos.legend.engine.repl.shared.REPLHelper.ansiDim;
import static org.finos.legend.engine.repl.shared.REPLHelper.ansiGreen;
import static org.finos.legend.engine.repl.shared.REPLHelper.ansiYellow;
import static org.finos.legend.engine.repl.shared.REPLHelper.getLineWidth;
import static org.finos.legend.engine.repl.shared.REPLHelper.wrap;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class DataCubeWalkthrough implements Command
{
    private final DataCube parentCommand;
    private final DocumentationHelper.Walkthrough walkthrough;

    public DataCubeWalkthrough(DataCube parentCommand, Client client, REPLServer replServer)
    {
        this.parentCommand = parentCommand;
        this.walkthrough = new DataCubeWalkthrough1(client, replServer);
    }

    @Override
    public String documentation()
    {
        return "datacube walkthrough (<action: run, next, restart>)";
    }

    @Override
    public String description()
    {
        return "launch REPL walkthrough; specify an action to control the walkthrough player:\n" +
                "1) next: move to the next step\n" +
                "2) prev: move to the previous step\n" +
                "3) restart: restart the walkthrough\n" +
                "4) [no-action]: display the current step";
    }

    @Override
    public Command parentCommand()
    {
        return this.parentCommand;
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("datacube walkthrough"))
        {
            String[] tokens = line.split(" ");
            if (tokens.length < 2 || tokens.length > 3)
            {
                throw new RuntimeException("Command should be used as '" + this.documentation() + "'");
            }
            if (tokens.length == 3)
            {
                String action = tokens[2];
                switch (action)
                {
                    case "next":
                    {
                        this.walkthrough.next();
                        break;
                    }
                    case "prev":
                    {
                        this.walkthrough.prev();
                        break;
                    }
                    case "restart":
                    {
                        this.walkthrough.restart();
                        break;
                    }
                    default:
                    {
                        throw new RuntimeException("Unknown action '" + action + "' for walkthrough player");
                    }
                }
            }
            else
            {
                this.walkthrough.current();
            }
            return true;
        }
        return false;
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        return null;
    }

    public DocumentationHelper.Walkthrough getWalkthrough()
    {
        return walkthrough;
    }

    public static class DataCubeWalkthrough1 extends DocumentationHelper.Walkthrough
    {
        public static final MutableList<String> SELECT_ALL = Lists.mutable.empty();
        public static final MutableList<String> FILTER = Lists.mutable.with("filter(x|$x.Year == 2010)");
        public static final MutableList<String> EXTEND = Lists.mutable.withAll(FILTER).with("extend(~Total: x|$x.Gold + $x.Silver + $x.Bronze)");
        public static final MutableList<String> GROUP_BY = Lists.mutable.withAll(EXTEND).with("groupBy(~Country, ~Total: x|$x.Total: x|$x->sum())");
        public static final MutableList<String> SORT = Lists.mutable.withAll(GROUP_BY).with("sort(~Total->descending())");
        public static final MutableList<String> LIMIT = Lists.mutable.withAll(SORT).with("limit(3)");

        private final REPLServer replServer;
        private String tableName;

        DataCubeWalkthrough1(Client client, REPLServer replServer)
        {
            super(client);
            this.replServer = replServer;
        }

        @Override
        public void beforeStep()
        {
            RelationalDatabaseConnection databaseConnection = ConnectionHelper.getDatabaseConnection(this.client.getModelState().parse(), DataCube.getLocalConnectionPath());

            MutableList<Table> tables = ConnectionHelper.getTables(databaseConnection, this.client.getPlanExecutor()).collect(Collectors.toCollection(Lists.mutable::empty));

            try (
                    InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/finos/legend/engine/repl/dataCube/walkthrough/sport-data.csv");
                    Connection connection = ConnectionHelper.getConnection(databaseConnection, client.getPlanExecutor());
                    Statement statement = connection.createStatement())
            {
                if (tables.anySatisfy(table -> table.name.equals(DataCubeSampleData.SPORT.tableName)))
                {
                    this.tableName = DataCubeSampleData.SPORT.tableName;
                }
                this.tableName = this.tableName == null ? "table" + (tables.size() + 1) : this.tableName;
                // automatically create new table for walkthrough if it doesn't exist or somehow dropped between walkthrough steps
                // NOTE: if the table has been replaced or changed somehow, the walkthrough will fail (we should consider resetting the table)
                if (!tables.anySatisfy(table -> table.name.equals(this.tableName)))
                {
                    Path tempFile = Files.createTempFile("walkthrough-sample-data", ".csv");
                    FileOutputStream fos = new FileOutputStream(tempFile.toFile());
                    IOUtils.copy(Objects.requireNonNull(inputStream, "Can't extract sample data for walkthrough"), fos);
                    statement.executeUpdate(DatabaseManager.fromString(databaseConnection.type.name()).relationalDatabaseSupport().load(tableName, tempFile.toString()));
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void afterStep()
        {
            // do nothing
        }

        @Override
        protected MutableList<Function0<Void>> getSteps()
        {
            return Lists.mutable.with(
                    () ->
                    {
                        this.printHeader();
                        this.println(wrap("Welcome! In this walkthrough, we will guide you through how to use Legend REPL to explore the 2000-2012 Olympics medal dataset. " +
                                "This dataset has been loaded to table '" + this.tableName + "' of the local database " + DataCube.getLocalDatabasePath()));
                        this.println(ansiDim("[!] If you want work on your own data, check out the 'load' command."));
                        this.printFooter();
                        return null;
                    },
                    () ->
                    {
                        this.printHeader();
                        this.println("First, let's look at the full dataset with the following command:");
                        this.printCommand(this.query(SELECT_ALL));
                        this.runQuery(this.query(SELECT_ALL));
                        this.println("A quick analysis of this command:");
                        this.printCommand(this.query(SELECT_ALL, 0), true);
                        this.println("  ^ specifies which database and table to access");
                        this.printCommand(this.query(SELECT_ALL, 1), true);
                        this.println("                                              ^ specifies the execution context");
                        this.printFooter();
                        return null;
                    },
                    () ->
                    {
                        this.printHeader();
                        this.println(wrap("Now, to explore this dataset, either continue on with the REPL or build query with Data Cube, an " +
                                "advanced GUI with feature-rich support to generate data report and visualization. Simply use the command below:"));
                        this.printCommand("show");
                        this.println("");
                        this.println(ansiDim(wrap("[!] Bear in mind that Data Cube will operate on the result of the last executed query. " +
                                "As such, the 'show' command comes in handy when one prefer to wrangle and further prepare the data in the REPL " +
                                "before launching Data Cube; if one just want to jump straight into Data Cube, use the following shortcut command:")));
                        this.printCommand("datacube table " + this.tableName);
                        this.println("");
                        this.println(wrap("Try out the commands or proceed to the next step to launch Data Cube..."));
                        this.printFooter();
                        return null;
                    },
                    () ->
                    {
                        this.printHeader();
                        this.println(wrap("There are a lot to cover for Data Cube. Unfortunately, an in-depth tutorial is out of " +
                                "the scope of this walkthrough. So now, we will launch Data Cube and let you explore on your own for a bit. " +
                                "After that, let's come back here to continue with the REPL walkthrough, we will check out some common query syntaxes."));
                        this.printCommand("datacube table " + this.tableName);
                        this.client.getTerminal().flush();
                        Show.run(this.query(SELECT_ALL), this.client, this.replServer);
                        this.println("");
                        this.printFooter();
                        return null;
                    },
                    () ->
                    {
                        this.printHeader();
                        this.println(wrap("Let's assume we are only interested in the 2010 Winter Olympics. Apply a filter on the year like below:"));
                        this.printCommand(this.query(FILTER, 1), true);
                        this.runQuery(this.query(FILTER));
                        this.printFooter();
                        return null;
                    },
                    () ->
                    {
                        this.printHeader();
                        this.println(wrap("Next, let's find out the total number of medals that each athlete got. Create an extended column (or derivation) like below:"));
                        this.printCommand(this.query(EXTEND, 2), true);
                        this.runQuery(this.query(EXTEND));
                        this.printFooter();
                        return null;
                    },
                    () ->
                    {
                        this.printHeader();
                        this.println(wrap("Now we'll group the result by country and find out the total number of medals for each country:"));
                        this.printCommand(this.query(GROUP_BY, 3), true);
                        this.runQuery(this.query(GROUP_BY));
                        this.printFooter();
                        return null;
                    },
                    () ->
                    {
                        this.printHeader();
                        this.println(wrap("Next, let's sort the result by the total number of medals:"));
                        this.printCommand(this.query(SORT, 4), true);
                        this.runQuery(this.query(SORT));
                        this.printFooter();
                        return null;
                    },
                    () ->
                    {
                        this.printHeader();
                        this.println(wrap("Finally, let's truncate the result to find out the top finishers:"));
                        this.println(ansiDim(wrap("[!] You might fact-check this result and found out that it mismatched with the official statistics! " +
                                "This might be due to the fact that team medals are counted multiple times, one for each member.")));
                        this.printCommand(this.query(LIMIT, 5), true);
                        this.runQuery(this.query(LIMIT));
                        this.printFooter();
                        return null;
                    },
                    () ->
                    {
                        this.printHeader();
                        this.println(wrap("Congratulations! You have completed the walkthrough. There are a lot more hidden gems " +
                                "packed with Legend REPL. We hope that you will discover and have the chance to experiment with them."));
                        this.println(ansiDim(wrap("[i] Generally, interactions one would expect from a typical REPL should be supported: " +
                                "using Ctrl+C to cancel, using Ctrl+D or 'exit' to quit, using 'clear' command to flush screen, navigating command " +
                                "history with ArrowUp/Down key, etc.")));
                        this.println("");
                        this.println(wrap("The function documentation lookup tool can be quite helpful, for example, to view documentation and " +
                                "usages for function 'filter', use the following command:"));
                        this.printCommand("doc collection/iteration/filter");
                        this.println(ansiDim(printRule(null)));
                        this.println(DocumentationHelper.generateANSIFunctionDocumentation(this.client.getFunctionDocumentation("meta::pure::functions::relation::filter"), this.client.getDocumentationAdapterKeys()));
                        this.client.addCommandToHistory("doc collection/iteration/filter");
                        this.println(ansiDim(printRule(null)));
                        this.println("");
                        this.println(wrap("Also, don't forget to hit the 'Tab' key while typing up an expression, the compiler can help validate " +
                                "your query as well as give you auto-complete suggestions. For example, in the command above, you can type 'doc filter' " +
                                "then hit 'Tab' and a list of suggestions should show up for you to choose from (navigate ths options by hitting 'Tab')."));
                        this.println("");
                        this.println("Hope you find this walkthrough fun and instructive! (here's one last useful command for today :D)");
                        this.printCommand("Enjoy your Legend REPL experience!");
                        this.println("");
                        this.println(ansiDim(printRule(null)));
                        this.println(ansiYellow("[!]") + " Next Step : Run command " + ansiGreen("datacube walkthrough restart"));
                        this.println(ansiDim(printRule(null)));
                        return null;
                    }
            );
        }

        private RelationalReplExtension getRelationalExtension()
        {
            return this.client.getReplExtensions().selectInstancesOf(RelationalReplExtension.class).getFirst();
        }

        public String getTableName()
        {
            return this.tableName;
        }

        private String query(MutableList<String> parts, Integer indexToHighlight)
        {
            MutableList<String> allParts = Lists.mutable
                    .with("#>{" + DataCube.getLocalDatabasePath() + "." + this.tableName + "}#")
                    .withAll(parts)
                    .with("from(" + DataCube.getLocalRuntimePath() + ")");
            if (indexToHighlight != null && indexToHighlight >= 0 && indexToHighlight < allParts.size())
            {
                allParts.set(indexToHighlight, ansiGreen(allParts.get(indexToHighlight)));
            }
            return allParts.makeString("->");
        }

        public String query(MutableList<String> parts)
        {
            return query(parts, null);
        }

        private static String printRule(String label)
        {
            return StringUtils.rightPad(label != null ? ("-- " + label + " ") : "", getLineWidth(), "-");
        }

        private void println(String text)
        {
            this.client.println(text);
        }

        private void printHeader()
        {
            this.println(ansiYellow("[>]") + " Step " + (this.getCurrentStep() + 1) + "/" + this.getStepCount());
            this.println(ansiDim(printRule(null)));
        }

        private void printCommand(String command)
        {
            this.printCommand(command, false);
        }

        private void printCommand(String command, boolean disableAutoHighlight)
        {
            this.println(ansiDim("> ") + (disableAutoHighlight ? command : ansiGreen(command)));
        }

        private void printFooter()
        {
            this.println(ansiDim(printRule(null)));
            this.println(ansiYellow("[!]") + " Next Step : Run command " + ansiGreen("datacube walkthrough next"));
            this.println(ansiDim(printRule(null)));
        }

        private void runQuery(String command)
        {
            this.println("");
            RelationalReplExtension relationalExtension = getRelationalExtension();
            int currentResultDisplayMaxRowSize = relationalExtension.getMaxRowSize();
            relationalExtension.setMaxRowSize(10);
            this.client.getTerminal().flush(); // flush the terminal before heavy computation to avoid the terminal from being unresponsive
            this.client.println(this.client.commands.selectInstancesOf(Execute.class).getAny().execute(command));
            this.client.addCommandToHistory(command);
            relationalExtension.setMaxRowSize(currentResultDisplayMaxRowSize);
            this.println("");
        }
    }
}
