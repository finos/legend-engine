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

import org.finos.legend.pure.m4.coreinstance.SourceInformation;

/**
 * Represents the result of evaluating a Pure expression.
 */
public class EvaluationResult
{
    public enum Status
    {
        SUCCESS,
        ERROR
    }

    private final Status status;
    private final String expression;
    private final String type;
    private final String result;
    private final String consoleOutput;
    private final long parseMs;
    private final long compileMs;
    private final long executeMs;

    // Error information
    private final String errorMessage;
    private final String errorType;
    private final String source;
    private final Integer line;
    private final Integer column;
    private final String stackTrace;

    private EvaluationResult(Builder builder)
    {
        this.status = builder.status;
        this.expression = builder.expression;
        this.type = builder.type;
        this.result = builder.result;
        this.consoleOutput = builder.consoleOutput;
        this.parseMs = builder.parseMs;
        this.compileMs = builder.compileMs;
        this.executeMs = builder.executeMs;
        this.errorMessage = builder.errorMessage;
        this.errorType = builder.errorType;
        this.source = builder.source;
        this.line = builder.line;
        this.column = builder.column;
        this.stackTrace = builder.stackTrace;
    }

    public Status getStatus()
    {
        return status;
    }

    public boolean isSuccess()
    {
        return status == Status.SUCCESS;
    }

    public boolean isError()
    {
        return status == Status.ERROR;
    }

    public String getExpression()
    {
        return expression;
    }

    public String getType()
    {
        return type;
    }

    public String getResult()
    {
        return result;
    }

    public String getConsoleOutput()
    {
        return consoleOutput;
    }

    public long getParseMs()
    {
        return parseMs;
    }

    public long getCompileMs()
    {
        return compileMs;
    }

    public long getExecuteMs()
    {
        return executeMs;
    }

    public long getTotalMs()
    {
        return parseMs + compileMs + executeMs;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public String getErrorType()
    {
        return errorType;
    }

    public String getSource()
    {
        return source;
    }

    public Integer getLine()
    {
        return line;
    }

    public Integer getColumn()
    {
        return column;
    }

    public String getStackTrace()
    {
        return stackTrace;
    }

    /**
     * Returns a formatted string representation for display.
     */
    public String toDisplayString()
    {
        if (isSuccess())
        {
            StringBuilder sb = new StringBuilder();
            if (consoleOutput != null && !consoleOutput.isEmpty())
            {
                sb.append(consoleOutput);
                if (!consoleOutput.endsWith("\n"))
                {
                    sb.append("\n");
                }
            }
            sb.append(result);
            return sb.toString();
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Error: ").append(errorMessage);
            if (source != null)
            {
                sb.append("\n  at ").append(source);
                if (line != null)
                {
                    sb.append(":").append(line);
                    if (column != null)
                    {
                        sb.append(":").append(column);
                    }
                }
            }
            return sb.toString();
        }
    }

    /**
     * Builder for EvaluationResult.
     */
    public static class Builder
    {
        private Status status;
        private String expression;
        private String type;
        private String result;
        private String consoleOutput;
        private long parseMs;
        private long compileMs;
        private long executeMs;
        private String errorMessage;
        private String errorType;
        private String source;
        private Integer line;
        private Integer column;
        private String stackTrace;

        public Builder expression(String expression)
        {
            this.expression = expression;
            return this;
        }

        public Builder type(String type)
        {
            this.type = type;
            return this;
        }

        public Builder result(String result)
        {
            this.result = result;
            return this;
        }

        public Builder consoleOutput(String consoleOutput)
        {
            this.consoleOutput = consoleOutput;
            return this;
        }

        public Builder parseMs(long parseMs)
        {
            this.parseMs = parseMs;
            return this;
        }

        public Builder compileMs(long compileMs)
        {
            this.compileMs = compileMs;
            return this;
        }

        public Builder executeMs(long executeMs)
        {
            this.executeMs = executeMs;
            return this;
        }

        public Builder success()
        {
            this.status = Status.SUCCESS;
            return this;
        }

        public Builder error()
        {
            this.status = Status.ERROR;
            return this;
        }

        public Builder errorMessage(String errorMessage)
        {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder errorType(String errorType)
        {
            this.errorType = errorType;
            return this;
        }

        public Builder source(String source)
        {
            this.source = source;
            return this;
        }

        public Builder line(Integer line)
        {
            this.line = line;
            return this;
        }

        public Builder column(Integer column)
        {
            this.column = column;
            return this;
        }

        public Builder stackTrace(String stackTrace)
        {
            this.stackTrace = stackTrace;
            return this;
        }

        public Builder sourceInfo(SourceInformation sourceInfo)
        {
            if (sourceInfo != null)
            {
                this.source = sourceInfo.getSourceId();
                this.line = sourceInfo.getLine();
                this.column = sourceInfo.getColumn();
            }
            return this;
        }

        public EvaluationResult build()
        {
            return new EvaluationResult(this);
        }
    }
}
