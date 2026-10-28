/*
 * Licensed to Crate.io GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package org.finos.legend.engine.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.exporter.servlet.jakarta.PrometheusMetricsServlet;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.finos.legend.engine.postgres.protocol.wire.SessionStats;
import org.finos.legend.engine.postgres.protocol.wire.auth.method.AuthenticationMethod;
import org.finos.legend.engine.postgres.protocol.wire.auth.method.ConnectionProperties;
import org.finos.legend.engine.postgres.config.GSSConfig;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.finos.legend.engine.postgres.config.SSLConfig;
import org.finos.legend.engine.postgres.protocol.sql.SQLManager;
import org.finos.legend.engine.postgres.protocol.wire.serialization.Messages;
import org.finos.legend.engine.postgres.protocol.wire.PostgresWireProtocol;
import org.finos.legend.engine.postgres.utils.netty.Netty4OpenChannelsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public class PostgresServer
{
    private static final Date startTime = new Date();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresServer.class);
    private final int httpPort;
    private final int port;
    private final Function2<String, ConnectionProperties, AuthenticationMethod> authenticationProvider;
    private final GSSConfig gssConfig;
    private final SSLConfig sslConfig;
    private final SslContext wireSslContext;
    private final boolean requireSsl;
    private Channel channel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final Messages messages;
    private final SQLManager sqlManager;
    private final ExecutorService sessionExecutor = Executors.newCachedThreadPool(new ThreadFactory()
    {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r)
        {
            Thread t = new Thread(r, "postgres-session-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });
    private static final int MAX_HISTORY = 1000;
    private final Set<PostgresWireProtocol> liveConnections = ConcurrentHashMap.newKeySet();
    private final List<SessionStats> connectionsHistory = Collections.synchronizedList(new ArrayList<>());
    // Prometheus state ---------
    private PrometheusRegistry registry;
    private PrometheusRegistry jvmRegistry;
    private Gauge connectionCount;
    private PrometheusUserMetrics userMetrics;
    // Prometheus state ---------
    private Server httpServer;

    public PostgresServer(ServerConfig serverConfig, SQLManager sqlManager, Function2<String, ConnectionProperties, AuthenticationMethod> authenticationProvider, Messages messages)
    {
        this.port = serverConfig.getPort();
        this.httpPort = serverConfig.getHttpPort() == null ? 8080 : serverConfig.getHttpPort();
        this.authenticationProvider = authenticationProvider;
        this.gssConfig = serverConfig.getGss();
        this.sslConfig = serverConfig.getSsl();
        this.messages = messages;
        this.sqlManager = sqlManager;
        this.wireSslContext = this.sslConfig == null ? null : buildWireSslContext(this.sslConfig);
        this.requireSsl = this.sslConfig != null && this.sslConfig.isRequireSsl();
    }

    public void open(PostgresWireProtocol postgresWireProtocol)
    {
        this.connectionCount.inc();
        liveConnections.add(postgresWireProtocol);
    }

    public void close(PostgresWireProtocol postgresWireProtocol)
    {
        if (liveConnections.remove(postgresWireProtocol))
        {
            if (!"Unknown".equals(postgresWireProtocol.getSessionStats().name))
            {
                synchronized (connectionsHistory)
                {
                    if (connectionsHistory.size() >= MAX_HISTORY)
                    {
                        connectionsHistory.remove(0);
                    }
                    connectionsHistory.add(postgresWireProtocol.getSessionStats());
                }
            }
            this.connectionCount.dec();
        }
        else
        {
            LOGGER.warn("no live connection to remove on session close");
        }
    }

    public void run()
    {
        try
        {
            startHttp();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        Netty4OpenChannelsHandler openChannelsHandler = new Netty4OpenChannelsHandler(
                LoggerFactory.getLogger(Netty4OpenChannelsHandler.class));

        bossGroup = newEventLoopGroup();
        workerGroup = newEventLoopGroup();
        SocketAddress socketAddress = new InetSocketAddress(port);
        try
        {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(getChannelClass())
                    .localAddress(socketAddress)
                    .childHandler(new ChannelInitializer<SocketChannel>()
                    {
                        @Override
                        protected void initChannel(SocketChannel ch)
                        {
                            PostgresWireProtocol postgresWireProtocol = new PostgresWireProtocol(PostgresServer.this, sqlManager, authenticationProvider, gssConfig, () -> wireSslContext, requireSsl, messages);
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("open_channels", openChannelsHandler);
                            pipeline.addLast("frame-decoder", postgresWireProtocol.decoder);
                            pipeline.addLast("handler", postgresWireProtocol.handler);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            //Bind and start accept incoming connections
            this.channel = bootstrap.bind().syncUninterruptibly().channel();
        }
        catch (RuntimeException e)
        {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private static Class<? extends ServerSocketChannel> getChannelClass()
    {
        if (Epoll.isAvailable())
        {
            return EpollServerSocketChannel.class;
        }
        else
        {
            return NioServerSocketChannel.class;
        }
    }

    private static EventLoopGroup newEventLoopGroup()
    {
        if (Epoll.isAvailable())
        {
            return new EpollEventLoopGroup();
        }
        else
        {
            return new NioEventLoopGroup();
        }
    }

    public Channel getChannel()
    {
        return channel;
    }

    public EventLoopGroup getBossGroup()
    {
        return bossGroup;
    }

    public EventLoopGroup getWorkerGroup()
    {
        return workerGroup;
    }

    public ExecutorService getSessionExecutor()
    {
        return sessionExecutor;
    }

    public int getActualHttpPort()
    {
        if (httpServer == null)
        {
            return httpPort;
        }
        return ((ServerConnector) httpServer.getConnectors()[0]).getLocalPort();
    }

    public void startHttp() throws Exception
    {
        Server server = new Server();
        this.httpServer = server;

        ServerConnector connector;
        if (this.sslConfig == null)
        {
            connector = new ServerConnector(server);
        }
        else
        {
            HttpConfiguration httpsConfig = new HttpConfiguration();
            httpsConfig.setSecureScheme("https");
            httpsConfig.addCustomizer(new SecureRequestCustomizer());

            SslContextFactory.Server sslContextFactory = buildSslContextFactory(this.sslConfig);
            connector = new ServerConnector(
                    server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfig));
        }
        connector.setPort(this.httpPort);
        server.addConnector(connector);

        // Static
        ResourceHandler staticResourceHandler = new ResourceHandler();
        staticResourceHandler.setDirAllowed(true);
        staticResourceHandler.setWelcomeFiles(new String[]{"index.html"});
        staticResourceHandler.setBaseResource(
                ResourceFactory.of(staticResourceHandler).newClassLoaderResource("/static/"));

        // Dynamic get Info
        ContextHandler dynamicContentHandler = new ContextHandler("/server/info");
        dynamicContentHandler.setHandler(new Handler.Abstract()
        {
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws IOException
            {
                response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json;charset=utf-8");
                response.setStatus(HttpStatus.OK_200);
                Content.Sink.write(response, true, MAPPER.writeValueAsString(
                        new Info(startTime, connectionsHistory, collectOpenSessionStats())
                ), callback);
                return true;
            }
        });

        // Prometheus
        this.registry = new PrometheusRegistry();
        this.userMetrics = new PrometheusUserMetrics(registry);
        this.connectionCount = Gauge.builder().name("serverLiveConnections").help("Total live connections count").register(this.registry);

        // Register HikariCP metrics
        sqlManager.registerHikariMetrics(this.registry);

        this.jvmRegistry = new PrometheusRegistry();
        JvmMetrics.builder().register(this.jvmRegistry);

        ServletContextHandler prometheusServletContext = new ServletContextHandler();
        prometheusServletContext.setContextPath("/");
        prometheusServletContext.addServlet(new ServletHolder(new PrometheusMetricsServlet(this.registry)), "/metrics/application");
        prometheusServletContext.addServlet(new ServletHolder(new PrometheusMetricsServlet(this.jvmRegistry)), "/metrics/jvm");

        // Manage collections
        server.setHandler(new Handler.Sequence(staticResourceHandler, dynamicContentHandler, prometheusServletContext));

        // Start the server
        server.start();
    }

    public PrometheusUserMetrics getUserMetrics()
    {
        return userMetrics;
    }

    private static SslContext buildWireSslContext(SSLConfig ssl)
    {
        if (ssl.getKeyStorePath() == null)
        {
            throw new IllegalStateException("'ssl.keyStorePath' is required when 'ssl' is configured");
        }
        if ((ssl.isNeedClientAuth() || ssl.isWantClientAuth()) && ssl.getTrustStorePath() == null)
        {
            throw new IllegalStateException("'ssl.trustStorePath' is required when client authentication (mTLS) is enabled");
        }

        try
        {
            KeyStore keyStore = KeyStore.getInstance(ssl.getKeyStoreType() == null ? "PKCS12" : ssl.getKeyStoreType());
            char[] keyStorePassword = ssl.getKeyStorePassword() == null ? null : ssl.getKeyStorePassword().toCharArray();
            try (InputStream in = new FileInputStream(ssl.getKeyStorePath()))
            {
                keyStore.load(in, keyStorePassword);
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            char[] keyManagerPassword = ssl.getKeyManagerPassword() != null
                    ? ssl.getKeyManagerPassword().toCharArray()
                    : keyStorePassword;
            keyManagerFactory.init(keyStore, keyManagerPassword);

            SslContextBuilder builder = SslContextBuilder.forServer(keyManagerFactory);

            if (ssl.getTrustStorePath() != null)
            {
                KeyStore trustStore = KeyStore.getInstance(ssl.getTrustStoreType() == null ? "PKCS12" : ssl.getTrustStoreType());
                char[] trustStorePassword = ssl.getTrustStorePassword() == null ? null : ssl.getTrustStorePassword().toCharArray();
                try (InputStream in = new FileInputStream(ssl.getTrustStorePath()))
                {
                    trustStore.load(in, trustStorePassword);
                }
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
                builder.trustManager(trustManagerFactory);
            }

            if (ssl.isNeedClientAuth())
            {
                builder.clientAuth(ClientAuth.REQUIRE);
            }
            else if (ssl.isWantClientAuth())
            {
                builder.clientAuth(ClientAuth.OPTIONAL);
            }

            List<String> protocols = ssl.getSupportedProtocols();
            if (protocols != null && !protocols.isEmpty())
            {
                builder.protocols(protocols.toArray(new String[0]));
            }

            List<String> ciphers = ssl.getSupportedCipherSuites();
            if (ciphers != null && !ciphers.isEmpty())
            {
                builder.ciphers(ciphers);
            }

            return builder.build();
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Failed to build TLS context for the Postgres wire protocol: " + e.getMessage(), e);
        }
    }

    private static SslContextFactory.Server buildSslContextFactory(SSLConfig ssl)
    {
        if (ssl.getKeyStorePath() == null)
        {
            throw new IllegalStateException("'ssl.keyStorePath' is required when 'ssl' is configured");
        }
        if ((ssl.isNeedClientAuth() || ssl.isWantClientAuth()) && ssl.getTrustStorePath() == null)
        {
            throw new IllegalStateException("'ssl.trustStorePath' is required when client authentication (mTLS) is enabled");
        }

        SslContextFactory.Server factory = new SslContextFactory.Server();
        factory.setKeyStorePath(ssl.getKeyStorePath());
        factory.setKeyStorePassword(ssl.getKeyStorePassword());
        factory.setKeyStoreType(ssl.getKeyStoreType() == null ? "PKCS12" : ssl.getKeyStoreType());
        if (ssl.getKeyManagerPassword() != null)
        {
            factory.setKeyManagerPassword(ssl.getKeyManagerPassword());
        }

        if (ssl.getTrustStorePath() != null)
        {
            factory.setTrustStorePath(ssl.getTrustStorePath());
            factory.setTrustStorePassword(ssl.getTrustStorePassword());
            factory.setTrustStoreType(ssl.getTrustStoreType() == null ? "PKCS12" : ssl.getTrustStoreType());
        }

        if (ssl.isNeedClientAuth())
        {
            factory.setNeedClientAuth(true);
        }
        else if (ssl.isWantClientAuth())
        {
            factory.setWantClientAuth(true);
        }

        List<String> protocols = ssl.getSupportedProtocols();
        factory.setIncludeProtocols((protocols == null || protocols.isEmpty()
                ? Arrays.asList("TLSv1.3", "TLSv1.2")
                : protocols).toArray(new String[0]));

        List<String> ciphers = ssl.getSupportedCipherSuites();
        if (ciphers != null && !ciphers.isEmpty())
        {
            factory.setIncludeCipherSuites(ciphers.toArray(new String[0]));
            factory.setUseCipherSuitesOrder(true);
        }
        return factory;
    }

    private List<SessionStats> collectOpenSessionStats()
    {
        List<SessionStats> snapshot = new ArrayList<>();
        for (PostgresWireProtocol conn : liveConnections)
        {
            snapshot.add(conn.getSessionStats());
        }
        return snapshot;
    }

    private static class Info
    {
        public Info()
        {
            memory.total = Runtime.getRuntime().totalMemory();
            memory.max = Runtime.getRuntime().maxMemory();
            memory.free = Runtime.getRuntime().freeMemory();
        }

        public Info(Date date, List<SessionStats> history, List<SessionStats> openSessions)
        {
            this();
            this.startedTime = date.toString();

            this.history = history;
            this.openSessions = openSessions;

            try
            {
                this.hostAddress = InetAddress.getLocalHost().getCanonicalHostName();
            }
            catch (UnknownHostException e)
            {
                throw new RuntimeException(e);
            }

            this.timeZone = java.util.TimeZone.getDefault().getID();
        }

        public List<SessionStats> history;

        public List<SessionStats> openSessions;

        public String startedTime;

        public String hostAddress;

        public String timeZone;

        public Memory memory = new Memory();
    }

    private static class Memory
    {
        public long total;

        public long max;

        public long free;
    }
}
