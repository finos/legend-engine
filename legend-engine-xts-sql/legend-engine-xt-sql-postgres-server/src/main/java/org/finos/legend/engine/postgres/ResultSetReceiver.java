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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.finos.legend.engine.postgres.DelayableWriteChannel.DelayedWrites;
import org.finos.legend.engine.postgres.handler.PostgresResultSet;
import org.finos.legend.engine.postgres.handler.PostgresResultSetMetaData;
import org.finos.legend.engine.postgres.types.PGType;
import org.finos.legend.engine.postgres.types.PGTypes;
import org.slf4j.Logger;

class ResultSetReceiver
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Messages.class);


    private final String query;
    private final DelayableWriteChannel channel;
    private boolean isSimpleQuery;
    private Channel directChannel;
    private DelayedWrites delayedWrites;
    private final FormatCodes.FormatCode[] formatCodes;

    private CompletableFuture<Void> completionFuture = new CompletableFuture<>();

    private final long rowCount = 0;

    ResultSetReceiver(String query, DelayableWriteChannel channel, DelayedWrites delayedWrites,
                      boolean isSimpleQuery, FormatCodes.FormatCode[] formatCodes)
    {
        this.query = query;
        this.channel = channel;
        this.isSimpleQuery = isSimpleQuery;
        this.delayedWrites = delayedWrites;
        this.formatCodes = formatCodes;
        this.directChannel = this.channel.bypassDelay();
    }


    public void sendResultSet(PostgresResultSet rs) throws Exception
    {
        int rowCount = 0;
        if (rs != null)
        {
            if (isSimpleQuery)
            {
                //Simple query requires to send description
                Messages.sendRowDescription(directChannel, rs.getMetaData(), formatCodes);
            }
            PostgresResultSetMetaData metaData = rs.getMetaData();
            List<PGType> columnTypes = new ArrayList<>(metaData.getColumnCount());
            for (int i = 0; i < metaData.getColumnCount(); i++)
            {
                PGType pgType = PGTypes.get(metaData.getColumnType(i + 1), metaData.getScale(i + 1));
                columnTypes.add(pgType);
            }
            while (rs.next())
            {
                rowCount++;
                Messages.sendDataRow(directChannel, rs, columnTypes, null);
                if (rowCount % 1000 == 0)
                {   //TODO REMOVE FLASH FROM Messages.sendDataRow
                    directChannel.flush();
                }
            }
        }
        LOGGER.info("Query complete with row count {}", rowCount);
    }

    public void allFinished()
    {
        ChannelFuture sendCommandComplete = Messages.sendCommandComplete(directChannel, query, rowCount);
        channel.writePendingMessages(delayedWrites);
        channel.flush();
        sendCommandComplete.addListener(future -> completionFuture.complete(null));
    }


    public void fail(Throwable throwable)
    {
        ChannelFuture sendErrorResponse = Messages.sendErrorResponse(directChannel, throwable);
        channel.writePendingMessages(delayedWrites);
        channel.flush();
        sendErrorResponse.addListener(f -> completionFuture.completeExceptionally(throwable));
    }

    public CompletableFuture<Void> completionFuture()
    {
        return completionFuture;
    }
}
