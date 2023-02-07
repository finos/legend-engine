// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.pg.postgres;

import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import org.finos.legend.engine.SessionsFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class TestPostgresServer extends PostgresServer
{

    public TestPostgresServer(int port, SessionsFactory sessionsFactory)
    {
        super(port, sessionsFactory);
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
            channel = null;
        }
    }

    public void shutDown()
    {
        NioEventLoopGroup bossGroup = this.getBossGroup();
        NioEventLoopGroup workerGroup = this.getWorkerGroup();
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
