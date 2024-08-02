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
import org.apache.commons.io.FileUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.predicate.checked.CheckedPredicate;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.repl.autocomplete.CompleterExtension;
import org.finos.legend.engine.repl.client.jline3.JLine3Completer;
import org.finos.legend.engine.repl.client.jline3.JLine3Highlighter;
import org.finos.legend.engine.repl.client.jline3.JLine3Parser;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.ReplExtension;
import org.finos.legend.engine.repl.core.commands.*;
import org.finos.legend.engine.repl.core.legend.LegendInterface;
import org.finos.legend.engine.repl.core.legend.LocalLegendInterface;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.jline.jansi.Ansi.ansi;
import static org.jline.reader.LineReader.BLINK_MATCHING_PAREN;

public class Client
{
    private final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private final LegendInterface legendInterface = new LocalLegendInterface();
    private final Terminal terminal;
    private final LineReader reader;
    private boolean debug = false;
    private MutableList<ReplExtension> replExtensions = Lists.mutable.empty();
    private MutableList<CompleterExtension> completerExtensions = Lists.mutable.empty();
    private ModelState state;
    private final PlanExecutor planExecutor;

    public static void main(String[] args) throws Exception
    {
        new Client(Lists.mutable.empty(), Lists.mutable.empty(), PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build()).loop();
    }

    public MutableList<Command> commands;

    public Client(MutableList<ReplExtension> replExtensions, MutableList<CompleterExtension> completerExtensions, PlanExecutor planExecutor) throws Exception
    {
        this.replExtensions = replExtensions;
        this.completerExtensions = completerExtensions;
        this.planExecutor = planExecutor;
        this.state = new ModelState(this.legendInterface, this.replExtensions);
        this.terminal = TerminalBuilder.terminal();

        this.initialize();
        replExtensions.forEach(e -> e.initialize(this));

        this.printDebug("Legend REPL v" + DeploymentStateAndVersions.sdlc.buildVersion + " (" + DeploymentStateAndVersions.sdlc.commitIdAbbreviated + ")");
        this.printDebug("Press 'Enter' or type 'help' to see the list of available commands.");
        this.printInfo("\n" + Logos.logos.get((int) (Logos.logos.size() * Math.random())) + "\n");

        // NOTE: the order here matters, the default command 'help' should always go first
        // and "catch-all" command 'execute' should always go last
        this.commands = replExtensions
                .flatCollect(ReplExtension::getExtraCommands)
                .withAll(
                        Lists.mutable.with(
                                new Ext(this),
                                new Debug(this),
                                new Graph(this),
                                new Execute(this)
                        )
                );

        this.commands.add(0, new Help(this, this.commands));

        this.reader = LineReaderBuilder.builder()
                .terminal(terminal)
                // Configure history file
                // See https://github.com/jline/jline3/wiki/History
                .variable(LineReader.HISTORY_FILE, this.getHomeDir().resolve("history"))
                .variable(LineReader.HISTORY_FILE_SIZE, 1_000)
                .variable(LineReader.HISTORY_IGNORE, ": *") // make sure empty space(s) are not persisted
                // Disable cursor jumping to opening brace when typing closing brace
                // See https://github.com/jline/jline3/issues/216
                .variable(BLINK_MATCHING_PAREN, false)
                // Make sure hitting <tab> at the beginning of line will insert a tab instead of triggering a completion
                // which will cause error since the completer doesn't handle such case
                // See https://github.com/jline/jline3/wiki/Completion
                .option(LineReader.Option.INSERT_TAB, true)
                // Make sure word navigation works properly with Alt + (left/right) arrow key
                .variable(LineReader.WORDCHARS, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-$")
                // Make sure to not break the completer when exclamation sign is present
                // Do this by disabling history expansion
                // See https://github.com/jline/jline3/issues/246
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                .highlighter(new JLine3Highlighter())
                .parser(new JLine3Parser())
                .completer(new JLine3Completer(this.commands))
                .build();

        this.printInfo("Warming up...");
        this.terminal.flush();
        ((Execute) this.commands.getLast()).execute("1+1");
        this.printInfo("Ready!\n");
    }

    public void loop()
    {
        while (true)
        {
            try
            {
                String line = this.reader.readLine("> ");
                if (line == null || line.equalsIgnoreCase("exit"))
                {
                    System.exit(0);
                    this.persistHistory();
                    break;
                }

                this.reader.getHistory().add(line);

                this.commands.detect(new CheckedPredicate<Command>()
                {
                    @Override
                    public boolean safeAccept(Command c) throws Exception
                    {
                        try
                        {
                            return c.process(line);
                        }
                        catch (RuntimeException e)
                        {
                            throw e;
                        }
                        catch (Exception e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            catch (EngineException e)
            {
                printEngineError(e, this.reader.getBuffer().toString());
            }
            // handle Ctrl + C: if the input is not empty, start a new line; otherwise, exit
            catch (UserInterruptException e)
            {
                String lineContent = this.reader.getBuffer().toString();
                if (lineContent.isEmpty())
                {
                    System.exit(0);
                    this.persistHistory();
                    break;
                }
                else
                {
                    this.loop();
                }
            }
            // handle Ctrl + D: exit
            catch (EndOfFileException e)
            {
                System.exit(0);
                this.persistHistory();
                break;
            }
            catch (Exception e)
            {
                this.printError(e.getMessage());
                if (this.debug)
                {
                    e.printStackTrace();
                }
            }
            finally
            {
                this.persistHistory();
            }
        }
    }

    public void printEngineError(EngineException e, String line)
    {
        int e_start = e.getSourceInformation().startColumn;
        int e_end = e.getSourceInformation().endColumn;
        try
        {
            if (e_start <= line.length())
            {
                String beg = line.substring(0, e_start - 1);
                String mid = line.substring(e_start - 1, e_end);
                String end = line.substring(e_end, line.length());
                AttributedStringBuilder ab = new AttributedStringBuilder();
                ab.style(new AttributedStyle().underlineOff().foregroundOff());
                ab.append(beg);
                ab.style(new AttributedStyle().underline().foreground(AttributedStyle.RED));
                ab.append(mid);
                ab.style(new AttributedStyle().underlineOff().foregroundOff());
                ab.append(end);
                this.printInfo("");
                this.printInfo(ab.toAnsi());
            }
        }
        catch (Exception ex)
        {
            // do nothing
        }
        this.printError(e.getMessage());
        if (this.debug)
        {
            e.printStackTrace();
        }
    }

    public void printDebug(String message)
    {
        this.terminal.writer().println(ansi().fgBrightBlack().a(message).reset());
    }

    public void printInfo(String message)
    {
        this.terminal.writer().println(message);
    }

    public void printError(String message)
    {
        this.terminal.writer().println(ansi().fgRed().a(message).reset());
    }

    private void initialize()
    {
        try
        {
            Path homeDir = this.getHomeDir();
            if (Files.notExists(homeDir))
            {
                Files.createDirectories(homeDir);
            }
        }
        catch (Exception e)
        {
            this.printError("Failed to create home directory at: " + this.getHomeDir().toString());
        }
    }

    private void persistHistory()
    {
        try
        {
            this.reader.getHistory().save();
        }
        catch (Exception e)
        {
            // ignore
        }
    }

    public Path getHomeDir()
    {
        return FileUtils.getUserDirectory().toPath().resolve(".legend/repl");
    }

    public Terminal getTerminal()
    {
        return this.terminal;
    }

    public LegendInterface getLegendInterface()
    {
        return this.legendInterface;
    }

    public boolean isDebug()
    {
        return this.debug;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    public ModelState getModelState()
    {
        return this.state;
    }

    public MutableList<ReplExtension> getReplExtensions()
    {
        return this.replExtensions;
    }

    public PlanExecutor getPlanExecutor()
    {
        return this.planExecutor;
    }

    public MutableList<CompleterExtension> getCompleterExtensions()
    {
        return this.completerExtensions;
    }

    public ObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

    public String getLastCommand()
    {
        return this.getLastCommand(0);
    }

    public String getLastCommand(int skip)
    {
        try
        {
            return this.reader.getHistory().get(this.reader.getHistory().last() - skip);
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
