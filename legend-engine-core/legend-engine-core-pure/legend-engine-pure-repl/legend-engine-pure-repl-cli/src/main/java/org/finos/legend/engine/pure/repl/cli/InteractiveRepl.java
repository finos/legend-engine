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

package org.finos.legend.engine.pure.repl.cli;

import org.finos.legend.engine.pure.repl.core.EvaluationResult;
import org.finos.legend.engine.pure.repl.core.OutputFormatter;
import org.finos.legend.engine.pure.repl.core.ReplCommands;
import org.finos.legend.engine.pure.repl.core.ReplConfiguration;
import org.finos.legend.engine.pure.repl.core.ReplEngine;
import org.finos.legend.engine.pure.repl.core.ReplException;
import org.finos.legend.engine.pure.repl.core.ReplSession;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Interactive REPL using JLine3 for readline support.
 */
public class InteractiveRepl
{
    private static final String PROMPT = "pure> ";
    private static final String CONTINUATION_PROMPT = "   .. ";
    private static final String BANNER =
            " _____                  ____  _____ ____  _     \n" +
            "|  __ \\                |  _ \\|  ___|  _ \\| |    \n" +
            "| |__) |   _ _ __ ___  | |_) | |_  | |_) | |    \n" +
            "|  ___/ | | | '__/ _ \\ |  _ <|  _| |  __/| |    \n" +
            "| |   | |_| | | |  __/ | |_) | |___| |   | |____\n" +
            "|_|    \\__,_|_|  \\___| |____/|_____|_|   |______|\n";

    private final ReplSession session;
    private final ReplEngine engine;
    private final ReplConfiguration config;
    private final OutputFormatter formatter;
    private final ReplCommands commands;

    private Terminal terminal;
    private LineReader reader;
    private boolean running = true;

    public InteractiveRepl(ReplSession session, ReplEngine engine, ReplConfiguration config, OutputFormatter formatter)
    {
        this.session = session;
        this.engine = engine;
        this.config = config;
        this.formatter = formatter;
        this.commands = new ReplCommands(session, engine, formatter);
    }

    /**
     * Runs the interactive REPL loop.
     *
     * @return exit code
     */
    public int run()
    {
        try
        {
            initialize();
            printBanner();
            printHelp();

            while (running)
            {
                try
                {
                    String line = readLine();
                    if (line == null)
                    {
                        // EOF (Ctrl+D)
                        break;
                    }

                    processLine(line);
                }
                catch (UserInterruptException e)
                {
                    // Ctrl+C - if input is empty, exit; otherwise, start new line
                    String buffer = reader.getBuffer().toString();
                    if (buffer.isEmpty())
                    {
                        break;
                    }
                    // Otherwise, continue
                }
                catch (EndOfFileException e)
                {
                    // Ctrl+D
                    break;
                }
                catch (Exception e)
                {
                    formatter.formatError(e.getMessage());
                    if (config.isDebug())
                    {
                        e.printStackTrace();
                    }
                }
            }

            saveHistory();
            formatter.formatMessage("Goodbye.");
            return PureReplMain.EXIT_SUCCESS;
        }
        catch (Exception e)
        {
            System.err.println("Failed to initialize REPL: " + e.getMessage());
            if (config.isDebug())
            {
                e.printStackTrace();
            }
            return PureReplMain.EXIT_CONFIG_ERROR;
        }
        finally
        {
            cleanup();
        }
    }

    /**
     * Initializes the terminal and line reader.
     */
    private void initialize() throws IOException
    {
        this.terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        // Ensure history directory exists
        Path historyPath = config.getHistoryFilePath();
        Path historyDir = historyPath.getParent();
        if (historyDir != null && Files.notExists(historyDir))
        {
            Files.createDirectories(historyDir);
        }

        this.reader = LineReaderBuilder.builder()
                .terminal(terminal)
                // History configuration
                .variable(LineReader.HISTORY_FILE, historyPath)
                .variable(LineReader.HISTORY_FILE_SIZE, config.getHistorySize())
                .variable(LineReader.HISTORY_IGNORE, ": *") // Ignore lines starting with space
                // Disable cursor jump on brace match
                .variable(LineReader.BLINK_MATCHING_PAREN, false)
                // Allow tab insertion at beginning of line
                .option(LineReader.Option.INSERT_TAB, true)
                // Word characters for navigation
                .variable(LineReader.WORDCHARS, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-$::")
                // Disable history expansion (! character issues)
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                .build();
    }

    /**
     * Prints the REPL banner.
     */
    private void printBanner()
    {
        if (!config.isQuiet())
        {
            terminal.writer().println(BANNER);
            terminal.writer().println("Pure REPL v1.0.0");
            terminal.writer().println("Repositories loaded: " + session.getRepositoryCount());
            terminal.writer().println();
        }
    }

    /**
     * Prints initial help message.
     */
    private void printHelp()
    {
        if (!config.isQuiet())
        {
            terminal.writer().println("Type :help for available commands, or enter a Pure expression.");
            terminal.writer().println();
        }
    }

    /**
     * Reads a line from the user, handling multi-line input.
     */
    private String readLine()
    {
        StringBuilder input = new StringBuilder();
        String prompt = PROMPT;
        boolean continuing = false;

        while (true)
        {
            String line = reader.readLine(prompt);

            if (line == null)
            {
                return continuing ? input.toString() : null;
            }

            input.append(line);

            // Check if we need to continue reading (unbalanced braces/parens)
            if (needsContinuation(input.toString()))
            {
                input.append("\n");
                prompt = CONTINUATION_PROMPT;
                continuing = true;
            }
            else
            {
                break;
            }
        }

        String result = input.toString().trim();

        // Add to history if non-empty
        if (!result.isEmpty())
        {
            reader.getHistory().add(result);
        }

        return result;
    }

    /**
     * Checks if the input needs continuation (unbalanced brackets).
     */
    private boolean needsContinuation(String input)
    {
        int braces = 0;
        int parens = 0;
        int brackets = 0;
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < input.length(); i++)
        {
            char c = input.charAt(i);

            // Handle string literals
            if (!inString && (c == '\'' || c == '"'))
            {
                inString = true;
                stringChar = c;
                continue;
            }

            if (inString)
            {
                if (c == stringChar && (i == 0 || input.charAt(i - 1) != '\\'))
                {
                    inString = false;
                }
                continue;
            }

            // Count brackets
            switch (c)
            {
                case '{':
                    braces++;
                    break;
                case '}':
                    braces--;
                    break;
                case '(':
                    parens++;
                    break;
                case ')':
                    parens--;
                    break;
                case '[':
                    brackets++;
                    break;
                case ']':
                    brackets--;
                    break;
            }
        }

        return inString || braces > 0 || parens > 0 || brackets > 0;
    }

    /**
     * Processes a line of input.
     */
    private void processLine(String line)
    {
        if (line.isEmpty())
        {
            return;
        }

        // Handle built-in commands
        if (commands.isCommand(line))
        {
            ReplCommands.CommandResult result = commands.execute(line);
            if (result.shouldExit())
            {
                running = false;
            }
            return;
        }

        // Handle special exit commands (without colon)
        if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line))
        {
            running = false;
            return;
        }

        // Evaluate as expression
        try
        {
            EvaluationResult result = engine.evaluate(line);
            formatter.formatEvaluationResult(result);
        }
        catch (ReplException e)
        {
            formatter.formatError(e.getMessage());
            if (config.isDebug())
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves command history.
     */
    private void saveHistory()
    {
        try
        {
            reader.getHistory().save();
        }
        catch (IOException e)
        {
            // Ignore history save failures
            if (config.isDebug())
            {
                System.err.println("Failed to save history: " + e.getMessage());
            }
        }
    }

    /**
     * Cleans up resources.
     */
    private void cleanup()
    {
        try
        {
            if (terminal != null)
            {
                terminal.close();
            }
        }
        catch (IOException e)
        {
            // Ignore
        }

        if (engine != null)
        {
            engine.shutdown();
        }

        if (session != null)
        {
            session.close();
        }
    }

    /**
     * Clears the terminal screen.
     */
    public void clearScreen()
    {
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();
    }
}
