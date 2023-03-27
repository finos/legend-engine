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

package org.finos.legend.engine.postgres.transport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

@ChannelHandler.Sharable
public class Netty4OpenChannelsHandler extends
        ChannelInboundHandlerAdapter /*implements Releasable*/
{

    final Set<Channel> openChannels = Collections.newSetFromMap(new ConcurrentHashMap<>());
    /* final CounterMetric openChannelsMetric = new CounterMetric();
     final CounterMetric totalChannelsMetric = new CounterMetric();
  */
    final Logger logger;

    public Netty4OpenChannelsHandler(Logger logger)
    {
        this.logger = logger;
    }


    final ChannelFutureListener remover = new ChannelFutureListener()
    {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception
        {
            boolean removed = openChannels.remove(future.channel());
            if (removed)
            {
                // openChannelsMetric.dec();
            }
            if (logger.isTraceEnabled())
            {
                logger.trace("channel closed: {}", future.channel());
            }
        }
    };

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        if (logger.isTraceEnabled())
        {
            logger.trace("channel opened: {}", ctx.channel());
        }
        final boolean added = openChannels.add(ctx.channel());
        if (added)
        {
            // openChannelsMetric.inc();
            // totalChannelsMetric.inc();
            ctx.channel().closeFuture().addListener(remover);
        }

        super.channelActive(ctx);
    }

 /*   public long numberOfOpenChannels() {
        return openChannelsMetric.count();
    }

    public long totalChannels() {
        return totalChannelsMetric.count();
    }*/


    public void close()
    {
        try
        {
            closeChannels(openChannels);
        }
        catch (IOException e)
        {
            logger.trace("exception while closing channels", e);
        }
        openChannels.clear();
    }


    public static void closeChannels(final Collection<Channel> channels) throws IOException
    {
        IOException closingExceptions = null;
        final List<ChannelFuture> futures = new ArrayList<>();
        for (final Channel channel : channels)
        {
            try
            {
                if (channel != null && channel.isOpen())
                {
                    futures.add(channel.close());
                }
            }
            catch (Exception e)
            {
                if (closingExceptions == null)
                {
                    closingExceptions = new IOException("failed to close channels");
                }
                closingExceptions.addSuppressed(e);
            }
        }
        for (final ChannelFuture future : futures)
        {
            future.awaitUninterruptibly();
        }

        if (closingExceptions != null)
        {
            throw closingExceptions;
        }
    }


}
