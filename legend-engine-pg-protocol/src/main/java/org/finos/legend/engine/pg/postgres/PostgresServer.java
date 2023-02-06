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

package org.finos.legend.engine.pg.postgres;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.finos.legend.engine.SessionsFactory;
import org.finos.legend.engine.pg.postgres.legend.LegendTdsClient;
import org.finos.legend.engine.pg.postgres.auth.Authentication;
import org.finos.legend.engine.pg.postgres.auth.AuthenticationMethod;
import org.finos.legend.engine.pg.postgres.auth.User;
import org.finos.legend.engine.pg.postgres.legend.LegendSessionFactory;
import org.finos.legend.engine.pg.postgres.transport.Netty4OpenChannelsHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class PostgresServer
{

  private final int port;
  private final SessionsFactory sessionsFactory;
  private Channel channel;
  private NioEventLoopGroup bossGroup;
  private NioEventLoopGroup workerGroup;

  public PostgresServer(int port, SessionsFactory sessionsFactory)
  {
    this.port = port;
    this.sessionsFactory = sessionsFactory;
  }

  public void run()
  {
    Authentication authentication = (user, connectionProperties) -> new AuthenticationMethod()
    {
      @Nullable
      @Override
      public User authenticate(final String userName, @Nullable String passwd,
          ConnectionProperties connProperties)
      {
        return () -> userName;
      }

      @Override
      public String name()
      {
        return null;
      }
    };

    Netty4OpenChannelsHandler openChannelsHandler = new Netty4OpenChannelsHandler(
        LoggerFactory.getLogger(Netty4OpenChannelsHandler.class));

    bossGroup = new NioEventLoopGroup();
    workerGroup = new NioEventLoopGroup();
    SocketAddress socketAddress = new InetSocketAddress("127.0.0.1", port);
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
                  authentication, () -> null);
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

  public static void main(String[] args)
  {
    int port = 9998;
    CookieStore cookieStore = new BasicCookieStore();
    BasicClientCookie legendSdlcJsessionid = new BasicClientCookie("LEGEND_SDLC_JSESSIONID", "node0k8i6uzjro6s4tdx2zwqmv8kt1.node0");
    legendSdlcJsessionid.setDomain("localhost");
    cookieStore.addCookie(legendSdlcJsessionid);
    LegendTdsClient client = new LegendTdsClient("localhost", "6300", "SAMPLE-40302763", cookieStore);
    new PostgresServer(port, new LegendSessionFactory(client)).run();
  }
}


