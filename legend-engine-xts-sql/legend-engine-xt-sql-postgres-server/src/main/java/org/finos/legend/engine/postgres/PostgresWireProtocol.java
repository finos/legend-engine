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

import com.google.common.net.InetAddresses;
import com.sun.security.jgss.GSSUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Scope;
import org.finos.legend.engine.postgres.auth.AuthenticationMethod;
import org.finos.legend.engine.postgres.auth.AuthenticationMethodType;
import org.finos.legend.engine.postgres.auth.AuthenticationProvider;
import org.finos.legend.engine.postgres.auth.KerberosIdentityProvider;
import org.finos.legend.engine.postgres.config.GSSConfig;
import org.finos.legend.engine.postgres.handler.PostgresResultSetMetaData;
import org.finos.legend.engine.postgres.types.PGType;
import org.finos.legend.engine.postgres.types.PGTypes;
import org.finos.legend.engine.postgres.utils.ExceptionUtil;
import org.finos.legend.engine.postgres.utils.OpenTelemetry;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.MDC;

import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.ParameterMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.finos.legend.engine.postgres.FormatCodes.getFormatCode;


/**
 * Netty Handler/FrameDecoder for the Postgres wire protocol.<br /> This class handles the message
 * flow and dispatching
 * <p>
 * <p>
 * <pre>
 *      Client                              Server
 *
 *  (optional ssl negotiation)
 *
 *
 *          |    SSLRequest                    |
 *          |--------------------------------->|
 *          |                                  |
 *          |     'S' | 'N' | error            |   (supported in Enterprise version)
 *          |<---------------------------------|
 *
 *
 *  startup:
 *  The authentication flow is handled by implementations of {@link AuthenticationMethod}.
 *
 *          |                                  |
 *          |      StartupMessage              |
 *          |--------------------------------->|
 *          |                                  |
 *          |      Authentication Method      |
 *          |      or                          |
 *          |      AuthenticationOK            |
 *          |      or                          |
 *          |      ErrorResponse               |
 *          |<---------------------------------|
 *          |                                  |
 *          |       ParameterStatus            |
 *          |<---------------------------------|
 *          |                                  |
 *          |       ReadyForQuery              |
 *          |<---------------------------------|
 *
 *
 * Simple Query:
 *
 *          +                                  +
 *          |   Q (query)                      |
 *          |--------------------------------->|
 *          |                                  |
 *          |     RowDescription               |
 *          |<---------------------------------|
 *          |                                  |
 *          |     DataRow                      |
 *          |<---------------------------------|
 *          |     DataRow                      |
 *          |<---------------------------------|
 *          |     CommandComplete              |
 *          |<---------------------------------|
 *          |     ReadyForQuery                |
 *          |<---------------------------------|
 *
 * Extended Query
 *
 *          +                                  +
 *          |  Parse                           |
 *          |--------------------------------->|
 *          |                                  |
 *          |  ParseComplete or ErrorResponse  |
 *          |<---------------------------------|
 *          |                                  |
 *          |  Describe Statement (optional)   |
 *          |--------------------------------->|
 *          |                                  |
 *          |  ParameterDescription (optional) |
 *          |<-------------------------------- |
 *          |                                  |
 *          |  RowDescription (optional)       |
 *          |<-------------------------------- |
 *          |                                  |
 *          |  Bind                            |
 *          |--------------------------------->|
 *          |                                  |
 *          |  BindComplete or ErrorResponse   |
 *          |<---------------------------------|
 *          |                                  |
 *          |  Describe Portal (optional)      |
 *          |--------------------------------->|
 *          |                                  |
 *          |  RowDescription (optional)       |
 *          |<-------------------------------- |
 *          |                                  |
 *          |  Execute                         |
 *          |--------------------------------->|
 *          |                                  |
 *          |  DataRow |                       |
 *          |  CommandComplete |               |
 *          |  EmptyQueryResponse |            |
 *          |  ErrorResponse                   |
 *          |<---------------------------------|
 *          |                                  |
 *          |  Sync                            |
 *          |--------------------------------->|
 *          |                                  |
 *          |  ReadyForQuery                   |
 *          |<---------------------------------|
 * </pre>
 * <p>
 * Take a look at {@link Messages} to see how the messages are structured.
 * <p>
 * See https://www.postgresql.org/docs/current/static/protocol-flow.html for a more detailed
 * description of the message flow
 */

public class PostgresWireProtocol
{

    //private static final Logger LOGGER = LogManager.getLogger(PostgresWireProtocol.class);
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(
            PostgresWireProtocol.class);

    public static int SERVER_VERSION_NUM = 100500;
    public static String PG_SERVER_VERSION = "10.5";

    final PgDecoder decoder;
    final MessageHandler handler;
    private final SessionsFactory sessions;
    /* private final Function<CoordinatorSessionSettings, AccessControl> getAccessControl;*/
    private final AuthenticationProvider authService;
    private final GSSConfig gssConfig;
    /*  private final Consumer<ChannelPipeline> addTransportHandler;
     */
    private DelayableWriteChannel channel;
    Session session;
    private boolean ignoreTillSync = false;
    private AuthenticationContext authContext;
    private Properties properties;

    public PostgresWireProtocol(SessionsFactory sessions,
            /*Function<CoordinatorSessionSettings, AccessControl> getAcessControl,*/
            /*Consumer<ChannelPipeline> addTransportHandler,*/
                                AuthenticationProvider authService,
                                GSSConfig gssConfig, Supplier<SslContext> getSslContext)
    {
        this.sessions = sessions;
        //this.getAccessControl = getAcessControl;
        //this.addTransportHandler = addTransportHandler;
        this.authService = authService;
        this.decoder = new PgDecoder(getSslContext);
        this.handler = new MessageHandler();
        this.gssConfig = gssConfig;
    }


    static String readCString(ByteBuf buffer)
    {
        byte[] bytes = new byte[buffer.bytesBefore((byte) 0) + 1];
        if (bytes.length == 0)
        {
            return null;
        }
        buffer.readBytes(bytes);
        return new String(bytes, 0, bytes.length - 1, StandardCharsets.UTF_8);
    }


    private static char[] readCharArray(ByteBuf buffer)
    {
        byte[] bytes = new byte[buffer.bytesBefore((byte) 0) + 1];
        if (bytes.length == 0)
        {
            return null;
        }
        buffer.readBytes(bytes);
        return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).array();
    }

    private static byte[] readByteArray(ByteBuf buffer, int payloadLength)
    {
        if (payloadLength == 0)
        {
            return null;
        }
        byte[] bytes = new byte[payloadLength];
        buffer.readBytes(bytes);
        return bytes;
    }

    private Properties readStartupMessage(ByteBuf buffer)
    {
        Properties properties = new Properties();
        while (true)
        {
            String key = readCString(buffer);
            if (key == null)
            {
                break;
            }
            String value = readCString(buffer);
            LOGGER.trace("payload: key={} value={}", key, value);
            if (!"".equals(key) && !"".equals(value))
            {
                properties.setProperty(key, value);
            }
        }
        return properties;
    }

    private static class ReadyForQueryCallback implements BiConsumer<Object, Throwable>
    {

        private final Channel channel;
        //private final TransactionState transactionState;

        private ReadyForQueryCallback(Channel channel)
        {
            this.channel = channel;
            //this.transactionState = transactionState;
        }

        @Override
        public void accept(Object result, Throwable t)
        {
            boolean clientInterrupted = t instanceof ClientInterrupted
                    || (t != null && t.getCause() instanceof ClientInterrupted);
            if (!clientInterrupted)
            {
                Messages.sendReadyForQuery(channel);
            }
        }
    }

    private class MessageHandler extends SimpleChannelInboundHandler<ByteBuf>
    {

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception
        {
            channel = new DelayableWriteChannel(ctx.channel());
        }

        @Override
        public boolean acceptInboundMessage(Object msg) throws Exception
        {
            return true;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception
        {
            assert channel != null : "Channel must be initialized";
            try
            {
                dispatchState(buffer, channel);
            }
            catch (Throwable t)
            {
                ignoreTillSync = true;
                try
                {
                    /*AccessControl accessControl = session == null
                        ? AccessControl.DISABLED
                        : getAccessControl.apply(session.sessionSettings());
                    Messages.sendErrorResponse(channel, accessControl, t);
                    */
                    LOGGER.error("Unable to handle query", t);
                    Messages.sendErrorResponse(channel, t);
                }
                catch (Throwable ti)
                {
                    LOGGER.error("Error trying to send error to client: {}", t, ti);
                }
            }
        }

        private void dispatchState(ByteBuf buffer, DelayableWriteChannel channel) throws Exception
        {
            switch (decoder.state())
            {
                case STARTUP_PARAMETERS:
                    handleStartupBody(buffer, channel);
                    decoder.startupDone();
                    return;

                case CANCEL:
                    handleCancelRequestBody(buffer, channel);
                    return;

                case MSG:
                    LOGGER.trace("msg={} msgLength={} readableBytes={}", ((char) decoder.msgType()),
                            decoder.payloadLength(), buffer.readableBytes());

                    if (ignoreTillSync && decoder.msgType() != 'S')
                    {
                        buffer.skipBytes(decoder.payloadLength());
                        return;
                    }
                    dispatchMessage(buffer, channel);
                    return;
                default:
                    throw new IllegalStateException("Illegal state: " + decoder.state());
            }
        }

        private void dispatchMessage(ByteBuf buffer, DelayableWriteChannel channel) throws Exception
        {
            switch (decoder.msgType())
            {
                case 'Q': // Query (simple)
                    LOGGER.trace("Dispatching simple query");
                    handleSimpleQuery(buffer, channel);
                    return;
                case 'P':
                    LOGGER.trace("Dispatching parse");
                    handleParseMessage(buffer, channel);
                    return;
                case 'p':
                    LOGGER.trace("Dispatching password");
                    handlePassword(buffer, channel, decoder.payloadLength());
                    return;
                case 'B':
                    LOGGER.trace("Dispatching bind");
                    handleBindMessage(buffer, channel);
                    return;
                case 'D':
                    LOGGER.trace("Dispatching describe");
                    handleDescribeMessage(buffer, channel);
                    return;
                case 'E':
                    LOGGER.trace("Dispatching execute");
                    handleExecute(buffer, channel);
                    return;
                case 'H':
                    LOGGER.trace("Dispatching flush");
                    handleFlush(channel);
                    return;
                case 'S':
                    LOGGER.trace("Dispatching sync");
                    handleSync(channel);
                    return;
                case 'C':
                    LOGGER.trace("Dispatching close ");
                    handleClose(buffer, channel);
                    return;
                case 'X': // Terminate (called when jdbc connection is closed)
                    LOGGER.trace("Dispatching close session");
                    closeSession();
                    channel.close();
                    return;
                default:
                   /* Messages.sendErrorResponse(
                        channel,
                        session == null
                            ? AccessControl.DISABLED
                            : getAccessControl.apply(session.sessionSettings()),
                        new UnsupportedOperationException("Unsupported messageType: " + decoder.msgType()));*/
                    Messages.sendErrorResponse(channel,
                            new UnsupportedOperationException("Unsupported messageType: " + decoder.msgType()));
            }
        }

        private void closeSession()
        {
            if (session != null)
            {
                session.close();
                session = null;
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
        {
            if (cause instanceof SocketException && cause.getMessage().equals("Connection reset"))
            {
                LOGGER.info("Connection reset. Client likely terminated connection");
                closeSession();
            }
            else
            {
                LOGGER.error("Uncaught exception: ", cause);
            }
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception
        {
            LOGGER.trace("channelDisconnected");
            channel = null;
            closeSession();
            super.channelUnregistered(ctx);
        }
    }

    private void handleStartupBody(ByteBuf buffer, Channel channel)
    {
        properties = readStartupMessage(buffer);
        initAuthentication(channel);
    }

    public static InetAddress getRemoteAddress(Channel channel)
    {
        if (channel.remoteAddress() instanceof InetSocketAddress)
        {
            return ((InetSocketAddress) channel.remoteAddress()).getAddress();
        }
        // In certain cases the channel is an EmbeddedChannel (e.g. in tests)
        // and this type of channel has an EmbeddedSocketAddress instance as remoteAddress
        // which does not have an address.
        // An embedded socket address is handled like a local connection via loopback.
        return InetAddresses.forString("127.0.0.1");
    }

    public static SSLSession getSession(Channel channel)
    {
        SslHandler sslHandler = channel.pipeline().get(SslHandler.class);
        if (sslHandler != null)
        {
            return sslHandler.engine().getSession();
        }
        return null;
    }

    private void initAuthentication(Channel channel)
    {
        String userName = properties.getProperty("user");
        InetAddress address = getRemoteAddress(channel);

        SSLSession sslSession = getSession(channel);
        ConnectionProperties connProperties = new ConnectionProperties(address, sslSession);

        AuthenticationMethod authMethod = authService.resolveAuthenticationType(userName,
                connProperties);
        if (authMethod == null)
        {
            String errorMessage = String.format(
                    Locale.ENGLISH,
                    "No valid auth.host_based entry found for host \"%s\", user \"%s\". Did you enable TLS in your client?",
                    address.getHostAddress(), userName
            );
            Messages.sendAuthenticationError(channel, errorMessage);
        }
        else
        {
            authContext = new AuthenticationContext(authMethod, connProperties, userName, LOGGER);
            if (authMethod.name() == AuthenticationMethodType.PASSWORD)
            {
                Messages.sendAuthenticationCleartextPassword(channel);
                return;
            }
            if (authMethod.name() == AuthenticationMethodType.GSS)
            {
                if (gssConfig == null)
                {
                    Messages.sendAuthenticationError(channel, "GSS Auth not configured in this server");
                    return;
                }
                Messages.sendAuthenticationKerberos(channel);
                return;
            }
            finishAuthentication(channel);
        }
    }

    private void finishAuthentication(Channel channel)
    {
        assert authContext != null : "finishAuthentication() requires an authContext instance";
        try
        {
            Identity authenticatedUser = authContext.authenticate();
            handleAuthSuccess(channel, authenticatedUser);
        }
        catch (Exception e)
        {
            Messages.sendAuthenticationError(channel, e.getMessage());
            LOGGER.error("Auth Error", e);
        }
        finally
        {
            authContext.close();
            authContext = null;
        }
    }

    private void finishAuthentication(Channel channel, Subject delegSubject)
    {
        assert authContext != null : "finishAuthentication() requires an authContext instance";
        try
        {
            Identity authenticatedUser = KerberosIdentityProvider.getIdentityForSubject(delegSubject);
            handleAuthSuccess(channel, authenticatedUser);
        }
        catch (Exception e)
        {
            Messages.sendAuthenticationError(channel, e.getMessage());
            LOGGER.error("Auth Error", e);
        }
        finally
        {
            authContext.close();
            authContext = null;
        }
    }

    private void handleAuthSuccess(Channel channel, Identity authenticatedUser) throws Exception
    {
        String database = properties.getProperty("database");
        session = sessions.createSession(database, authenticatedUser);
        MDC.put("user", authenticatedUser.getName());
        Messages.sendAuthenticationOK(channel)
                .addListener(f -> sendParams(channel))
                //.addListener(f -> Messages.sendKeyData(channel, session.id(), session.secret()))
                .addListener(f ->
                {
                    Messages.sendReadyForQuery(channel);
                /*if (properties.containsKey("CrateDBTransport")) {
                    switchToTransportProtocol(channel);
                }*/
                });
    }

/*    private void switchToTransportProtocol(Channel channel) {
        var pipeline = channel.pipeline();
        pipeline.remove("frame-decoder");
        pipeline.remove("handler");

        // SSL is already done via PostgreSQL handshake/auth
        addTransportHandler.accept(pipeline);
    }*/

    private void sendParams(Channel channel)
    {
        /* Messages.sendParameterStatus(channel, "crate_version", Version.CURRENT.externalNumber());
         */
        Messages.sendParameterStatus(channel, "server_version", PG_SERVER_VERSION);
        Messages.sendParameterStatus(channel, "server_encoding", "UTF8");
        Messages.sendParameterStatus(channel, "client_encoding", "UTF8");
        Messages.sendParameterStatus(channel, "datestyle", "ISO");
        Messages.sendParameterStatus(channel, "TimeZone", "UTC");
        Messages.sendParameterStatus(channel, "integer_datetimes", "on");
    }

    /**
     * Flush Message | 'H' | int32 len
     * <p>
     * Flush forces the backend to deliver any data pending in it's output buffers.
     */
    private void handleFlush(Channel channel)
    {
        try
        {
/*            // If we have deferred any executions we need to trigger a sync now because the client is expecting data
            // (That we've been holding back, as we don't eager react to `execute` requests. (We do that to optimize batch inserts))
            // The sync will also trigger a flush eventually if there are deferred executions.
            if (session.hasDeferredExecutions()) {
                session.flush();
            } else {
                channel.flush();
            }*/

            //since we don't handle buffering we should flash right away
            //TODO understand when this is called
            session.sync();
        }
        catch (Throwable t)
        {
            //Messages.sendErrorResponse(channel, getAccessControl.apply(session.sessionSettings()), t);
            Messages.sendErrorResponse(channel, t);

        }
    }

    /**
     * Parse Message header: | 'P' | int32 len
     * <p>
     * body: | string statementName | string query | int16 numParamTypes | foreach param: | int32
     * type_oid (zero = unspecified)
     */
    private void handleParseMessage(ByteBuf buffer, final Channel channel)
    {
        String statementName = readCString(buffer);
        final String query = readCString(buffer);
        short numParams = buffer.readShort();
        List<Integer> paramTypes = new ArrayList<>(numParams);
        for (int i = 0; i < numParams; i++)
        {
            int oid = buffer.readInt();
            int dataType = PGTypes.fromOID(oid);
 /*           if (dataType == null) {
                throw new IllegalArgumentException(
                    String.format(Locale.ENGLISH, "Can't map PGType with oid=%d to Crate type", oid));
            }*/
            paramTypes.add(dataType);
        }
        session.parse(statementName, query, paramTypes);
        Messages.sendParseComplete(channel);
    }

    private void handlePassword(ByteBuf buffer, final Channel channel, int payloadLength)
    {
        switch (authContext.getAuthenticationMethodType())
        {
            case GSS:
                byte[] inputToken = readByteArray(buffer, payloadLength);
                if (inputToken == null)
                {
                    Messages.sendErrorResponse(channel, new IllegalStateException("GSS Token cannot be empty"));
                    return;
                }
                Subject serverSubject = SubjectTools.getSubjectFromKeytab(gssConfig.getKerberosKeytabFile(), gssConfig.getKerberosUserPrincipal(), false);
                GSSManager manager = GSSManager.getInstance();

                try
                {
                    GSSCredential gssCredential = Subject.doAs(serverSubject, new AcceptorCreator(manager, gssConfig.getKerberosUserPrincipal()));
                    GSSContext gssContext = manager.createContext(gssCredential);
                    gssContext.requestCredDeleg(true);
                    gssContext.requestMutualAuth(true);
                    byte[] outputToken;
                    if (!gssContext.isEstablished())
                    {
                        outputToken = gssContext.acceptSecContext(inputToken, 0, inputToken.length);
                        if (outputToken != null)
                        {
                            Messages.sendGssOutToken(channel, outputToken);
                        }
                    }

                    Subject delegatedSubject = GSSUtil.createSubject(gssContext.getSrcName(), gssContext.getDelegCred());
                    finishAuthentication(channel, delegatedSubject);
                }
                catch (PrivilegedActionException | GSSException e)
                {
                    throw new RuntimeException(e);
                }
                break;
            case PASSWORD:
            default:
                char[] passwd = readCharArray(buffer);
                if (passwd != null)
                {
                    authContext.setSecurePassword(passwd);
                }
                finishAuthentication(channel);
        }
    }

    /**
     * Bind Message Header: | 'B' | int32 len
     * <p>
     * Body:
     * <pre>
     * | string portalName | string statementName
     * | int16 numFormatCodes
     *      foreach
     *      | int16 formatCode
     * | int16 numParams
     *      foreach
     *      | int32 valueLength
     *      | byteN value
     * | int16 numResultColumnFormatCodes
     *      foreach
     *      | int16 formatCode
     * </pre>
     */
    private void handleBindMessage(ByteBuf buffer, Channel channel)
    {
        String portalName = readCString(buffer);
        String statementName = readCString(buffer);

        FormatCodes.FormatCode[] formatCodes = FormatCodes.fromBuffer(buffer);

        short numParams = buffer.readShort();
        List<Object> params = createList(numParams);
        for (int i = 0; i < numParams; i++)
        {
            int valueLength = buffer.readInt();
            if (valueLength == -1)
            {
                params.add(null);
            }
            else
            {
                int paramType = session.getParamType(statementName, i);
                PGType pgType = PGTypes.get(paramType, 0);
                FormatCodes.FormatCode formatCode = getFormatCode(formatCodes, i);
                switch (formatCode)
                {
                    case TEXT:
                        params.add(pgType.readTextValue(buffer, valueLength));
                        break;

                    case BINARY:
                        params.add(pgType.readBinaryValue(buffer, valueLength));
                        break;

                    default:
                       /* Messages.sendErrorResponse(
                            channel,
                            getAccessControl.apply(session.sessionSettings()),
                            new UnsupportedOperationException(String.format(
                                Locale.ENGLISH,
                                "Unsupported format code '%d' for param '%s'",
                                formatCode.ordinal(),
                                paramType.getName())
                            )
                        );*/
                        Messages.sendErrorResponse(
                                channel,
                                new UnsupportedOperationException(String.format(
                                        Locale.ENGLISH,
                                        "Unsupported format code '%d' for param '%s'",
                                        formatCode.ordinal(),
                                        paramType)
                                )
                        );
                        return;
                }
            }
        }

        FormatCodes.FormatCode[] resultFormatCodes = FormatCodes.fromBuffer(buffer);
        session.bind(portalName, statementName, params, resultFormatCodes);
        Messages.sendBindComplete(channel);
    }

    private <T> List<T> createList(short size)
    {
        return size == 0 ? Collections.emptyList() : new ArrayList<T>(size);
    }


    /**
     * Describe Message Header: | 'D' | int32 len
     * <p>
     * Body: | 'S' = prepared statement or 'P' = portal | string nameOfPortalOrStatement
     */
    private void handleDescribeMessage(ByteBuf buffer, Channel channel) throws Exception
    {
        OpenTelemetry.TOTAL_METADATA.add(1);
        OpenTelemetry.ACTIVE_METADATA.add(1);
        long startTime = System.currentTimeMillis();

        Tracer tracer = OpenTelemetry.getTracer();
        Span span = tracer.spanBuilder("PostgresWireProtocol.handleDescribeMessage").startSpan();
        try (Scope scope = span.makeCurrent())
        {
            byte type = buffer.readByte();
            String portalOrStatement = readCString(buffer);
            DescribeResult describeResult = session.describe((char) type, portalOrStatement);
            PostgresResultSetMetaData fields = describeResult.getFields();
            if (type == 'S')
            {
                ParameterMetaData parameters = describeResult.getParameters();
                Messages.sendParameterDescription(channel, parameters);
            }
            if (fields == null)
            {
                Messages.sendNoData(channel);
            }
            else
            {
                FormatCodes.FormatCode[] resultFormatCodes =
                        type == 'P' ? session.getResultFormatCodes(portalOrStatement) : null;
                Messages.sendRowDescription(channel, fields, resultFormatCodes);
            }
            OpenTelemetry.TOTAL_SUCCESS_METADATA.add(1);
            OpenTelemetry.METADATA_DURATION.record(System.currentTimeMillis() - startTime);
        }
        catch (Exception e)
        {
            span.recordException(e);
            OpenTelemetry.TOTAL_FAILURE_METADATA.add(1);
            throw e;
        }
        finally
        {
            OpenTelemetry.ACTIVE_METADATA.add(-1);
        }
    }

    /**
     * Execute Message Header: | 'E' | int32 len
     * <p>
     * Body: | string portalName | int32 maxRows (0 = unlimited)
     */
    private void handleExecute(ByteBuf buffer, DelayableWriteChannel channel)
    {
        Tracer tracer = OpenTelemetry.getTracer();
        Span span = tracer.spanBuilder("PostgresWireProtocol.handleExecute").startSpan();
        try (Scope scope = span.makeCurrent())
        {
            String portalName = readCString(buffer);
            int maxRows = buffer.readInt();
            String query = session.getQuery(portalName);
            span.setAttribute("portalName", portalName);
            span.setAttribute("query", query);
 /*       if (query.isEmpty()) {
            // remove portal so that it doesn't stick around and no attempt to batch it with follow up statement is made
            session.close((byte) 'P', portalName);
            Messages.sendEmptyQueryResponse(channel);
            return;
        }*/
      /*  List<? extends DataType> outputTypes = session.getOutputTypes(portalName);

        // .execute is going async and may execute the query in another thread-pool.
        // The results are later sent to the clients via the `ResultReceiver` created
        // above, The `channel.write` calls - which the `ResultReceiver` makes - may
        // happen in a thread which is *not* a netty thread.
        // If that is the case, netty schedules the writes instead of running them
        // immediately. A consequence of that is that *this* thread can continue
        // processing other messages from the client, and if this thread then sends messages to the
        // client, these are sent immediately, overtaking the result messages of the
        // execute that is triggered here.
        //
        // This would lead to out-of-order messages. For example, we could send a
        // `parseComplete` before the `commandComplete` of the previous statement has
        // been transmitted.
        //
        // To ensure clients receive messages in the correct order we delay all writes
        // The "finish" logic of the ResultReceivers writes out all pending writes/unblocks the channel

        DelayedWrites delayedWrites = channel.delayWrites();
        ResultReceiver<?> resultReceiver;
        if (outputTypes == null) {
            // this is a DML query
            maxRows = 0;
            resultReceiver = new RowCountReceiver(
                query,
                channel,
                delayedWrites,
                getAccessControl.apply(session.sessionSettings())
            );
        } else {
            // query with resultSet
            resultReceiver = new ResultSetReceiver(
                query,
                channel,
                delayedWrites,
                session.transactionState(),
                getAccessControl.apply(session.sessionSettings()),
                Lists2.map(outputTypes, PGTypes::get),
                session.getResultFormatCodes(portalName)
            );
        }
        session.execute(portalName, maxRows, resultReceiver);*/


            DelayableWriteChannel.DelayedWrites delayedWrites = channel.delayWrites();
            ResultSetReceiver resultReceiver = new ResultSetReceiver(query, channel, delayedWrites, false, null);
            session.execute(portalName, maxRows, resultReceiver);
        }
        catch (Exception e)
        {
            span.recordException(e);
            throw ExceptionUtil.wrapException(e);
        }
        finally
        {
            span.end();
        }
    }


    private void handleSync(DelayableWriteChannel channel)
    {
        if (ignoreTillSync)
        {
            ignoreTillSync = false;
   /*         // If an error happens all sub-sequent messages can be ignored until the client sends a sync message
            // We need to discard any deferred executions to make sure that the *next* sync isn't executing
            // something we had previously deferred.
            // E.g. JDBC client:
            //  1) `addBatch` -> success (results in bind+execute -> we defer execution)
            //  2) `addBatch` -> failure (ignoreTillSync=true; we stop after bind, no execute, etc..)
            //  3) `sync`     -> sendReadyForQuery (this if branch)
            //  4) p, b, e    -> We've a new query deferred.
            //  5) `sync`     -> We must execute the query from 4, but not 1)
            //session.resetDeferredExecutions();
            channel.writePendingMessages();*/
            session.clearState();
            Messages.sendReadyForQuery(channel);
            return;
        }
        try
        {
            ReadyForQueryCallback readyForQueryCallback = new ReadyForQueryCallback(channel);
            session.sync().whenComplete(readyForQueryCallback);
        }
        catch (Throwable t)
        {
            channel.discardDelayedWrites();
            Messages.sendErrorResponse(channel, t);
            Messages.sendReadyForQuery(channel);
        }
    }

    /**
     * | 'C' | int32 len | byte portalOrStatement | string portalOrStatementName |
     */
    private void handleClose(ByteBuf buffer, Channel channel)
    {
        byte b = buffer.readByte();
        String portalOrStatementName = readCString(buffer);
        session.close((char) b, portalOrStatementName);
        Messages.sendCloseComplete(channel);
    }

    /*    void handleSimpleQuery(ByteBuf buffer, final DelayableWriteChannel channel) {
            String queryString = readCString(buffer);
            assert queryString != null : "query must not be nulL";

            if (queryString.isEmpty() || ";".equals(queryString)) {
                Messages.sendEmptyQueryResponse(channel);
                sendReadyForQuery(channel, TransactionState.IDLE);
                return;
            }

            List<Statement> statements;
            try {
                statements = SqlParser.createStatements(queryString);
            } catch (Exception ex) {
                Messages.sendErrorResponse(channel, getAccessControl.apply(session.sessionSettings()), ex);
                sendReadyForQuery(channel, TransactionState.IDLE);
                return;
            }
            CompletableFuture<?> composedFuture = CompletableFuture.completedFuture(null);
            for (var statement : statements) {
                composedFuture = composedFuture.thenCompose(result -> handleSingleQuery(statement, channel));
            }
            composedFuture.whenComplete(new ReadyForQueryCallback(channel, TransactionState.IDLE));
        }*/
    void handleSimpleQuery(ByteBuf buffer, final DelayableWriteChannel channel)
    {
        Tracer tracer = OpenTelemetry.getTracer();
        Span span = tracer.spanBuilder("PostgresWireProtocol.handleSimpleQuery").startSpan();
        try (Scope scope = span.makeCurrent())
        {
            String queryString = readCString(buffer);
            assert queryString != null : "query must not be nulL";
            span.setAttribute("query", queryString);

            if (queryString.isEmpty() || ";".equals(queryString))
            {
                Messages.sendEmptyQueryResponse(channel);
                Messages.sendReadyForQuery(channel);
                return;
            }

            List<String> queries = QueryStringSplitter.splitQuery(queryString);
            CompletableFuture<?> composedFuture = CompletableFuture.completedFuture(null);
            for (String query : queries)
            {
                composedFuture = composedFuture.thenCompose(result -> handleSingleQuery(query, channel));
            }
            composedFuture.whenComplete(new ReadyForQueryCallback(channel));
        }
        catch (Exception e)
        {
            span.recordException(e);
            throw e;
        }
        finally
        {
            span.end();
        }
    }


    private CompletableFuture<?> handleSingleQuery(String query, DelayableWriteChannel channel)
    {

        Tracer tracer = OpenTelemetry.getTracer();
        Span span = tracer.spanBuilder("PostgresWireProtocol.handleSimpleQuery").startSpan();
        try (Scope scope = span.makeCurrent())
        {
            CompletableFuture<?> result = new CompletableFuture<>();

            if (query.isEmpty() || ";".equals(query))
            {
                Messages.sendEmptyQueryResponse(channel);
                result.complete(null);
                return result;
            }
            try
            {
                DelayableWriteChannel.DelayedWrites delayedWrites = channel.delayWrites();
                ResultSetReceiver resultReceiver = new ResultSetReceiver(query, channel, delayedWrites, true, null);
                session.executeSimple(query, resultReceiver);
                return session.sync();
            }
            catch (Throwable t)
            {
                //TODO need to understand this usecase
                LOGGER.warn("Error processing single query", t);
                session.clearState();
                Messages.sendErrorResponse(channel, t);
                result.completeExceptionally(t);
                return result;
            }
        }
        finally
        {
            span.end();
        }

    }


    private void handleCancelRequestBody(ByteBuf buffer, Channel channel)
    {
   /*     var keyData = KeyData.of(buffer);

        sessions.cancel(keyData);*/

        // Cancel request is sent by the client over a new connection.
        // This closes the new connection, not the one running the query.
        handler.closeSession();
        channel.close();
    }

    private static class AcceptorCreator implements PrivilegedExceptionAction<GSSCredential>
    {
        private final GSSManager manager;
        private final String accountPrincipal;

        public AcceptorCreator(GSSManager manager, String accountPrincipal)
        {
            this.manager = manager;
            this.accountPrincipal = accountPrincipal;
        }

        @Override
        public GSSCredential run() throws Exception
        {
            final GSSName gssName = manager.createName(this.accountPrincipal, GSSName.NT_USER_NAME);
            return manager
                    .createCredential(gssName, GSSCredential.DEFAULT_LIFETIME, new Oid[] {
                            new Oid("1.2.840.113554.1.2.2"),    // Kerberos v5
                            new Oid("1.3.6.1.5.5.2")            // SPNEGO
                    }, GSSCredential.ACCEPT_ONLY);
        }
    }
}
