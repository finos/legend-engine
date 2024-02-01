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

package org.finos.legend.engine.repl.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtensionLoader;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.repl.client.jline3.JLine3Completer;
import org.finos.legend.engine.repl.client.jline3.JLine3Highlighter;
import org.finos.legend.engine.repl.REPLInterface;
import org.finos.legend.engine.repl.client.jline3.JLine3Parser;
import org.finos.legend.engine.repl.local.LocalREPL;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.sql.*;
import java.util.List;

import static org.finos.legend.engine.repl.database.MetadataReader.getTables;
import static org.finos.legend.engine.repl.grid.Grid.prettyGridPrint;

public class Client
{
    private static final PlanExecutor planExecutor;

    public static final REPLInterface replInterface = new LocalREPL();

    public static MutableList<String> state = Lists.mutable.empty();

    public static int count;

    public static List<PureGrammarComposerExtension> loader = PureGrammarComposerExtensionLoader.extensions();

    public static Terminal terminal;

    public static boolean debug = false;

    public static ObjectMapper objectMapper = new ObjectMapper();


    static
    {
        System.setProperty("legend.test.h2.port", "1975");

        planExecutor = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build();

        count = getTables(getConnection()).size();

    }


    public static void main(String[] args) throws IOException
    {
        terminal = TerminalBuilder.terminal();

        terminal.writer().println("\n" + Logos.logos.get((int) (Logos.logos.size() * Math.random())) + "\n");

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .highlighter(new JLine3Highlighter())
                .parser(new JLine3Parser())//new DefaultParser().quoteChars(new char[]{'"'}))
                .completer(new JLine3Completer())
                .build();

        terminal.writer().println("Warming up...");
        terminal.flush();
        execute("1+1");
        terminal.writer().println("Ready!\n");

        while (true)
        {
            String line = reader.readLine("> ");
            if (line == null || line.equalsIgnoreCase("exit"))
            {
                break;
            }

            reader.getHistory().add(line);

            try
            {
                if (line.isEmpty())
                {
                    terminal.writer().println("Commands:");
                    terminal.writer().println("  load <path> <destination>");
                    terminal.writer().println("  db");
                    terminal.writer().println("  graph [<packagePath>]");
                    terminal.writer().println("  debug");
                }
                else if (line.startsWith("debug"))
                {
                    String[] cmd = line.split(" ");
                    if (cmd.length == 1)
                    {
                        debug = !debug;
                    }
                    else
                    {
                        debug = Boolean.parseBoolean(cmd[1]);
                    }
                    terminal.writer().println("debug: " + debug);
                }
                else if (line.startsWith("load "))
                {
                    String path = line.substring("load ".length()).trim();

                    String tableName = "test" + count++;

                    try (Connection connection = getConnection())
                    {
                        try (Statement statement = connection.createStatement())
                        {
                            statement.executeUpdate("CREATE TABLE " + tableName + " AS SELECT * FROM CSVREAD('" + path + "');");
                        }
                    }
                }
                else if (line.startsWith("graph"))
                {
                    MutableList<String> all = Lists.mutable.with(line.split(" "));
                    MutableList<String> showArgs = all.subList(1, all.size());
                    if (showArgs.isEmpty())
                    {
                        PureModelContextData d = replInterface.parse(buildState().makeString("\n"));
                        ListIterate.forEach(
                                ListIterate.collect(
                                        ListIterate.select(d.getElements(), c -> !c._package.equals("__internal__")),
                                        c ->
                                        {
                                            AttributedStringBuilder ab = new AttributedStringBuilder();
                                            ab.append("   ");
                                            drawPath(ab, c._package, c.name);
                                            return ab.toAnsi();
                                        }
                                ),
                                e -> terminal.writer().println(e));
                    }
                    else
                    {
                        PureModelContextData d = replInterface.parse(buildState().makeString("\n"));
                        PackageableElement element = ListIterate.select(d.getElements(), c -> c.getPath().equals(showArgs.getFirst())).getFirst();
                        String result = ListIterate.flatCollect(loader, c -> c.getExtraPackageableElementComposers().collect(x -> x.apply(element, PureGrammarComposerContext.Builder.newInstance().build()))).select(Predicates.notNull()).getFirst();
                        terminal.writer().println(result);
                    }
                }
                else if (line.startsWith("db"))
                {
                    try (Connection connection = getConnection())
                    {
                        terminal.writer().println(
                                getTables(connection).collect(c -> c.schema + "." + c.name + "(" + c.columns.collect(col -> col.name + " " + col.type).makeString(", ") + ")").makeString("\n")
                        );
                    }
                }
                else
                {
                    terminal.writer().println(execute(line));
                }
            }
            catch (EngineException e)
            {
                printError(e, line);
            }
            catch (Exception ee)
            {
                terminal.writer().println(ee.getMessage());
                if (debug)
                {
                    ee.printStackTrace();
                }
            }
        }
    }

    public static void printError(EngineException e, String line)
    {
        int e_start = e.getSourceInformation().startColumn;
        int e_end = e.getSourceInformation().endColumn;
        if (e_start <= line.length())
        {
            String beg = line.substring(0, e_start - 1);
            String mid = line.substring(e_start - 1, e_end);
            String end = line.substring(e_end, line.length());
            AttributedStringBuilder ab = new AttributedStringBuilder();
            ab.style(new AttributedStyle().underlineOff().boldOff().foreground(0, 200, 0));
            ab.append(beg);
            ab.style(new AttributedStyle().underline().bold().foreground(200, 0, 0));
            ab.append(mid);
            ab.style(new AttributedStyle().underlineOff().boldOff().foreground(0, 200, 0));
            ab.append(end);
            terminal.writer().println("");
            terminal.writer().println(ab.toAnsi());
        }
        terminal.writer().println(e.getMessage());
        if (debug)
        {
            e.printStackTrace();
        }
    }

    private static Connection getConnection()
    {
        RelationalStoreExecutor r = (RelationalStoreExecutor) planExecutor.getExecutorsOfType(StoreType.Relational).getFirst();
        return r.getStoreState().getRelationalExecutor().getConnectionManager().getTestDatabaseConnection();
    }

    public static void drawPath(AttributedStringBuilder ab, String path)
    {
        MutableList<String> spl = Lists.mutable.with(path.split("::"));
        if (path.endsWith("::"))
        {
            spl.add("");
        }
        if (spl.size() == 1)
        {
            drawPath(ab, null, spl.get(0));
        }
        else
        {
            drawPath(ab, spl.subList(0, spl.size() - 1).makeString("::"), spl.getLast());
        }
    }

    public static void drawPath(AttributedStringBuilder ab, String _package, String name)
    {
        ab.style(new AttributedStyle().foreground(100, 100, 100).italic());
        ab.append(_package == null || _package.isEmpty() ? "" : _package + "::");
        ab.style(new AttributedStyle().foreground(200, 200, 200).italic());
        ab.append(name);
    }

    public static void drawCommand(AttributedStringBuilder ab, String command)
    {
        ab.style(new AttributedStyle().foreground(0, 200, 0).italic());
        ab.append(command);
    }





    public static MutableList<String> buildState()
    {
        MutableList<String> res = Lists.mutable.withAll(state);

        res.add("###Relational\n" +
                "Database test::TestDatabase" +
                "(" +
                getTables(getConnection()).collect(table -> "Table " + table.name + "(" + table.columns.collect(c -> (c.name.contains(" ") ? "\"" + c.name + "\"" : c.name) + " " + c.type).makeString(",") + ")").makeString("\n") +
                ")\n");

        res.add("###Connection\n" +
                "RelationalDatabaseConnection test::testConnection\n" +
                "{\n" +
                "   store: test::TestDatabase;" +
                "   specification: LocalH2{};" +
                "   type: H2;" +
                "   auth: DefaultH2;" +
                "}\n");

        res.add("###Runtime\n" +
                "Runtime test::test\n" +
                "{\n" +
                "   mappings : [];" +
                "   connections:\n" +
                "   [\n" +
                "       test::TestDatabase : [connection: test::testConnection]\n" +
                "   ];\n" +
                "}\n");

        return res;
    }


    public static String execute(String txt)
    {
        String code = "###Pure\n" +
                "function a::b::c::d():Any[*]\n{\n" + txt + ";\n}";

        PureModelContextData d = replInterface.parse(buildState().makeString("\n") + code);
        if (debug)
        {
            try
            {
                terminal.writer().println((objectMapper.writeValueAsString(d)));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        // Compile
        PureModel pureModel = replInterface.compile(d);
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
        if (debug)
        {
            terminal.writer().println(">> " + extensions.collect(Root_meta_pure_extension_Extension::_type).makeString(", "));
        }

        // Plan
        Root_meta_pure_executionPlan_ExecutionPlan plan = replInterface.generatePlan(pureModel, debug);
        String planStr = PlanGenerator.serializeToJSON(plan, "vX_X_X", pureModel, extensions, LegendPlanTransformers.transformers);
        if (debug)
        {
            terminal.writer().println(planStr);
        }

        // Execute
        Result res = planExecutor.execute(planStr);
        if (res instanceof RelationalResult)
        {
            return prettyGridPrint((RelationalResult) res, 60);
//            Serializer s = new RelationalResultToCSVSerializer((RelationalResult) res);
//            return s.flush().toString();
        }
        else if (res instanceof ConstantResult)
        {
            return ((ConstantResult) res).getValue().toString();
        }
        throw new RuntimeException(res.getClass() + " not supported!");
    }


}
