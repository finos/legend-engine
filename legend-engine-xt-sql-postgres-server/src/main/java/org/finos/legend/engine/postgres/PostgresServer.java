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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.finos.legend.engine.postgres.auth.AuthenticationMethod;
import org.finos.legend.engine.postgres.auth.AuthenticationProvider;
import org.finos.legend.engine.postgres.config.Builder;
import org.finos.legend.engine.postgres.config.GSSConfig;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.finos.legend.engine.postgres.transport.Netty4OpenChannelsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresServer
{

    private static final Logger logger = LoggerFactory.getLogger(PostgresServer.class);
    private final int port;
    private final SessionsFactory sessionsFactory;
    private final AuthenticationProvider authenticationProvider;
    private final GSSConfig gssConfig;
    private Channel channel;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    public PostgresServer(ServerConfig serverConfig, SessionsFactory sessionsFactory, AuthenticationProvider authenticationProvider)
    {
        this.port = serverConfig.getPort();
        this.sessionsFactory = sessionsFactory;
        this.authenticationProvider = authenticationProvider;
        this.gssConfig = serverConfig.getGss();
    }

    public void run()
    {

        Netty4OpenChannelsHandler openChannelsHandler = new Netty4OpenChannelsHandler(
                LoggerFactory.getLogger(Netty4OpenChannelsHandler.class));

        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        SocketAddress socketAddress = new InetSocketAddress(port);
        try
        {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(socketAddress)
                    .childHandler(new ChannelInitializer<SocketChannel>()
                    {
                        @Override
                        protected void initChannel(SocketChannel ch)
                        {
                            PostgresWireProtocol postgresWireProtocol = new PostgresWireProtocol(sessionsFactory,
                                    authenticationProvider, gssConfig, () -> null);
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

    public Channel getChannel()
    {
        return channel;
    }

    public NioEventLoopGroup getBossGroup()
    {
        return bossGroup;
    }

    public NioEventLoopGroup getWorkerGroup()
    {
        return workerGroup;
    }

    public static void main(String[] args) throws Exception
    {
        //TODO ADD CLI
        String configPath = args[0];
        InputStream configStream = new FileInputStream(new File(configPath));
        ObjectMapper objectMapper = new ObjectMapper();
        ServerConfig serverConfig = objectMapper.readValue(configStream, ServerConfig.class);
        SessionsFactory sessionFactory = Builder.buildSessionFactory(serverConfig);
        AuthenticationMethod authenticationMethod = Builder.buildAuthenticationMethod(serverConfig);
        if (serverConfig.getGss() != null)
        {
            System.setProperty("java.security.krb5.conf", serverConfig.getGss().getKerberosConfigFile());
        }

        logger.info("Starting server in port: " + serverConfig.getPort());
        new PostgresServer(serverConfig, sessionFactory, (user, connectionProperties) -> authenticationMethod).run();
    }
}


