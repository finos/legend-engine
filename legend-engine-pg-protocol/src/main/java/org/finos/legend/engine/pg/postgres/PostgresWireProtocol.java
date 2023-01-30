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

package org.finos.legend.engine.pg.postgres;

import static org.finos.legend.engine.pg.postgres.FormatCodes.getFormatCode;
import static org.finos.legend.engine.pg.postgres.Messages.sendReadyForQuery;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.InetAddresses;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.ParameterMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSession;
import org.finos.legend.engine.DescribeResult;
import org.finos.legend.engine.Session;
import org.finos.legend.engine.SessionsFactory;
import org.finos.legend.engine.pg.postgres.auth.Authentication;
import org.finos.legend.engine.pg.postgres.auth.AuthenticationMethod;
import org.finos.legend.engine.pg.postgres.auth.Protocol;
import org.finos.legend.engine.pg.postgres.auth.User;
import org.finos.legend.engine.pg.postgres.types.PGType;
import org.finos.legend.engine.pg.postgres.types.PGTypes;
import org.slf4j.Logger;


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

  private static final String PASSWORD_AUTH_NAME = "password";

  public static int SERVER_VERSION_NUM = 100500;
  public static String PG_SERVER_VERSION = "10.5";

  final PgDecoder decoder;
  final MessageHandler handler;
  private final SessionsFactory sessions;
  /* private final Function<CoordinatorSessionSettings, AccessControl> getAccessControl;*/
  private final Authentication authService;
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
      Authentication authService,
      Supplier<SslContext> getSslContext)
  {
    this.sessions = sessions;
    //this.getAccessControl = getAcessControl;
    //this.addTransportHandler = addTransportHandler;
    this.authService = authService;
    this.decoder = new PgDecoder(getSslContext);
    this.handler = new MessageHandler();
  }

  @Nullable
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

  @Nullable
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
        sendReadyForQuery(channel);
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
          Messages.sendErrorResponse(channel, t);
        }
        catch (Throwable ti)
        {
          LOGGER.error("Error trying to send error to client: {}", t, ti);
        }
      }
    }

    private void dispatchState(ByteBuf buffer, DelayableWriteChannel channel) throws SQLException
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

    private void dispatchMessage(ByteBuf buffer, DelayableWriteChannel channel) throws SQLException
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
          handlePassword(buffer, channel);
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
    ConnectionProperties connProperties = new ConnectionProperties(address, Protocol.POSTGRES,
        sslSession);

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
      if (PASSWORD_AUTH_NAME.equals(authMethod.name()))
      {
        Messages.sendAuthenticationCleartextPassword(channel);
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
      User authenticatedUser = authContext.authenticate();
      String database = properties.getProperty("database");
      session = sessions.createSession(database, authenticatedUser);
      Messages.sendAuthenticationOK(channel)
          .addListener(f -> sendParams(channel))
          //.addListener(f -> Messages.sendKeyData(channel, session.id(), session.secret()))
          .addListener(f ->
          {
            sendReadyForQuery(channel);
                    /*if (properties.containsKey("CrateDBTransport")) {
                        switchToTransportProtocol(channel);
                    }*/
          });
    }
    catch (Exception e)
    {
      Messages.sendAuthenticationError(channel, e.getMessage());
    }
    finally
    {
      authContext.close();
      authContext = null;
    }
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

  private void handlePassword(ByteBuf buffer, final Channel channel)
  {
    char[] passwd = readCharArray(buffer);
    if (passwd != null)
    {
      authContext.setSecurePassword(passwd);
    }
    finishAuthentication(channel);
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
  private void handleDescribeMessage(ByteBuf buffer, Channel channel) throws SQLException
  {
    byte type = buffer.readByte();
    String portalOrStatement = readCString(buffer);
    DescribeResult describeResult = session.describe((char) type, portalOrStatement);
    ResultSetMetaData fields = describeResult.getFields();
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
  }

  /**
   * Execute Message Header: | 'E' | int32 len
   * <p>
   * Body: | string portalName | int32 maxRows (0 = unlimited)
   */
  private void handleExecute(ByteBuf buffer, DelayableWriteChannel channel)
  {
    String portalName = readCString(buffer);
    int maxRows = buffer.readInt();
    String query = session.getQuery(portalName);
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

    try
    {
      ResultSet resultSet = session.execute(portalName, maxRows);
      sendResultSet(channel, query, resultSet, false);
    }
    catch (SQLException e)
    {
      throw new RuntimeException(e);
    }
  }

  private void sendResultSet(Channel channel, String query, ResultSet rs, boolean isSimpleQuery)
      throws SQLException
  {
    int rowCount = 0;
    if (rs != null)
    {
      if (isSimpleQuery)
      {
        //Simple query requires to send description
        Messages.sendRowDescription(channel, rs.getMetaData(), null);
      }
      ResultSetMetaData metaData = rs.getMetaData();
      List<PGType> columnTypes = new ArrayList<>(metaData.getColumnCount());
      for (int i = 0; i < metaData.getColumnCount(); i++)
      {
        PGType pgType = PGTypes.get(metaData.getColumnType(i + 1), metaData.getScale(i + 1));
        columnTypes.add(pgType);
      }
      while (rs.next())
      {
        rowCount++;
        Messages.sendDataRow(channel, rs, columnTypes, null);
      }
    }
    LOGGER.info("Query complete with row count {}", rowCount);
    Messages.sendCommandComplete(channel, query, rowCount);
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
      sendReadyForQuery(channel);
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
      //Messages.sendErrorResponse(channel, getAccessControl.apply(session.sessionSettings()), t);
      Messages.sendErrorResponse(channel, t);
      sendReadyForQuery(channel);
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

  @VisibleForTesting
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
  void handleSimpleQuery(ByteBuf buffer, final Channel channel)
  {
    String queryString = readCString(buffer);
    assert queryString != null : "query must not be nulL";

    List<String> queries = QueryStringSplitter.splitQuery(queryString);

    CompletableFuture<?> composedFuture = CompletableFuture.completedFuture(null);
    for (String query : queries)
    {
      composedFuture = composedFuture.thenCompose(result ->
      {
        try
        {
          //TODO NEED A CLEANER SOLUTION
          return handleSingleQuery(query, channel);
        }
        catch (SQLException e)
        {
          throw new RuntimeException(e);
        }
      });
    }
    composedFuture.whenComplete(new ReadyForQueryCallback(channel));
  }


   /* private CompletableFuture<?> handleSingleQuery(String query, DelayableWriteChannel channel) {
        CompletableFuture<?> result = new CompletableFuture<>();

        String query;
        try {
            query = SqlFormatter.formatSql(statement);
        } catch (Exception e) {
            query = statement.toString();
        }
        AccessControl accessControl = getAccessControl.apply(session.sessionSettings());
        try {
            session.analyze("", statement, Collections.emptyList(), query);
            session.bind("", "", Collections.emptyList(), null);
            DescribeResult describeResult = session.describe('P', "");
            List<Symbol> fields = describeResult.getFields();

            if (fields == null) {
                DelayedWrites delayedWrites = channel.delayWrites();
                RowCountReceiver rowCountReceiver = new RowCountReceiver(
                    query,
                    channel,
                    delayedWrites,
                    accessControl
                );
                session.execute("", 0, rowCountReceiver);
            } else {
                Messages.sendRowDescription(channel, fields, null, describeResult.relation());
                DelayedWrites delayedWrites = channel.delayWrites();
                ResultSetReceiver resultSetReceiver = new ResultSetReceiver(
                    query,
                    channel,
                    delayedWrites,
                    TransactionState.IDLE,
                    accessControl,
                    Lists2.map(fields, x -> PGTypes.get(x.valueType())),
                    null
                );
                session.execute("", 0, resultSetReceiver);
            }
            return session.sync();
        } catch (Throwable t) {
            channel.discardDelayedWrites();
            Messages.sendErrorResponse(channel, accessControl, t);
            result.completeExceptionally(t);
            return result;
        }
    }*/

  private CompletableFuture<?> handleSingleQuery(String query, Channel channel) throws SQLException
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
      ResultSet resultSet = session.executeSimple(query);
      sendResultSet(channel, query, resultSet, true);
      result.complete(null);
      return result;
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


  private void handleCancelRequestBody(ByteBuf buffer, Channel channel)
  {
   /*     var keyData = KeyData.of(buffer);

        sessions.cancel(keyData);*/

    // Cancel request is sent by the client over a new connection.
    // This closes the new connection, not the one running the query.
    handler.closeSession();
    channel.close();
  }
}
