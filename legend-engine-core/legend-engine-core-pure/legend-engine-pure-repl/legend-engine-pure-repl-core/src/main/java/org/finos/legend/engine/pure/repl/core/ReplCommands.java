// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.pure.repl.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles built-in REPL commands (prefixed with :).
 */
public class ReplCommands
{
    private final ReplSession session;
    private final ReplEngine engine;
    private final OutputFormatter formatter;

    // Command registry
    private final Map<String, CommandHandler> commands = new HashMap<>();

    public ReplCommands(ReplSession session, ReplEngine engine, OutputFormatter formatter)
    {
        this.session = session;
        this.engine = engine;
        this.formatter = formatter;

        registerCommands();
    }

    /**
     * Registers all built-in commands.
     */
    private void registerCommands()
    {
        commands.put("help", this::handleHelp);
        commands.put("h", this::handleHelp);
        commands.put("?", this::handleHelp);

        commands.put("info", this::handleInfo);
        commands.put("i", this::handleInfo);

        commands.put("type", this::handleType);
        commands.put("t", this::handleType);

        commands.put("reload", this::handleReload);
        commands.put("r", this::handleReload);

        commands.put("test", this::handleTest);

        commands.put("clear", this::handleClear);

        commands.put("option", this::handleOption);
        commands.put("opt", this::handleOption);
        commands.put("options", args -> handleOption(""));

        commands.put("quit", this::handleQuit);
        commands.put("exit", this::handleQuit);
        commands.put("q", this::handleQuit);
    }

    /**
     * Checks if the input is a REPL command.
     */
    public boolean isCommand(String input)
    {
        return input != null && input.startsWith(":");
    }

    /**
     * Executes a REPL command.
     *
     * @param input the command input (including the : prefix)
     * @return true if the command was handled, false otherwise
     */
    public CommandResult execute(String input)
    {
        if (!isCommand(input))
        {
            return new CommandResult(false, "Not a command");
        }

        // Parse the command and arguments
        String commandLine = input.substring(1).trim();
        if (commandLine.isEmpty())
        {
            return new CommandResult(false, "Empty command");
        }

        String[] parts = commandLine.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1].trim() : "";

        CommandHandler handler = commands.get(command);
        if (handler == null)
        {
            formatter.formatError("Unknown command: " + command + ". Type :help for available commands.");
            return new CommandResult(false, "Unknown command");
        }

        return handler.handle(args);
    }

    /**
     * Gets the set of registered command names.
     */
    public Set<String> getCommandNames()
    {
        return commands.keySet();
    }

    // Command handlers

    private CommandResult handleHelp(String args)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Available commands:\n");
        sb.append("  :help, :h, :?      Display this help message\n");
        sb.append("  :info, :i          Display runtime information\n");
        sb.append("  :type <expr>, :t   Show the type of an expression without evaluating\n");
        sb.append("  :reload, :r        Recompile Pure sources\n");
        sb.append("  :test <path>       Run tests at the specified path\n");
        sb.append("  :option <name> <true|false>, :opt\n");
        sb.append("                     Set a runtime option\n");
        sb.append("  :option <name>, :opt\n");
        sb.append("                     Get a runtime option value\n");
        sb.append("  :options           List all runtime options\n");
        sb.append("  :clear             Clear session state (future enhancement)\n");
        sb.append("  :quit, :exit, :q   Exit the REPL\n");
        sb.append("\n");
        sb.append("Expression evaluation:\n");
        sb.append("  Enter any Pure expression to evaluate it.\n");
        sb.append("  Examples:\n");
        sb.append("    1 + 2\n");
        sb.append("    'hello'->toUpper()\n");
        sb.append("    [1, 2, 3]->map(x | $x * 2)\n");

        formatter.formatMessage(sb.toString());
        return new CommandResult(true, null);
    }

    private CommandResult handleInfo(String args)
    {
        formatter.formatRuntimeInfo(session);
        return new CommandResult(true, null);
    }

    private CommandResult handleType(String args)
    {
        if (args.isEmpty())
        {
            formatter.formatError("Usage: :type <expression>");
            return new CommandResult(false, "Missing expression");
        }

        try
        {
            String type = engine.getExpressionType(args);
            formatter.formatType(args, type);
            return new CommandResult(true, null);
        }
        catch (ReplException e)
        {
            formatter.formatError(e.getMessage());
            return new CommandResult(false, e.getMessage());
        }
    }

    private CommandResult handleReload(String args)
    {
        try
        {
            formatter.formatMessage("Reloading Pure sources...");
            session.reload();
            formatter.formatMessage("Reload complete.");
            return new CommandResult(true, null);
        }
        catch (Exception e)
        {
            formatter.formatError("Reload failed: " + e.getMessage());
            return new CommandResult(false, e.getMessage());
        }
    }

    private CommandResult handleTest(String args)
    {
        String path = args.isEmpty() ? "::" : args;

        // Parse optional PCT adapter
        String pctAdapter = null;
        String[] parts = path.split("\\s+--pct-adapter\\s+");
        if (parts.length > 1)
        {
            path = parts[0].trim();
            pctAdapter = parts[1].trim();
        }

        formatter.formatMessage("Running tests at: " + path + (pctAdapter != null ? " (PCT adapter: " + pctAdapter + ")" : ""));

        TestResults results = engine.runTests(path, pctAdapter);
        formatter.formatTestResults(results);

        return new CommandResult(results.allPassed(), results.allPassed() ? null : "Some tests failed");
    }

    private CommandResult handleOption(String args)
    {
        if (args.isEmpty())
        {
            // List all options
            java.util.Map<String, Boolean> options = session.getAllPureRuntimeOptions();
            if (options.isEmpty())
            {
                formatter.formatMessage("No runtime options set.");
            }
            else
            {
                StringBuilder sb = new StringBuilder();
                sb.append("Runtime options:\n");
                options.entrySet().stream()
                        .sorted(java.util.Map.Entry.comparingByKey())
                        .forEach(e -> sb.append("  ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n"));
                formatter.formatMessage(sb.toString());
            }
            return new CommandResult(true, null);
        }

        String[] parts = args.split("\\s+", 2);
        if (parts.length == 1)
        {
            // Get a single option
            String name = parts[0];
            boolean value = session.getPureRuntimeOption(name);
            formatter.formatMessage(name + " = " + value);
            return new CommandResult(true, null);
        }
        else
        {
            // Set an option
            String name = parts[0];
            String valStr = parts[1].trim().toLowerCase();
            if (!"true".equals(valStr) && !"false".equals(valStr))
            {
                formatter.formatError("Usage: :option <name> <true|false>");
                return new CommandResult(false, "Invalid boolean value");
            }
            boolean value = Boolean.parseBoolean(valStr);
            session.setPureRuntimeOption(name, value);
            formatter.formatMessage(name + " = " + value);
            return new CommandResult(true, null);
        }
    }

    private CommandResult handleClear(String args)
    {
        // Future enhancement: clear session bindings
        formatter.formatMessage("Session cleared.");
        return new CommandResult(true, null);
    }

    private CommandResult handleQuit(String args)
    {
        formatter.formatMessage("Goodbye.");
        return new CommandResult(true, null, true);
    }

    /**
     * Functional interface for command handlers.
     */
    @FunctionalInterface
    private interface CommandHandler
    {
        CommandResult handle(String args);
    }

    /**
     * Result of executing a command.
     */
    public static class CommandResult
    {
        private final boolean success;
        private final String message;
        private final boolean shouldExit;

        public CommandResult(boolean success, String message)
        {
            this(success, message, false);
        }

        public CommandResult(boolean success, String message, boolean shouldExit)
        {
            this.success = success;
            this.message = message;
            this.shouldExit = shouldExit;
        }

        public boolean isSuccess()
        {
            return success;
        }

        public String getMessage()
        {
            return message;
        }

        public boolean shouldExit()
        {
            return shouldExit;
        }
    }
}
