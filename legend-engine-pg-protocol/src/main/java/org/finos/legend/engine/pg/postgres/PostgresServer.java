package org.finos.legend.engine.pg.postgres;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.finos.legend.engine.Session;
import org.finos.legend.engine.SessionHandler;
import org.finos.legend.engine.SessionsFactory;
import org.finos.legend.engine.pg.postgres.auth.Authentication;
import org.finos.legend.engine.pg.postgres.auth.AuthenticationMethod;
import org.finos.legend.engine.pg.postgres.auth.User;
import org.finos.legend.engine.pg.postgres.jdbc.JDBCSessionFactory;
import org.finos.legend.engine.pg.postgres.transport.Netty4OpenChannelsHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class PostgresServer {

    private int port;
    private SessionsFactory sessionsFactory;

    public PostgresServer(int port, SessionsFactory sessionsFactory) {
        this.port = port;
        this.sessionsFactory = sessionsFactory;
    }

    public void run() {
        Authentication authentication = new Authentication() {
            @Nullable
            @Override
            public AuthenticationMethod resolveAuthenticationType(String user, ConnectionProperties connectionProperties) {
                return new AuthenticationMethod() {
                    @Nullable
                    @Override
                    public User authenticate(final String userName, @Nullable String passwd, ConnectionProperties connProperties) {
                        return new User() {
                            @Override
                            public String name() {
                                return userName;
                            }
                        };
                    }

                    @Override
                    public String name() {
                        return null;
                    }
                };
            }
        };

        SSLContext sslContext = null;

        Netty4OpenChannelsHandler openChannelsHandler = new Netty4OpenChannelsHandler(LoggerFactory.getLogger(Netty4OpenChannelsHandler.class));


        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            PostgresWireProtocol postgresWireProtocol = new PostgresWireProtocol(sessionsFactory, authentication, () -> null);
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("open_channels", openChannelsHandler);
                            pipeline.addLast("frame-decoder", postgresWireProtocol.decoder);
                            pipeline.addLast("handler", postgresWireProtocol.handler);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            //Bind and start accept incoming connections
            ChannelFuture future = bootstrap.bind(port).sync();


            //Wait until server socket is closed

            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        int port =  9998;
        new PostgresServer(port, new JDBCSessionFactory("jdbc:postgresql://localhost:5432/postgres", "postgres", "vika")).run();
    }
}


