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

package org.finos.legend.engine.postgres.protocol.wire.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSession;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.List;
import java.util.function.Supplier;

public class PgDecoder extends ByteToMessageDecoder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PgDecoder.class);

    static final int CANCEL_CODE = 80877102;
    static final int SSL_REQUEST_CODE = 80877103;

    static final int MIN_STARTUP_LENGTH = 8;
    static final int MIN_MSG_LENGTH = 5;

    public enum State
    {

        /**
         * In this state we expect a startup message.
         * <p>
         * Startup payload is int32: length int32: requestCode (ssl | protocol version | cancel)
         * [payload (parameters)] | [KeyData(int32, int32) in case of cancel]
         */
        STARTUP,

        /**
         * In this state the handler must consume the startup payload and then call
         * {@link #startupDone()}
         */
        STARTUP_PARAMETERS,

        /**
         * In this state the handler must read the KeyData and cancel the query
         */
        CANCEL,

        /**
         * In this state we expect a message.
         * <p>
         * Message payload is byte: message type int32: length [payload]
         */
        MSG,
    }


    {
        setCumulator(COMPOSITE_CUMULATOR);
    }

    private final Supplier<SslContext> getSslContext;
    private final boolean requireSsl;

    private State state = State.STARTUP;
    private byte msgType;
    private int payloadLength;


    public PgDecoder(Supplier<SslContext> getSslContext)
    {
        this(getSslContext, false);
    }

    public PgDecoder(Supplier<SslContext> getSslContext, boolean requireSsl)
    {
        this.getSslContext = getSslContext;
        this.requireSsl = requireSsl;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        ByteBuf decode = decode(ctx, in);
        if (decode != null)
        {
            out.add(decode);
        }
    }

    private ByteBuf decode(ChannelHandlerContext ctx, ByteBuf in)
    {
        switch (state)
        {
            case STARTUP:
            {
                if (in.readableBytes() < MIN_STARTUP_LENGTH)
                {
                    return null;
                }

                in.markReaderIndex();
                payloadLength = in.readInt() - 8;
                int requestCode = in.readInt();

                if (requestCode == SSL_REQUEST_CODE)
                {
                    SslContext sslContext = getSslContext.get();
                    Channel channel = ctx.channel();
                    ByteBuf out = ctx.alloc().buffer(1);
                    if (sslContext == null)
                    {
                        channel.writeAndFlush(out.writeByte('N'));
                    }
                    else
                    {
                        channel.writeAndFlush(out.writeByte('S'));
                        SslHandler sslHandler = sslContext.newHandler(ctx.alloc());
                        sslHandler.handshakeFuture().addListener(future ->
                        {
                            if (future.isSuccess())
                            {
                                logConnectionSecurity(sslHandler.engine().getSession());
                            }
                            else
                            {
                                LOGGER.info("TLS handshake failed for client {}", channel.remoteAddress(), future.cause());
                            }
                        });
                        ctx.pipeline().addFirst(sslHandler);
                    }
                    in.markReaderIndex();
                    return decode(ctx, in);
                }
                else
                {
                    if (in.readableBytes() < payloadLength)
                    {
                        in.resetReaderIndex();
                        return null;
                    }
                    // A CancelRequest carries no query/result data and is commonly sent on a fresh
                    // plaintext connection, so it is exempt from the requireSsl check.
                    if (requireSsl && requestCode != CANCEL_CODE && ctx.pipeline().get(SslHandler.class) == null)
                    {
                        rejectPlaintext(ctx);
                        return null;
                    }
                    state = requestCode == CANCEL_CODE ? State.CANCEL : State.STARTUP_PARAMETERS;
                    return in.readBytes(payloadLength);
                }
            }

            /**
             * Message payload is
             *  byte: message type
             *  int32: length (excluding message type, including length itself)
             *  payload
             **/
            case MSG:
            {
                if (in.readableBytes() < MIN_MSG_LENGTH)
                {
                    return null;
                }
                in.markReaderIndex();
                msgType = in.readByte();
                payloadLength = in.readInt() - 4; // exclude length itself

                if (in.readableBytes() < payloadLength)
                {
                    in.resetReaderIndex();
                    return null;
                }
                return in.readBytes(payloadLength);
            }
            default:
                throw new IllegalStateException("Invalid state " + state);
        }
    }

    private static void logConnectionSecurity(SSLSession session)
    {
        Certificate[] peerCertificates;
        try
        {
            peerCertificates = session.getPeerCertificates();
        }
        catch (javax.net.ssl.SSLPeerUnverifiedException e)
        {
            peerCertificates = null;
        }
        boolean mtls = peerCertificates != null && peerCertificates.length > 0;
        LOGGER.info("Client connected using SSL (protocol={}, cipher={}), mTLS={}", session.getProtocol(), session.getCipherSuite(), mtls);
    }

    private static void rejectPlaintext(ChannelHandlerContext ctx)
    {
        // Postgres ErrorResponse: 'E', int32 length, then null-terminated (code, value) fields,
        // terminated by a trailing zero byte. SQLSTATE 08P01 == protocol_violation.
        byte[] severity = "FATAL".getBytes(StandardCharsets.UTF_8);
        byte[] sqlState = "08P01".getBytes(StandardCharsets.UTF_8);
        byte[] message = "SSL connection is required".getBytes(StandardCharsets.UTF_8);

        int length = 4
                + (1 + severity.length + 1)
                + (1 + sqlState.length + 1)
                + (1 + message.length + 1)
                + 1;

        ByteBuf out = ctx.alloc().buffer(1 + length);
        out.writeByte('E');
        out.writeInt(length);
        out.writeByte('S').writeBytes(severity).writeByte(0);
        out.writeByte('C').writeBytes(sqlState).writeByte(0);
        out.writeByte('M').writeBytes(message).writeByte(0);
        out.writeByte(0);

        ctx.channel().writeAndFlush(out).addListener(ChannelFutureListener.CLOSE);
    }

    public void startupDone()
    {
        Assert.assertTrue(state == State.STARTUP_PARAMETERS,
                () -> "Must only call startupDone if state == STARTUP_PARAMETERS");
        state = State.MSG;
    }

    public byte msgType()
    {
        return msgType;
    }

    public int payloadLength()
    {
        return payloadLength;
    }

    public State state()
    {
        return state;
    }
}
