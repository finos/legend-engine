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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the Pure REPL.
 * Supports loading from JSON file, environment variables, and programmatic overrides.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReplConfiguration
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplConfiguration.class);

    // Environment variable prefix
    private static final String ENV_PREFIX = "PURE_REPL_";

    // Default values
    private static final long DEFAULT_TIMEOUT_MS = 30000;
    private static final int DEFAULT_HISTORY_SIZE = 1000;
    private static final String DEFAULT_OUTPUT_FORMAT = "text";
    private static final String DEFAULT_HISTORY_FILE = "~/.pure_history";
    private static final String DEFAULT_CONFIG_FILE = "~/.pure-repl.json";

    @JsonProperty("timeout")
    private Long timeoutMs;

    @JsonProperty("historyFile")
    private String historyFile;

    @JsonProperty("historySize")
    private Integer historySize;

    @JsonProperty("outputFormat")
    private String outputFormat;

    @JsonProperty("debug")
    private Boolean debug;

    @JsonProperty("quiet")
    private Boolean quiet;

    @JsonProperty("repositories")
    private List<RepositoryConfig> repositories;

    @JsonProperty("sourceRoot")
    private String sourceRoot;

    @JsonProperty("requiredRepositories")
    private List<String> requiredRepositories;

    public ReplConfiguration()
    {
        // Initialize with defaults
        this.timeoutMs = DEFAULT_TIMEOUT_MS;
        this.historySize = DEFAULT_HISTORY_SIZE;
        this.outputFormat = DEFAULT_OUTPUT_FORMAT;
        this.historyFile = DEFAULT_HISTORY_FILE;
        this.debug = false;
        this.quiet = false;
        this.repositories = new ArrayList<>();
    }

    /**
     * Loads configuration from the default location (~/.pure-repl.json).
     */
    public static ReplConfiguration loadDefault()
    {
        return load(expandPath(DEFAULT_CONFIG_FILE));
    }

    /**
     * Loads configuration from the specified file.
     * If the file doesn't exist, returns default configuration.
     */
    public static ReplConfiguration load(String configPath)
    {
        String expandedPath = expandPath(configPath);
        File configFile = new File(expandedPath);

        ReplConfiguration config;

        if (configFile.exists() && configFile.isFile())
        {
            try
            {
                ObjectMapper mapper = new ObjectMapper();
                config = mapper.readValue(configFile, ReplConfiguration.class);
                LOGGER.debug("Loaded configuration from {}", expandedPath);
            }
            catch (IOException e)
            {
                LOGGER.warn("Failed to load configuration from {}: {}", expandedPath, e.getMessage());
                config = new ReplConfiguration();
            }
        }
        else
        {
            LOGGER.debug("Configuration file not found: {}", expandedPath);
            config = new ReplConfiguration();
        }

        // Apply environment variable overrides
        config.applyEnvironmentOverrides();

        return config;
    }

    /**
     * Applies environment variable overrides.
     */
    private void applyEnvironmentOverrides()
    {
        String envTimeout = System.getenv(ENV_PREFIX + "TIMEOUT");
        if (envTimeout != null)
        {
            try
            {
                this.timeoutMs = Long.parseLong(envTimeout);
            }
            catch (NumberFormatException e)
            {
                LOGGER.warn("Invalid PURE_REPL_TIMEOUT value: {}", envTimeout);
            }
        }

        String envHistory = System.getenv(ENV_PREFIX + "HISTORY");
        if (envHistory != null)
        {
            this.historyFile = envHistory;
        }

        String envOutputFormat = System.getenv(ENV_PREFIX + "OUTPUT_FORMAT");
        if (envOutputFormat != null)
        {
            this.outputFormat = envOutputFormat;
        }

        String envDebug = System.getenv(ENV_PREFIX + "DEBUG");
        if ("true".equalsIgnoreCase(envDebug))
        {
            this.debug = true;
        }

        String envConfig = System.getenv(ENV_PREFIX + "CONFIG");
        // Config is handled at load time, not here
    }

    // Getters and setters

    public long getTimeoutMs()
    {
        return timeoutMs != null ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }

    public void setTimeoutMs(long timeoutMs)
    {
        this.timeoutMs = timeoutMs;
    }

    public String getHistoryFile()
    {
        return expandPath(historyFile != null ? historyFile : DEFAULT_HISTORY_FILE);
    }

    public void setHistoryFile(String historyFile)
    {
        this.historyFile = historyFile;
    }

    public Path getHistoryFilePath()
    {
        return Paths.get(getHistoryFile());
    }

    public int getHistorySize()
    {
        return historySize != null ? historySize : DEFAULT_HISTORY_SIZE;
    }

    public void setHistorySize(int historySize)
    {
        this.historySize = historySize;
    }

    public OutputFormatter.OutputFormat getOutputFormat()
    {
        String format = outputFormat != null ? outputFormat : DEFAULT_OUTPUT_FORMAT;
        if ("json".equalsIgnoreCase(format))
        {
            return OutputFormatter.OutputFormat.JSON;
        }
        return OutputFormatter.OutputFormat.TEXT;
    }

    public void setOutputFormat(String outputFormat)
    {
        this.outputFormat = outputFormat;
    }

    public boolean isDebug()
    {
        return debug != null && debug;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    public boolean isQuiet()
    {
        return quiet != null && quiet;
    }

    public void setQuiet(boolean quiet)
    {
        this.quiet = quiet;
    }

    public List<RepositoryConfig> getRepositories()
    {
        return repositories != null ? repositories : new ArrayList<>();
    }

    public void setRepositories(List<RepositoryConfig> repositories)
    {
        this.repositories = repositories;
    }

    public String getSourceRoot()
    {
        return expandPath(sourceRoot);
    }

    public void setSourceRoot(String sourceRoot)
    {
        this.sourceRoot = sourceRoot;
    }

    public List<String> getRequiredRepositories()
    {
        return requiredRepositories;
    }

    public void setRequiredRepositories(List<String> requiredRepositories)
    {
        this.requiredRepositories = requiredRepositories;
    }

    /**
     * Expands ~ to user home directory.
     */
    private static String expandPath(String path)
    {
        if (path == null)
        {
            return null;
        }
        if (path.startsWith("~"))
        {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    /**
     * Configuration for additional repository paths.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepositoryConfig
    {
        @JsonProperty("name")
        private String name;

        @JsonProperty("path")
        private String path;

        @JsonProperty("definition")
        private String definition;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getPath()
        {
            return expandPath(path);
        }

        public void setPath(String path)
        {
            this.path = path;
        }

        public String getDefinition()
        {
            return expandPath(definition);
        }

        public void setDefinition(String definition)
        {
            this.definition = definition;
        }
    }
}
