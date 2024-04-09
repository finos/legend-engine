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
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.name.Names;
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
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.File;
import java.lang.annotation.Retention;
import org.apache.commons.lang3.StringUtils;
import org.finos.legend.engine.postgres.auth.AnonymousIdentityProvider;
import org.finos.legend.engine.postgres.auth.AuthenticationMethod;
import org.finos.legend.engine.postgres.auth.AuthenticationProvider;
import org.finos.legend.engine.postgres.auth.GSSAuthenticationMethod;
import org.finos.legend.engine.postgres.auth.IdentityProvider;
import org.finos.legend.engine.postgres.auth.IdentityType;
import org.finos.legend.engine.postgres.auth.KerberosIdentityProvider;
import org.finos.legend.engine.postgres.auth.NoPasswordAuthenticationMethod;
import org.finos.legend.engine.postgres.auth.UsernamePasswordAuthenticationMethod;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.finos.legend.engine.postgres.utils.ErrorMessageFormatter;
import org.finos.legend.engine.postgres.utils.ErrorMessageFormatterImpl;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class PostgresMainModule extends AbstractModule
{
    @BindingAnnotation
    @Retention(RUNTIME)
    private @interface PrivateBindingAnnotation
    {
    }

    private String configPath;

    public PostgresMainModule(String configPath)
    {
        this.configPath = configPath;
    }

    @Override
    protected void configure()
    {
        bind(String.class).annotatedWith(Names.named("configPath")).toInstance(configPath);
        bind(OpenTelemetrySdk.class).annotatedWith(PrivateBindingAnnotation.class).to(OpenTelemetrySdk.class).asEagerSingleton();
        bind(ErrorMessageFormatter.class).to(ErrorMessageFormatterImpl.class);
        // install jul to slf4j bridge
        SLF4JBridgeHandler.install();
    }


    @Provides
    @Singleton
    public ServerConfig provideSeverConfig(@Named("configPath") String configPath) throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        ServerConfig serverConfig = objectMapper.readValue(new File(configPath), ServerConfig.class);
        /* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        System property must be set before any logger instance is created.
        Do NOT create static logger in this class.*/

        if (serverConfig.getLogConfigFile() != null)
        {
            System.setProperty("logback.configurationFile", serverConfig.getLogConfigFile());
        }
        if (serverConfig.getGss() != null)
        {
            System.setProperty("java.security.krb5.conf", serverConfig.getGss().getKerberosConfigFile());
        }
        return serverConfig;
    }

    @Provides
    @Singleton
    public OpenTelemetrySdk provideOpenTelemetrySdk(ServerConfig serverConfig)
    {
        String zipkinEndpoint = serverConfig.getOtelConfig().getZipkinEndpoint();
        String serviceName = serverConfig.getOtelConfig().getServiceName();

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
        return openTelemetrySdk;
    }

    @Provides
    @Singleton
    public SessionsFactory buildSessionFactory(ServerConfig serverConfig)
    {
        return serverConfig.getHandler().buildSessionsFactory();
    }

    @Provides
    @Singleton
    public AuthenticationMethod buildAuthenticationMethod(ServerConfig serverConfig)
    {
        IdentityProvider identityProvider;
        if (serverConfig.getIdentityType() == IdentityType.KERBEROS)
        {
            identityProvider = new KerberosIdentityProvider();
        }
        else if (serverConfig.getIdentityType() == IdentityType.ANONYMOUS)
        {
            identityProvider = new AnonymousIdentityProvider();
        }
        else
        {
            throw new UnsupportedOperationException("Identity type not supported :" + serverConfig.getIdentityType());
        }

        switch (serverConfig.getAuthenticationMethod())
        {
            case PASSWORD:
                return new UsernamePasswordAuthenticationMethod(identityProvider);
            case NO_PASSWORD:
                return new NoPasswordAuthenticationMethod(identityProvider);
            case GSS:
                return new GSSAuthenticationMethod(identityProvider);
            default:
                throw new UnsupportedOperationException("Authentication Method not supported :" + serverConfig.getAuthenticationMethod());
        }
    }

    @Provides
    @Singleton
    public AuthenticationProvider provideAuthenticationProvider(AuthenticationMethod authenticationMethod)
    {
        return (user, connectionProperties) -> authenticationMethod;
    }
}

