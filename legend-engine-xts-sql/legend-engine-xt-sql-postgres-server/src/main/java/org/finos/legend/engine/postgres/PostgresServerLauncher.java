// Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import org.apache.commons.lang3.StringUtils;
import org.finos.legend.engine.postgres.auth.AuthenticationMethod;
import org.finos.legend.engine.postgres.config.OpenTelemetryConfig;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class PostgresServerLauncher
{
    private String configPath;

    public PostgresServerLauncher(String configPath)
    {
        this.configPath = configPath;
    }

    public void launch() throws Exception
    {
        //TODO ADD CLI

        ObjectMapper objectMapper = new ObjectMapper();
        ServerConfig serverConfig = objectMapper.readValue(new File(configPath), ServerConfig.class);
        /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
          System property must be set before any logger instance is created.
          Do NOT create static logger in this class.
        */
        if (serverConfig.getLogConfigFile() != null)
        {
            System.setProperty("logback.configurationFile", serverConfig.getLogConfigFile());
        }
        if (serverConfig.getGss() != null)
        {
            System.setProperty("java.security.krb5.conf", serverConfig.getGss().getKerberosConfigFile());
        }

        setupOpenTelemetry(serverConfig.getOtelConfig());

        //Log config has been initiated. We can create a logger now.
        Logger logger = LoggerFactory.getLogger(PostgresServerLauncher.class);

        // install jul to slf4j bridge
        SLF4JBridgeHandler.install();

        SessionsFactory sessionFactory = serverConfig.buildSessionFactory();
        AuthenticationMethod authenticationMethod = serverConfig.buildAuthenticationMethod();

        logger.info("Starting server in port: " + serverConfig.getPort());

        new PostgresServer(serverConfig, sessionFactory, (user, connectionProperties) -> authenticationMethod).run();
    }

    private void setupOpenTelemetry(OpenTelemetryConfig otelConfig)
    {
        String zipkinEndpoint = otelConfig.getZipkinEndpoint();
        String serviceName = otelConfig.getServiceName();

        SdkTracerProviderBuilder tracerProviderBuilder = SdkTracerProvider.builder();
        OpenTelemetrySdkBuilder otelSdkBuilder = OpenTelemetrySdk.builder();
        ContextPropagators propagators = ContextPropagators.noop();

        if (StringUtils.isNotBlank(zipkinEndpoint))
        {
            ZipkinSpanExporter spanExporter = ZipkinSpanExporter.builder().setEndpoint(zipkinEndpoint).build();
            tracerProviderBuilder.addSpanProcessor(SimpleSpanProcessor.create(spanExporter));
            propagators = ContextPropagators.create(B3Propagator.injectingMultiHeaders());
        }

        if (StringUtils.isNotBlank(serviceName))
        {
            Resource serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName));
            tracerProviderBuilder.setResource(serviceNameResource);
        }

        SdkTracerProvider tracerProvider = tracerProviderBuilder.build();

        OpenTelemetrySdk openTelemetrySdk = otelSdkBuilder.setTracerProvider(tracerProvider)
                .setPropagators(propagators)
                .buildAndRegisterGlobal();

        Runtime.getRuntime().addShutdownHook(new Thread(openTelemetrySdk::close));
    }

    public static void main(String[] args) throws Exception
    {
        String configPath = args[0];
        new PostgresServerLauncher(configPath).launch();
    }
}
