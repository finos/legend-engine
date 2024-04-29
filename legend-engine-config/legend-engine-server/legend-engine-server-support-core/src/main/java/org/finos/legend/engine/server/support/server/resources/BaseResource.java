// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.server.support.server.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.finos.legend.engine.server.support.server.exception.ServerException;
import org.finos.legend.engine.server.support.server.monitoring.ServerMetricsHandler;
import org.finos.legend.engine.server.support.server.tools.StringTools;

import javax.ws.rs.core.Response;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class BaseResource
{
    protected <T> T execute(String descriptionForLogging, String metricName, Supplier<T> supplier)
    {
        Logger logger = getLogger();
        ServerMetricsHandler.operationStart();
        boolean isInfoLogging = logger.isInfoEnabled();
        String sanitizedDescription = isInfoLogging ? StringTools.sanitizeForLogging(descriptionForLogging, "_", false) : null;
        long startTime = System.nanoTime();
        if (isInfoLogging)
        {
            logger.info("Starting {}", sanitizedDescription);
        }
        try
        {
            T result = supplier.get();
            long endTime = System.nanoTime();
            ServerMetricsHandler.operationComplete(startTime, endTime, metricName);
            if (isInfoLogging)
            {
                long duration = endTime - startTime;
                StringBuilder builder = new StringBuilder(sanitizedDescription.length() + 32).append("Finished ").append(sanitizedDescription).append(" (");
                StringTools.formatDurationInNanos(builder, duration);
                builder.append("s)");
                logger.info(builder.toString());
            }
            return result;
        }
        catch (ServerException e)
        {
            long endTime = System.nanoTime();
            Response.Status status = e.getStatus();
            if ((status != null) && (status.getFamily() == Response.Status.Family.REDIRECTION))
            {
                ServerMetricsHandler.operationRedirect(startTime, endTime, metricName);
                if (isInfoLogging)
                {
                    long duration = endTime - startTime;
                    String redirectLocation = String.valueOf(e.getMessage());
                    StringBuilder builder = new StringBuilder(sanitizedDescription.length() + redirectLocation.length() + 39).append("Redirected ").append(sanitizedDescription).append(" to: ").append(redirectLocation).append(" (");
                    StringTools.formatDurationInNanos(builder, duration);
                    builder.append("s)");
                    logger.info(builder.toString());
                }
            }
            else
            {
                ServerMetricsHandler.operationError(startTime, endTime, metricName);
                if (logger.isErrorEnabled())
                {
                    long duration = endTime - startTime;
                    if (sanitizedDescription == null)
                    {
                        sanitizedDescription = StringTools.sanitizeForLogging(descriptionForLogging, "_", false);
                    }
                    logger.error(buildLoggingErrorMessage(e, sanitizedDescription, duration), e);
                }
            }
            throw e;
        }
        catch (Throwable t)
        {
            long endTime = System.nanoTime();
            ServerMetricsHandler.operationError(startTime, endTime, metricName);
            if (logger.isErrorEnabled())
            {
                long duration = endTime - startTime;
                if (sanitizedDescription == null)
                {
                    sanitizedDescription = StringTools.sanitizeForLogging(descriptionForLogging, "_", false);
                }
                logger.error(buildLoggingErrorMessage(t, sanitizedDescription, duration), t);
            }
            throw t;
        }
    }

    protected <T, R> R execute(String descriptionForLogging, String metricName, Function<? super T, R> function, T arg)
    {
        return execute(descriptionForLogging, metricName, () -> function.apply(arg));
    }

    protected <T, U, R> R execute(String descriptionForLogging, String metricName, BiFunction<? super T, ? super U, R> function, T arg1, U arg2)
    {
        return execute(descriptionForLogging, metricName, () -> function.apply(arg1, arg2));
    }

    protected void execute(String descriptionForLogging, String metricName, Runnable runnable)
    {
        execute(descriptionForLogging, metricName, () ->
        {
            runnable.run();
            return null;
        });
    }

    protected <T> void execute(String descriptionForLogging, String metricName, Consumer<? super T> consumer, T arg)
    {
        execute(descriptionForLogging, metricName, () ->
        {
            consumer.accept(arg);
            return null;
        });
    }

    protected <T, U> void execute(String descriptionForLogging, String metricName, BiConsumer<? super T, ? super U> consumer, T arg1, U arg2)
    {
        execute(descriptionForLogging, metricName, () ->
        {
            consumer.accept(arg1, arg2);
            return null;
        });
    }

    protected <T> T executeWithLogging(String description, Supplier<T> supplier)
    {
        return execute(description, null, supplier);
    }

    protected <T, R> R executeWithLogging(String description, Function<? super T, R> function, T arg)
    {
        return executeWithLogging(description, () -> function.apply(arg));
    }

    protected <T, U, R> R executeWithLogging(String description, BiFunction<? super T, ? super U, R> function, T arg1, U arg2)
    {
        return executeWithLogging(description, () -> function.apply(arg1, arg2));
    }

    protected void executeWithLogging(String description, Runnable runnable)
    {
        executeWithLogging(description, () ->
        {
            runnable.run();
            return null;
        });
    }

    protected <T> void executeWithLogging(String description, Consumer<? super T> consumer, T arg)
    {
        executeWithLogging(description, () ->
        {
            consumer.accept(arg);
            return null;
        });
    }

    protected <T, U> void executeWithLogging(String description, BiConsumer<? super T, ? super U> consumer, T arg1, U arg2)
    {
        executeWithLogging(description, () ->
        {
            consumer.accept(arg1, arg2);
            return null;
        });
    }

    protected Logger getLogger()
    {
        return LoggerFactory.getLogger(getClass());
    }

    private String buildLoggingErrorMessage(Throwable t, String description, long durationNanos)
    {
        StringBuilder builder = new StringBuilder(description.length() + 29).append("Error ").append(description).append(" (");
        StringTools.formatDurationInNanos(builder, durationNanos);
        builder.append("s)");
        String message = t.getMessage();
        if (message != null)
        {
            builder.append(": ").append(StringTools.replaceVerticalWhitespace(message, " ", true));
        }
        return builder.toString();
    }
}
