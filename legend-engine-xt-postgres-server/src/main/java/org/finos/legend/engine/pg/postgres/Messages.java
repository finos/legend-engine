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

/*import io.crate.auth.AccessControl;
//import io.crate.data.Row;
import io.crate.exceptions.SQLExceptions;
import io.crate.expression.symbol.Symbol;
import io.crate.expression.symbol.Symbols;
import io.crate.metadata.Reference;
import io.crate.metadata.RelationInfo;
import io.crate.metadata.pgcatalog.OidHash;
import io.crate.protocols.postgres.types.PGType;
import io.crate.protocols.postgres.types.PGTypes;
import io.crate.types.DataType;*/

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.nio.charset.StandardCharsets;
import java.sql.ParameterMetaData;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.finos.legend.engine.pg.postgres.types.PGType;
import org.finos.legend.engine.pg.postgres.types.PGTypes;
import org.slf4j.Logger;

/**
 * Regular data packet is in the following format:
 * <p>
 * +----------+-----------+----------+ | char tag | int32 len | payload  |
 * +----------+-----------+----------+
 * <p>
 * The tag indicates the message type, the second field is the length of the packet (excluding the
 * tag, but including the length itself)
 * <p>
 * <p>
 * See https://www.postgresql.org/docs/9.2/static/protocol-message-formats.html
 */
public class Messages
{

  //private static final Logger LOGGER = LogManager.getLogger(Messages.class);
  private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Messages.class);

  private static final byte[] METHOD_NAME_CLIENT_AUTH = "ClientAuthentication".getBytes(
      StandardCharsets.UTF_8);

  public static ChannelFuture sendAuthenticationOK(Channel channel)
  {
    ByteBuf buffer = channel.alloc().buffer(9);
    buffer.writeByte('R');
    buffer.writeInt(8); // size excluding char
    buffer.writeInt(0);
    ChannelFuture channelFuture = channel.writeAndFlush(buffer);
    if (LOGGER.isTraceEnabled())
    {
      channelFuture.addListener(
          (ChannelFutureListener) future -> LOGGER.trace("sentAuthenticationOK"));
    }
    return channelFuture;
  }

  /**
   * | 'C' | int32 len | str commandTag
   *
   * @param query    :the query
   * @param rowCount : number of rows in the result set or number of rows affected by the DML
   *                 statement
   */
  static ChannelFuture sendCommandComplete(Channel channel, String query, long rowCount)
  {
    query = query.trim().split(" ", 2)[0].toUpperCase(Locale.ENGLISH);
    String commandTag;
    /*
     * from https://www.postgresql.org/docs/current/static/protocol-message-formats.html:
     *
     * For an INSERT command, the tag is INSERT oid rows, where rows is the number of rows inserted.
     * oid is the object ID of the inserted row if rows is 1 and the target table has OIDs; otherwise oid is 0.
     */
    if ("BEGIN".equals(query))
    {
      commandTag = "BEGIN";
    }
    else if ("INSERT".equals(query))
    {
      commandTag = "INSERT 0 " + rowCount;
    }
    else
    {
      commandTag = query + " " + rowCount;
    }

    byte[] commandTagBytes = commandTag.getBytes(StandardCharsets.UTF_8);
    int length = 4 + commandTagBytes.length + 1;
    ByteBuf buffer = channel.alloc().buffer(length + 1);
    buffer.writeByte('C');
    buffer.writeInt(length);
    writeCString(buffer, commandTagBytes);
    ChannelFuture channelFuture = channel.write(buffer);
    if (LOGGER.isTraceEnabled())
    {
      channelFuture.addListener(
          (ChannelFutureListener) future -> LOGGER.trace("sentCommandComplete"));
    }
    return channelFuture;
  }

  /**
   * ReadyForQuery (B)
   * <p>
   * Byte1('Z') Identifies the message type. ReadyForQuery is sent whenever the backend is ready for
   * a new query cycle.
   * <p>
   * Int32(5) Length of message contents in bytes, including self.
   * <p>
   * Byte1 Current backend transaction status indicator. Possible values are 'I' if idle (not in a
   * transaction block); 'T' if in a transaction block; or 'E' if in a failed transaction block
   * (queries will be rejected until block is ended).
   */
  static ChannelFuture sendReadyForQuery(Channel channel)
  {
    ByteBuf buffer = channel.alloc().buffer(6);
    buffer.writeByte('Z');
    buffer.writeInt(5);
    buffer.writeByte('I');
    ChannelFuture channelFuture = channel.writeAndFlush(buffer);
    if (LOGGER.isTraceEnabled())
    {
      channelFuture.addListener(
          (ChannelFutureListener) future -> LOGGER.trace("sentReadyForQuery"));
    }
    return channelFuture;
  }

  /**
   * | 'S' | int32 len | str name | str value
   * <p>
   * See https://www.postgresql.org/docs/9.2/static/protocol-flow.html#PROTOCOL-ASYNC
   * <p>
   * > At present there is a hard-wired set of parameters for which ParameterStatus will be
   * generated: they are
   * <p>
   * - server_version, - server_encoding, - client_encoding, - application_name, - is_superuser, -
   * session_authorization, - DateStyle, - IntervalStyle, - TimeZone, - integer_datetimes, -
   * standard_conforming_string
   */
  static void sendParameterStatus(Channel channel, final String name, final String value)
  {
    byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
    byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);

    int length = 4 + nameBytes.length + 1 + valueBytes.length + 1;
    ByteBuf buffer = channel.alloc().buffer(length + 1);
    buffer.writeByte('S');
    buffer.writeInt(length);
    writeCString(buffer, nameBytes);
    writeCString(buffer, valueBytes);
    ChannelFuture channelFuture = channel.write(buffer);
    if (LOGGER.isTraceEnabled())
    {
      channelFuture.addListener(
          (ChannelFutureListener) future -> LOGGER.trace("sentParameterStatus {}={}", name, value));
    }
  }

  static void sendAuthenticationError(Channel channel, String message)
  {
    LOGGER.warn(message);
    byte[] msg = message.getBytes(StandardCharsets.UTF_8);
    byte[] errorCode = PGErrorStatus.INVALID_AUTHORIZATION_SPECIFICATION.code()
        .getBytes(StandardCharsets.UTF_8);

    sendErrorResponse(channel, message, msg, PGError.SEVERITY_FATAL, null, null,
        METHOD_NAME_CLIENT_AUTH, errorCode);
  }

  static ChannelFuture sendErrorResponse(Channel channel, /*AccessControl accessControl,*/
      Throwable throwable)
  {
    //final PGError error = PGError.fromThrowable(SQLExceptions.prepareForClientTransmission(accessControl, throwable));
    final PGError error = PGError.fromThrowable(throwable);

    ByteBuf buffer = channel.alloc().buffer();
    buffer.writeByte('E');
    buffer.writeInt(0); // length, updated later

    buffer.writeByte('S');
    writeCString(buffer, PGError.SEVERITY_ERROR);

    buffer.writeByte('M');
    writeCString(buffer, error.message().getBytes(StandardCharsets.UTF_8));

    buffer.writeByte('C');
    writeCString(buffer, error.status().code().getBytes(StandardCharsets.UTF_8));

    StackTraceElement[] stackTrace = error.throwable().getStackTrace();
    if (stackTrace.length > 0)
    {
      StackTraceElement first = stackTrace[0];
      String fileName = first.getFileName();
      if (fileName != null)
      {
        buffer.writeByte('F');
        writeCString(buffer, fileName.getBytes(StandardCharsets.UTF_8));
      }
      String methodName = first.getMethodName();
      if (methodName != null)
      {
        buffer.writeByte('R');
        writeCString(buffer, methodName.getBytes(StandardCharsets.UTF_8));
      }

      int lineNumber = first.getLineNumber();
      if (lineNumber >= 0)
      {
        buffer.writeByte('L');
        writeCString(buffer, String.valueOf(lineNumber).getBytes(StandardCharsets.UTF_8));
      }

      buffer.writeByte('W');
      StringBuilder sb = new StringBuilder();
      int cap = Math.min(stackTrace.length, 20);
      for (int i = 0; i < cap; i++)
      {
        StackTraceElement stackTraceElement = stackTrace[i];
        sb.append(stackTraceElement.toString());
        sb.append("\n");
      }
      writeCString(buffer, sb.toString().getBytes(StandardCharsets.UTF_8));
    }
    buffer.writeByte(0);
    buffer.setInt(1, buffer.writerIndex() - 1); // exclude msg type from length
    ChannelFuture channelFuture = channel.writeAndFlush(buffer);
    if (LOGGER.isTraceEnabled())
    {
      channelFuture.addListener(f -> LOGGER.trace("sentErrorResponse msg={}", error.message()));
    }
    return channelFuture;
  }

  /**
   * 'E' | int32 len | char code | str value | \0 | char code | str value | \0 | ... | \0
   * <p>
   * char code / str value -> key-value fields example error fields are: message, detail, hint,
   * error position
   * <p>
   * See https://www.postgresql.org/docs/9.2/static/protocol-error-fields.html for a list of error
   * codes
   */
  private static ChannelFuture sendErrorResponse(Channel channel,
      String message,
      byte[] msg,
      byte[] severity,
      byte[] lineNumber,
      byte[] fileName,
      byte[] methodName,
      byte[] errorCode)
  {
    int length = 4 +
        1 + (severity.length + 1) +
        1 + (msg.length + 1) +
        1 + (errorCode.length + 1) +
        (fileName != null ? 1 + (fileName.length + 1) : 0) +
        (lineNumber != null ? 1 + (lineNumber.length + 1) : 0) +
        (methodName != null ? 1 + (methodName.length + 1) : 0) +
        1;
    ByteBuf buffer = channel.alloc().buffer(length + 1);
    buffer.writeByte('E');
    buffer.writeInt(length);
    buffer.writeByte('S');
    writeCString(buffer, severity);
    buffer.writeByte('M');
    writeCString(buffer, msg);
    buffer.writeByte(('C'));
    writeCString(buffer, errorCode);
    if (fileName != null)
    {
      buffer.writeByte('F');
      writeCString(buffer, fileName);
    }
    if (lineNumber != null)
    {
      buffer.writeByte('L');
      writeCString(buffer, lineNumber);
    }
    if (methodName != null)
    {
      buffer.writeByte('R');
      writeCString(buffer, methodName);
    }
    buffer.writeByte(0);
    ChannelFuture channelFuture = channel.writeAndFlush(buffer);
    if (LOGGER.isTraceEnabled())
    {
      channelFuture.addListener(
          (ChannelFutureListener) future -> LOGGER.trace("sentErrorResponse msg={}", message));
    }
    return channelFuture;
  }

  /**
   * Byte1('D') Identifies the message as a data row.
   * <p>
   * Int32 Length of message contents in bytes, including self.
   * <p>
   * Int16 The number of column values that follow (possibly zero).
   * <p>
   * Next, the following pair of fields appear for each column:
   * <p>
   * Int32 The length of the column value, in bytes (this count does not include itself). Can be
   * zero. As a special case, -1 indicates a NULL column value. No value bytes follow in the NULL
   * case.
   * <p>
   * ByteN The value of the column, in the format indicated by the associated format code. n is the
   * above length.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  static void sendDataRow(Channel channel, PostgresResultSet rs, List<PGType> columnTypes,
      @Nullable FormatCodes.FormatCode[] formatCodes) throws Exception
  {
    int length = 4 + 2;
    assert columnTypes.size() == rs.getMetaData().getColumnCount()
        : "Number of columns in the row must match number of columnTypes. Row: " + rs + " types: "
        + columnTypes;

    ByteBuf buffer = channel.alloc().buffer();
    buffer.writeByte('D');
    buffer.writeInt(0); // will be set at the end
    buffer.writeShort(columnTypes.size());

    for (int i = 0; i < columnTypes.size(); i++)
    {
      PGType pgType = columnTypes.get(i);
      Object value;
      try
      {
        value = rs.getObject(i + 1);
      }
      catch (Exception e)
      {
        buffer.release();
        throw e;
      }
      if (value == null)
      {
        buffer.writeInt(-1);
        length += 4;
      }
      else
      {
        FormatCodes.FormatCode formatCode = FormatCodes.getFormatCode(formatCodes, i);
        switch (formatCode)
        {
          case TEXT:
            length += pgType.writeAsText(buffer, value);
            break;
          case BINARY:
            length += pgType.writeAsBinary(buffer, value);
            break;

          default:
            buffer.release();
            throw new AssertionError("Unrecognized formatCode: " + formatCode);
        }
      }
    }

    buffer.setInt(1, length);
    channel.writeAndFlush(buffer);
  }

  static void writeCString(ByteBuf buffer, byte[] valBytes)
  {
    buffer.writeBytes(valBytes);
    buffer.writeByte(0);
  }

  /**
   * ParameterDescription (B)
   * <p>
   * Byte1('t')
   * <p>
   * Identifies the message as a parameter description. Int32
   * <p>
   * Length of message contents in bytes, including self. Int16
   * <p>
   * The number of parameters used by the statement (can be zero).
   * <p>
   * Then, for each parameter, there is the following:
   * <p>
   * Int32
   * <p>
   * Specifies the object ID of the parameter data type.
   *
   * @param channel    The channel to write the parameter description to.
   * @param parameters A {@link SortedSet} containing the parameters from index 1 upwards.
   */
  static void sendParameterDescription(Channel channel, ParameterMetaData parameters)
  {
    throw new UnsupportedOperationException("Not supported feature");
      /*  final int messageByteSize = 4 + 2 + parameters.length * 4;
        ByteBuf buffer = channel.alloc().buffer(messageByteSize);
        buffer.writeByte('t');
        buffer.writeInt(messageByteSize);
        if (parameters.length > Short.MAX_VALUE) {
            buffer.release();
            throw new IllegalArgumentException("Too many parameters. Max supported: " + Short.MAX_VALUE);
        }
        buffer.writeShort(parameters.length);
        for (DataType dataType : parameters) {
            int pgTypeId = PGTypes.get(dataType).oid();
            buffer.writeInt(pgTypeId);
        }
        channel.write(buffer);*/
  }

 /*   private static boolean isRefWithPosition(Symbol symbol) {
        return symbol instanceof Reference ref && ref.position() != 0;
    }
*/

  /**
   * RowDescription (B)
   * <p>
   * | 'T' | int32 len | int16 numCols
   * <p>
   * For each field:
   * <p>
   * | string name | int32 table_oid | int16 attr_num | int32 oid | int16 typlen | int32
   * type_modifier | int16 format_code
   * <p>
   * See https://www.postgresql.org/docs/current/static/protocol-message-formats.html
   */
  static void sendRowDescription(Channel channel,
      PostgresResultSetMetaData resultSetMetaData,
      @Nullable FormatCodes.FormatCode[] formatCodes) throws Exception
  {
    int length = 4 + 2;
    int columnSize = 4 + 2 + 4 + 2 + 4 + 2;
    ByteBuf buffer = channel.alloc().buffer(
        length + (resultSetMetaData.getColumnCount() * (10
            + columnSize))); // use 10 as an estimate for columnName length

    buffer.writeByte('T');
    buffer.writeInt(0); // will be set at the end
    buffer.writeShort(resultSetMetaData.getColumnCount());

    int tableOid = 0;
       /* if (relation != null && columns.stream().allMatch(Messages::isRefWithPosition)) {
            tableOid = OidHash.relationOid(relation);
        }*/
    //int idx = 0;
    for (int idx = 0; idx < resultSetMetaData.getColumnCount(); idx++)
    {
      byte[] nameBytes = resultSetMetaData.getColumnName(idx + 1).getBytes(StandardCharsets.UTF_8);
      length += nameBytes.length + 1;
      length += columnSize;

      writeCString(buffer, nameBytes);
      buffer.writeInt(tableOid);  //table_oid
      buffer.writeShort(idx);    //attr num

      // attr_num
       /*     if (column instanceof Reference ref) {
                int position = ref.position();
                buffer.writeShort(position);
            } else {
                buffer.writeShort(0);
            }*/

      PGType<?> pgType = PGTypes.get(resultSetMetaData.getColumnType(idx + 1),
          resultSetMetaData.getScale(idx + 1));
      buffer.writeInt(pgType.oid());
      buffer.writeShort(pgType.typeLen());
      buffer.writeInt(pgType.typeMod());
      buffer.writeShort(FormatCodes.getFormatCode(formatCodes, idx).ordinal());
    }

    buffer.setInt(1, length);
    ChannelFuture channelFuture = channel.write(buffer);
    if (LOGGER.isTraceEnabled())
    {
      channelFuture.addListener(
          (ChannelFutureListener) future -> LOGGER.trace("sentRowDescription"));
    }
  }

  /**
   * ParseComplete | '1' | int32 len |
   */
  static void sendParseComplete(Channel channel)
  {
    sendShortMsg(channel, '1', "sentParseComplete");
  }

  /**
   * BindComplete | '2' | int32 len |
   */
  static void sendBindComplete(Channel channel)
  {
    sendShortMsg(channel, '2', "sentBindComplete");
  }

  /**
   * EmptyQueryResponse | 'I' | int32 len |
   */
  static void sendEmptyQueryResponse(Channel channel)
  {
    sendShortMsg(channel, 'I', "sentEmptyQueryResponse");
  }

  /**
   * NoData | 'n' | int32 len |
   */
  static void sendNoData(Channel channel)
  {
    sendShortMsg(channel, 'n', "sentNoData");
  }

  /**
   * Send a message that just contains the msgType and the msg length
   */
  private static void sendShortMsg(Channel channel, char msgType, final String traceLogMsg)
  {
    ByteBuf buffer = channel.alloc().buffer(5);
    buffer.writeByte(msgType);
    buffer.writeInt(4);

    ChannelFuture channelFuture = channel.write(buffer);
    if (LOGGER.isTraceEnabled())
    {
      channelFuture.addListener((ChannelFutureListener) future -> LOGGER.trace(traceLogMsg));
    }
  }

  static void sendPortalSuspended(Channel channel)
  {
    sendShortMsg(channel, 's', "sentPortalSuspended");
  }

  /**
   * CloseComplete | '3' | int32 len |
   */
  static void sendCloseComplete(Channel channel)
  {
    sendShortMsg(channel, '3', "sentCloseComplete");
  }

  /**
   * AuthenticationCleartextPassword (B)
   * <p>
   * Byte1('R') Identifies the message as an authentication request.
   * <p>
   * Int32(8) Length of message contents in bytes, including self.
   * <p>
   * Int32(3) Specifies that a clear-text password is required.
   *
   * @param channel The channel to write to.
   */
  static void sendAuthenticationCleartextPassword(Channel channel)
  {
    ByteBuf buffer = channel.alloc().buffer(9);
    buffer.writeByte('R');
    buffer.writeInt(8);
    buffer.writeInt(3);
    ChannelFuture channelFuture = channel.writeAndFlush(buffer);
    if (LOGGER.isTraceEnabled())
    {
      channelFuture.addListener(
          (ChannelFutureListener) future -> LOGGER.trace("sentAuthenticationCleartextPassword"));
    }
  }

  /**
   * CancelRequest | 'K' | int32 request code | int32 pid | int32 secret key |
   */
  static void sendKeyData(Channel channel, int pid, int secretKey)
  {
    ByteBuf buffer = channel.alloc().buffer(13);
    buffer.writeByte('K');
    buffer.writeInt(12);
    buffer.writeInt(pid);
    buffer.writeInt(secretKey);
    ChannelFuture channelFuture = channel.writeAndFlush(buffer);
    if (LOGGER.isTraceEnabled())
    {
      channelFuture.addListener((ChannelFutureListener) future -> LOGGER.trace("sentKeyData"));
    }
  }
}
