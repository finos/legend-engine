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
import org.finos.legend.engine.pure.repl.core.ReplConfiguration;
import org.finos.legend.engine.pure.repl.core.ReplEngine;
import org.finos.legend.engine.pure.repl.core.ReplException;
import org.finos.legend.engine.pure.repl.core.ReplSession;
import org.finos.legend.engine.pure.repl.core.TestResults;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Main entry point for the Pure REPL CLI.
 *
 * Usage examples:
 *   pure-repl                           # Interactive mode
 *   pure-repl -e "1 + 2"               # Evaluate expression
 *   pure-repl -e "1 + 2" --json        # JSON output
 *   pure-repl --test meta::pure::...   # Run tests
 *   pure-repl -f script.pure           # Execute file
 *   pure-repl --run-go                 # Run go() function
 */
@Command(
        name = "pure-repl",
        description = "Interactive REPL for Pure language expressions",
        version = "Pure REPL 1.0.0",
        mixinStandardHelpOptions = true
)
public class PureReplMain implements Callable<Integer>
{
    // Exit codes per PRD
    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_RUNTIME_ERROR = 1;
    public static final int EXIT_COMPILE_ERROR = 2;
    public static final int EXIT_CONFIG_ERROR = 3;
    public static final int EXIT_TEST_FAILURE = 4;

    @Option(names = {"-e", "--eval"}, description = "Evaluate a Pure expression")
    private String expression;

    @Option(names = {"-f", "--file"}, description = "Execute a Pure file")
    private File file;

    @Option(names = {"--stdin"}, description = "Read Pure code from stdin")
    private boolean stdin;

    @Option(names = {"--test"}, description = "Run tests at the specified path")
    private String testPath;

    @Option(names = {"--pct-adapter"}, description = "PCT adapter for test execution")
    private String pctAdapter;

    @Option(names = {"--filter"}, description = "Regex filter for test selection")
    private String testFilter;

    @Option(names = {"--run-go"}, description = "Execute the go():Any[*] function")
    private boolean runGo;

    @Option(names = {"--json"}, description = "Output in JSON format")
    private boolean jsonOutput;

    @Option(names = {"--config"}, description = "Configuration file path")
    private File configFile;

    @Option(names = {"--debug"}, description = "Enable debug mode")
    private boolean debug;

    @Option(names = {"-q", "--quiet"}, description = "Suppress non-essential output")
    private boolean quiet;

    @Option(names = {"--timeout"}, description = "Expression timeout in milliseconds")
    private Long timeout;

    @Option(names = {"--load"}, description = "Load additional Pure file")
    private File loadFile;

    @Option(names = {"--load-dir"}, description = "Load Pure files from directory")
    private File loadDir;

    @Option(names = {"--source-root"}, description = "Root directory of the legend-engine checkout (enables filesystem repository loading)")
    private File sourceRoot;

    @Option(names = {"--repositories"}, description = "Comma-separated list of repository names to load (subset filtering)")
    private String repositories;

    @Option(names = {"--set-option"}, description = "Set a runtime option (repeatable, format: name=true|false)", split = ",")
    private List<String> setOptions;

    public static void main(String[] args)
    {
        int exitCode = new CommandLine(new PureReplMain()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call()
    {
        try
        {
            // Load configuration
            ReplConfiguration config = loadConfiguration();
            applyConfigOverrides(config);

            // Create formatter
            OutputFormatter.OutputFormat format = jsonOutput ?
                    OutputFormatter.OutputFormat.JSON : config.getOutputFormat();
            OutputFormatter formatter = new OutputFormatter(format);

            // Determine source root and required repositories
            String srcRoot = sourceRoot != null ? sourceRoot.getAbsolutePath() : config.getSourceRoot();
            List<String> requiredRepos = null;
            if (repositories != null)
            {
                requiredRepos = Arrays.asList(repositories.split(","));
            }
            else if (config.getRequiredRepositories() != null)
            {
                requiredRepos = config.getRequiredRepositories();
            }

            // Create session and engine
            ReplSession session = new ReplSession(srcRoot, requiredRepos);

            // Apply --set-option flags
            if (setOptions != null)
            {
                for (String opt : setOptions)
                {
                    int eqIdx = opt.indexOf('=');
                    if (eqIdx <= 0 || eqIdx == opt.length() - 1)
                    {
                        System.err.println("Invalid --set-option format: " + opt + " (expected name=true|false)");
                        return EXIT_CONFIG_ERROR;
                    }
                    String name = opt.substring(0, eqIdx);
                    String val = opt.substring(eqIdx + 1).toLowerCase();
                    if (!"true".equals(val) && !"false".equals(val))
                    {
                        System.err.println("Invalid --set-option value: " + opt + " (expected true or false)");
                        return EXIT_CONFIG_ERROR;
                    }
                    session.setPureRuntimeOption(name, Boolean.parseBoolean(val));
                }
            }

            ReplEngine engine = new ReplEngine(session);

            if (timeout != null)
            {
                engine.setTimeout(timeout);
            }
            else
            {
                engine.setTimeout(config.getTimeoutMs());
            }

            // Initialize
            if (!quiet)
            {
                System.err.println("Initializing Pure runtime...");
            }
            session.initialize();
            if (!quiet)
            {
                System.err.println("Ready.");
            }

            // Determine mode of operation
            if (expression != null)
            {
                return executeExpression(engine, formatter, expression);
            }
            else if (file != null)
            {
                return executeFile(engine, formatter, file);
            }
            else if (stdin)
            {
                return executeStdin(engine, formatter);
            }
            else if (testPath != null)
            {
                return executeTests(engine, formatter);
            }
            else if (runGo)
            {
                return executeGo(engine, formatter);
            }
            else
            {
                // Interactive mode
                InteractiveRepl repl = new InteractiveRepl(session, engine, config, formatter);
                return repl.run();
            }
        }
        catch (Exception e)
        {
            System.err.println("Error: " + e.getMessage());
            if (debug)
            {
                e.printStackTrace();
            }
            return EXIT_CONFIG_ERROR;
        }
    }

    private ReplConfiguration loadConfiguration()
    {
        if (configFile != null && configFile.exists())
        {
            return ReplConfiguration.load(configFile.getAbsolutePath());
        }
        return ReplConfiguration.loadDefault();
    }

    private void applyConfigOverrides(ReplConfiguration config)
    {
        if (debug)
        {
            config.setDebug(true);
        }
        if (quiet)
        {
            config.setQuiet(true);
        }
        if (jsonOutput)
        {
            config.setOutputFormat("json");
        }
        if (timeout != null)
        {
            config.setTimeoutMs(timeout);
        }
    }

    private int executeExpression(ReplEngine engine, OutputFormatter formatter, String expr)
    {
        try
        {
            EvaluationResult result = engine.evaluate(expr);
            formatter.formatEvaluationResult(result);

            if (result.isSuccess())
            {
                return EXIT_SUCCESS;
            }
            else
            {
                return isCompileError(result) ? EXIT_COMPILE_ERROR : EXIT_RUNTIME_ERROR;
            }
        }
        catch (ReplException e)
        {
            formatter.formatError(e.getMessage());
            return EXIT_RUNTIME_ERROR;
        }
    }

    private int executeFile(ReplEngine engine, OutputFormatter formatter, File pureFile)
    {
        try
        {
            String content = readFile(pureFile);
            return executeExpression(engine, formatter, content);
        }
        catch (IOException e)
        {
            formatter.formatError("Failed to read file: " + e.getMessage());
            return EXIT_CONFIG_ERROR;
        }
    }

    private int executeStdin(ReplEngine engine, OutputFormatter formatter)
    {
        try
        {
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = reader.readLine()) != null)
            {
                content.append(line).append("\n");
            }
            return executeExpression(engine, formatter, content.toString());
        }
        catch (IOException e)
        {
            formatter.formatError("Failed to read stdin: " + e.getMessage());
            return EXIT_CONFIG_ERROR;
        }
    }

    private int executeTests(ReplEngine engine, OutputFormatter formatter)
    {
        TestResults results = engine.runTests(testPath, pctAdapter, testFilter);
        formatter.formatTestResults(results);

        if (results.getStatus() == TestResults.Status.ERROR)
        {
            return EXIT_RUNTIME_ERROR;
        }
        return results.allPassed() ? EXIT_SUCCESS : EXIT_TEST_FAILURE;
    }

    private int executeGo(ReplEngine engine, OutputFormatter formatter)
    {
        try
        {
            EvaluationResult result = engine.executeGo();
            formatter.formatEvaluationResult(result);

            if (result.isSuccess())
            {
                return EXIT_SUCCESS;
            }
            else
            {
                return isCompileError(result) ? EXIT_COMPILE_ERROR : EXIT_RUNTIME_ERROR;
            }
        }
        catch (ReplException e)
        {
            formatter.formatError(e.getMessage());
            return EXIT_RUNTIME_ERROR;
        }
    }

    private boolean isCompileError(EvaluationResult result)
    {
        String errorType = result.getErrorType();
        return errorType != null &&
                (errorType.contains("Compilation") || errorType.contains("Parser"));
    }

    private String readFile(File file) throws IOException
    {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
