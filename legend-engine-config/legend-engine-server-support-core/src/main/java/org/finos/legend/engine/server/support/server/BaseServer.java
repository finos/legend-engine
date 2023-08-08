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

package org.finos.legend.engine.server.support.server;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.finos.legend.server.shared.bundles.ChainFixingFilterHandler;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.finos.legend.engine.server.support.server.config.BaseServerConfiguration;
import org.finos.legend.engine.server.support.server.tools.StringTools;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.ext.ParamConverter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.EnumSet;
import java.util.concurrent.Callable;

public abstract class BaseServer<C extends BaseServerConfiguration> extends Application<C>
{
    protected ServerInfo serverInfo;

    @Override
    public void initialize(Bootstrap<C> bootstrap)
    {
        // initialize server info
        String initTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        String hostName = tryGetValue(BaseServer::getLocalHostName);
        ServerPlatformInfo platformInfo = tryGetValue(this::newServerPlatformInfo);
        this.serverInfo = new ServerInfo(hostName, initTime, platformInfo);

        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(true)));

        // TODO
        bootstrap.addBundle(new DropwizardConfigurationSwaggerBundle());
    }

    @Override
    public void run(C configuration, Environment environment)
    {
        ChainFixingFilterHandler.apply(environment.getApplicationContext(), configuration.getFilterPriorities());
        SessionHandler sessionHandler = new SessionHandler();
        if (configuration.getSessionCookie() != null)
        {
            sessionHandler.setSessionCookie(configuration.getSessionCookie());
        }
        environment.servlets().setSessionHandler(sessionHandler);

        // Enable CORS
        FilterRegistration.Dynamic corsFilter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS");
        corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_TIMING_ORIGINS_PARAM, "*");

        if (configuration.getCORSConfiguration() != null && configuration.getCORSConfiguration().getAllowedHeaders() != null)
        {
            corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, LazyIterate.adapt(configuration.getCORSConfiguration().getAllowedHeaders()).makeString(","));
        }
        else
        {
            // NOTE: this set of headers are kept as default for backward compatibility, the headers starting with prefix `x-` are meant for Zipkin
            // client using SDLC server. We should consider using the CORS configuration and remove those from this default list.
            corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Access-Control-Allow-Credentials,x-b3-parentspanid,x-b3-sampled,x-b3-spanid,x-b3-traceid");
        }
        corsFilter.setInitParameter(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, "false");
        corsFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "*");

        environment.jersey().register(MultiPartFeature.class);
        environment.healthChecks().register("server", new MinimalServerHealthCheck());

        // Temporal configuration
        environment.getObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Error handling
        // TODO - fix
        //boolean includeStackTraces = Optional.ofNullable(configuration.getErrorHandlingConfiguration()).map(ErrorHandlingConfiguration::getIncludeStackTrace).orElse(false);
        //environment.jersey().register(new JsonProcessingExceptionMapper(includeStackTraces));
        //environment.jersey().register(new LegendSDLCServerExceptionMapper(includeStackTraces));
        //environment.jersey().register(new CatchAllExceptionMapper(includeStackTraces));

        configureServer(configuration, environment);
    }

    void configureServer(C configuration, Environment environment)
    {
        this.configureServerCore(configuration, environment);
        this.configureServerExtension(configuration, environment);
    }

    protected void configureServerCore(C configuration, Environment environment)
    {
    }

    protected void configureServerExtension(C configuration, Environment environment)
    {
    }

    public ServerInfo getServerInfo()
    {
        return this.serverInfo;
    }

    protected abstract ServerPlatformInfo newServerPlatformInfo() throws Exception;

    protected static <T> T tryGetValue(Callable<T> callable)
    {
        return tryGetValue(callable, null);
    }

    protected static <T> T tryGetValue(Callable<T> callable, T defaultValue)
    {
        try
        {
            return callable.call();
        }
        catch (Exception e)
        {
            LoggerFactory.getLogger(BaseServer.class).warn("Error getting info property", e);
            return defaultValue;
        }
    }

    private static String getLocalHostName() throws UnknownHostException
    {
        return InetAddress.getLocalHost().getHostName();
    }

    public static final class ServerInfo
    {
        private final String hostName;
        private final String initTime;
        private final ServerPlatformInfo serverPlatformInfo;

        private ServerInfo(String hostName, String initTime, ServerPlatformInfo serverPlatformInfo)
        {
            this.hostName = hostName;
            this.initTime = initTime;
            this.serverPlatformInfo = (serverPlatformInfo == null) ? new ServerPlatformInfo(null, null, null) : serverPlatformInfo;
        }

        public String getHostName()
        {
            return this.hostName;
        }

        public String getInitTime()
        {
            return this.initTime;
        }

        public ServerPlatformInfo getPlatform()
        {
            return this.serverPlatformInfo;
        }
    }

    public static final class ServerPlatformInfo
    {
        private final String version;
        private final String buildTime;
        private final String buildRevision;

        public ServerPlatformInfo(String version, String buildTime, String buildRevision)
        {
            this.version = version;
            this.buildTime = buildTime;
            this.buildRevision = buildRevision;
        }

        public String getVersion()
        {
            return this.version;
        }

        public String getBuildTime()
        {
            return this.buildTime;
        }

        public String getBuildRevision()
        {
            return this.buildRevision;
        }
    }

    private static class DropwizardConfigurationSwaggerBundle extends SwaggerBundle<BaseServerConfiguration>
    {
        @Override
        protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(BaseServerConfiguration configuration)
        {
            return configuration.getSwaggerBundleConfiguration();
        }
    }

    private static class MinimalServerHealthCheck extends HealthCheck
    {
        @Override
        protected HealthCheck.Result check()
        {
            // if this is able to return a value, the server is healthy at a minimal level
            return Result.healthy();
        }
    }

    private abstract static class BaseConverter<T> implements ParamConverter<T>
    {
        private final Logger logger = LoggerFactory.getLogger(getClass());

        @Override
        public T fromString(String value)
        {
            if (value == null)
            {
                throw new IllegalArgumentException("Could not convert null");
            }

            try
            {
                return parseNonNull(value);
            }
            catch (Exception e)
            {
                StringBuilder builder = new StringBuilder("Could not convert \"").append(value).append('"');
                StringTools.appendThrowableMessageIfPresent(builder, e);
                String message = builder.toString();
                this.logger.debug(message, e);
                // We throw a ProcessingException rather than an IllegalArgumentException, as jersey's handling for
                // IllegalArgumentException means the default value is sent rather than an error being propagated
                throw new ProcessingException(message, e);
            }
        }

        protected abstract T parseNonNull(String value) throws Exception;
    }

    private static class InstantConverter extends BaseConverter<Instant>
    {
        @Override
        public String toString(Instant value)
        {
            return value.toString();
        }

        @Override
        protected Instant parseNonNull(String value)
        {
            return Instant.parse(value);
        }
    }

    private static class DateConverter extends BaseConverter<Date>
    {
        @Override
        public String toString(Date value)
        {
            return value.toInstant().toString();
        }

        @Override
        protected Date parseNonNull(String value)
        {
            return Date.from(Instant.parse(value));
        }
    }
}
