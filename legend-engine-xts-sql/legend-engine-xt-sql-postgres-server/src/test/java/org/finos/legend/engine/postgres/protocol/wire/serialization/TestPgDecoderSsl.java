// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.postgres.protocol.wire.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class TestPgDecoderSsl
{
    private static final int SSL_REQUEST_CODE = 80877103;
    private static final int CANCEL_CODE = 80877102;
    private static final int PROTOCOL_VERSION_3_0 = 196608;

    private static SslContext serverSslContext;

    @BeforeClass
    public static void buildSslContext() throws Exception
    {
        SelfSignedCertificate cert = new SelfSignedCertificate();
        serverSslContext = SslContextBuilder.forServer(cert.certificate(), cert.privateKey()).build();
    }

    private static ByteBuf sslRequest()
    {
        ByteBuf buf = Unpooled.buffer(8);
        buf.writeInt(8);
        buf.writeInt(SSL_REQUEST_CODE);
        return buf;
    }

    private static ByteBuf startupMessage()
    {
        ByteBuf buf = Unpooled.buffer(8);
        buf.writeInt(8);
        buf.writeInt(PROTOCOL_VERSION_3_0);
        return buf;
    }

    private static ByteBuf cancelRequest()
    {
        ByteBuf buf = Unpooled.buffer(16);
        buf.writeInt(16);
        buf.writeInt(CANCEL_CODE);
        buf.writeInt(42);   // pid
        buf.writeInt(99);   // secret key
        return buf;
    }

    @Test
    public void sslRequestWithContextIsAcceptedAndUpgradesPipeline()
    {
        Supplier<SslContext> supplier = () -> serverSslContext;
        EmbeddedChannel channel = new EmbeddedChannel(new PgDecoder(supplier, false));

        channel.writeInbound(sslRequest());

        ByteBuf reply = channel.readOutbound();
        Assert.assertNotNull("server must answer the SSLRequest", reply);
        Assert.assertEquals('S', reply.readByte());
        Assert.assertNotNull("TLS handler must be inserted after accepting SSL", channel.pipeline().get(SslHandler.class));

        channel.finishAndReleaseAll();
    }

    @Test
    public void sslRequestWithoutContextIsDeclined()
    {
        Supplier<SslContext> supplier = () -> null;
        EmbeddedChannel channel = new EmbeddedChannel(new PgDecoder(supplier, false));

        channel.writeInbound(sslRequest());

        ByteBuf reply = channel.readOutbound();
        Assert.assertNotNull(reply);
        Assert.assertEquals('N', reply.readByte());
        Assert.assertNull(channel.pipeline().get(SslHandler.class));

        channel.finishAndReleaseAll();
    }

    @Test
    public void plaintextStartupIsAllowedWhenSslNotRequired()
    {
        EmbeddedChannel channel = new EmbeddedChannel(new PgDecoder(() -> null, false));

        channel.writeInbound(startupMessage());

        Assert.assertTrue("plaintext startup must proceed when requireSsl is off", channel.isOpen());
        Assert.assertNull("no ErrorResponse should be emitted", channel.readOutbound());
        Assert.assertNotNull("startup payload must be forwarded", channel.readInbound());

        channel.finishAndReleaseAll();
    }

    @Test
    public void plaintextStartupIsRejectedWhenSslRequired()
    {
        EmbeddedChannel channel = new EmbeddedChannel(new PgDecoder(() -> serverSslContext, true));

        channel.writeInbound(startupMessage());

        ByteBuf reply = channel.readOutbound();
        Assert.assertNotNull("requireSsl must emit a fatal ErrorResponse", reply);
        Assert.assertEquals('E', reply.readByte());

        int length = reply.readInt();
        byte[] body = new byte[length - 4];
        reply.readBytes(body);
        Assert.assertTrue("error must mention SSL requirement",
                new String(body, StandardCharsets.UTF_8).contains("SSL connection is required"));

        Assert.assertFalse("connection must be closed after rejection", channel.isOpen());

        channel.finishAndReleaseAll();
    }

    @Test
    public void cancelRequestIsExemptFromSslRequirement()
    {
        EmbeddedChannel channel = new EmbeddedChannel(new PgDecoder(() -> serverSslContext, true));

        channel.writeInbound(cancelRequest());

        Assert.assertTrue("CancelRequest must not be rejected by requireSsl", channel.isOpen());
        Assert.assertNull("no ErrorResponse should be emitted for a CancelRequest", channel.readOutbound());
        Assert.assertNotNull("cancel payload must be forwarded", channel.readInbound());

        channel.finishAndReleaseAll();
    }

    @Test
    public void tlsClientReachesStartupWhenSslRequired()
    {
        EmbeddedChannel channel = new EmbeddedChannel(new PgDecoder(() -> serverSslContext, true));

        channel.writeInbound(sslRequest());
        ByteBuf sslReply = channel.readOutbound();
        Assert.assertEquals('S', sslReply.readByte());
        Assert.assertNotNull(channel.pipeline().get(SslHandler.class));
        Assert.assertTrue("TLS client must not be rejected", channel.isOpen());

        channel.finishAndReleaseAll();
    }
}

