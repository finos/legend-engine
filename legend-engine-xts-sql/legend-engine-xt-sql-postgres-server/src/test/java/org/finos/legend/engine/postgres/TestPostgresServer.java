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

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.eclipse.collections.api.block.function.Function2;
import org.finos.legend.engine.postgres.protocol.wire.auth.method.AuthenticationMethod;
import org.finos.legend.engine.postgres.protocol.wire.auth.method.ConnectionProperties;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.finos.legend.engine.postgres.protocol.sql.SQLManager;
import org.finos.legend.engine.postgres.protocol.wire.serialization.Messages;

public class TestPostgresServer extends PostgresServer
{

    public TestPostgresServer(ServerConfig serverConfig,
                              SQLManager sqlManager,
                              Function2<String, ConnectionProperties, AuthenticationMethod> authenticationProvider, Messages messages)
    {
        super(serverConfig, sqlManager, authenticationProvider, messages);
    }

    public void startUp()
    {
        this.run();
    }

    public void stopListening()
    {
        Channel channel = this.getChannel();
        if (channel != null)
        {
            channel.close().syncUninterruptibly();
        }
    }

    public void shutDown()
    {
        EventLoopGroup bossGroup = this.getBossGroup();
        EventLoopGroup workerGroup = this.getWorkerGroup();
        if (bossGroup != null)
        {
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
        if (workerGroup != null)
        {
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
        if (bossGroup != null)
        {
            bossGroup.terminationFuture().syncUninterruptibly();
        }
        if (workerGroup != null)
        {
            workerGroup.terminationFuture().syncUninterruptibly();
        }
    }

    public InetSocketAddress getLocalAddress()
    {
        return (InetSocketAddress) this.getChannel().localAddress();
    }
}
