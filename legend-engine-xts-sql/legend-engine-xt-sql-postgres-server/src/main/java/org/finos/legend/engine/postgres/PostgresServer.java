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

import jakarta.inject.Inject;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.finos.legend.engine.postgres.auth.AuthenticationProvider;
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
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Messages messages;


    @Inject
    public PostgresServer(ServerConfig serverConfig, SessionsFactory sessionsFactory, AuthenticationProvider authenticationProvider, Messages messages)
    {
        this.port = serverConfig.getPort();
        this.sessionsFactory = sessionsFactory;
        this.authenticationProvider = authenticationProvider;
        this.gssConfig = serverConfig.getGss();
        this.messages = messages;
    }

    public void run()
    {

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
                            PostgresWireProtocol postgresWireProtocol = new PostgresWireProtocol(sessionsFactory,
                                    authenticationProvider, gssConfig, () -> null, messages);
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

}


