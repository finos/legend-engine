// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.finos.legend.engine.pg.postgres.FormatCodes;
import org.finos.legend.engine.pg.postgres.PostgresPreparedStatement;
import org.finos.legend.engine.pg.postgres.PostgresResultSet;
import org.finos.legend.engine.pg.postgres.PostgresStatement;
import org.slf4j.Logger;

public class Session implements AutoCloseable
{

  private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Session.class);


  private final Map<String, Prepared> parsed = new HashMap<>();
  private final Map<String, Portal> portals = new HashMap<>();

  private final SessionHandler sessionHandler;

  public Session(SessionHandler sessionHandler)
  {
    this.sessionHandler = sessionHandler;
  }

  public CompletableFuture<?> sync()
  {
    //TODO do we need to handle batch requests?
    LOGGER.info("Sync");
    CompletableFuture<String> completableFuture = new CompletableFuture<>();
    completableFuture.complete(null);
    return completableFuture;

  }

  public void parse(String statementName, String query, List<Integer> paramTypes)
  {
    if (LOGGER.isDebugEnabled())
    {
      LOGGER.debug("method=parse stmtName={} query={} paramTypes={}", statementName, query,
          paramTypes);
    }

    Prepared p = new Prepared();
    p.name = statementName;
    p.sql = query;
    p.paramType = paramTypes.toArray(new Integer[]{});

    if (query != null && query.length() > 0)
    {
      try
      {
        p.prep = sessionHandler.prepareStatement(query);
      }
      catch (Exception e)
      {
        throw new RuntimeException(e);
      }
    }
    parsed.put(p.name, p);
  }


  public int getParamType(String statementName, int idx)
  {
    Prepared stmt = getSafeStmt(statementName);
    return stmt.paramType[idx];
  }


  public void bind(String portalName, String statementName, List<Object> params,
      @Nullable FormatCodes.FormatCode[] resultFormatCodes)
  {
    if (LOGGER.isDebugEnabled())
    {
      LOGGER.debug("method=bind portalName={} statementName={} params={}", portalName,
          statementName, params);
    }
    Prepared preparedStmt = getSafeStmt(statementName);

    Portal portal = new Portal(portalName, preparedStmt, resultFormatCodes);
    portals.put(portalName, portal);
/*        if (oldPortal != null) {
            // According to the wire protocol spec named portals should be removed explicitly and only
            // unnamed portals are implicitly closed/overridden.
            // We don't comply with the spec because we allow batching of statements, see #execute
            oldPortal.closeActiveConsumer();
        }*/

    PostgresPreparedStatement preparedStatement = portal.prep.prep;
    for (int i = 0; i < params.size(); i++)
    {
      try
      {
        preparedStatement.setObject(i, params.get(i));
      }
      catch (Exception e)
      {
        throw new RuntimeException(e);
      }
    }
  }


  public DescribeResult describe(char type, String portalOrStatement)
  {
    if (LOGGER.isDebugEnabled())
    {
      LOGGER.debug("method=describe type={} portalOrStatement={}", type, portalOrStatement);
    }
    switch (type)
    {
      case 'P':
        Portal portal = getSafePortal(portalOrStatement);
        return describe('S', portal.prep.name);
      case 'S':
        /*
         * describe might be called without prior bind call.
         *
         * If the client uses server-side prepared statements this is usually the case.
         *
         * E.g. the statement is first prepared:
         *
         *      parse stmtName=S_1 query=insert into t (x) values ($1) paramTypes=[integer]
         *      describe type=S portalOrStatement=S_1
         *      sync
         *
         * and then used with different bind calls:
         *
         *      bind portalName= statementName=S_1 params=[0]
         *      describe type=P portalOrStatement=
         *      execute
         *
         *      bind portalName= statementName=S_1 params=[1]
         *      describe type=P portalOrStatement=
         *      execute
         */

        Prepared prepared = parsed.get(portalOrStatement);
        try
        {
          PostgresPreparedStatement preparedStatement = prepared.prep;
          if (portalOrStatement == null)
          {
            return new DescribeResult(null, null);
          }
          else
          {
            return new DescribeResult(preparedStatement.getMetaData(),
                preparedStatement.getParameterMetaData());
          }
        }
        catch (Exception e)
        {
          throw new RuntimeException(e);
        }
      default:
        throw new AssertionError("Unsupported type: " + type);
    }
  }

  @Nullable
  public FormatCodes.FormatCode[] getResultFormatCodes(String portal)
  {
    return getSafePortal(portal).resultColumnFormat;
  }


  public String getQuery(String portalName)
  {
    return getSafePortal(portalName).prep.sql;
  }


  /*TransactionState transactionState();
   */

  public void close()
  {
    clearState();
  }

  public void close(char type, String name)
  {
    if (LOGGER.isDebugEnabled())
    {
      LOGGER.debug("method=close type={} name={}", type, name);
    }

    switch (type)
    {
      case 'P':
      {
        Portal portal = portals.get(name);
        if (portal == null)
        {
          throw new IllegalArgumentException("Portal not found: " + name);
        }
        if (parsed.containsKey(portal.prep.name))
        {
          close('S', portal.prep.name);
        }
        else
        {
          LOGGER.warn("Skipping close of statement {}, from portal, as already closed",
              portal.prep.name);
        }
        portals.remove(portal.name);
        return;
      }
      case 'S':
      {
        Prepared prepared = parsed.remove(name);
        if (prepared == null)
        {
          throw new IllegalArgumentException("Prepared not found: " + name);
        }
        try
        {
          prepared.prep.close();
        }
        catch (Exception e)
        {
          throw new RuntimeException(e);
        }
        parsed.remove(prepared.name);
        return;

               /* if (prepared != null) {
                    Iterator<Map.Entry<String, Portal>> it = portals.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Portal> entry = it.next();
                        Portal portal = entry.getValue();
                        if (portal.prep.equals(prepared)) {
                            try {
                                portal.prep.prep.close();
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                            it.remove();
                        }
                    }
                }
                return;*/
      }
      default:
        throw new IllegalArgumentException("Invalid type: " + type + ", valid types are: [P, S]");
    }
  }

  public PostgresResultSet execute(String portalName, int maxRows)
  {
    Portal portal = getSafePortal(portalName);
    if (LOGGER.isDebugEnabled())
    {
      LOGGER.debug("Executing query {}/{} ", portalName, portal.prep.sql);
    }
    try
    {
      //TODO IDENTIFY THE USE CASE
      PostgresPreparedStatement preparedStatement = portal.prep.prep;
      if (preparedStatement == null)
      {
        return null;
      }
      preparedStatement.setMaxRows(maxRows);
      boolean results = preparedStatement.execute();
      if (!results)
      {
        return null;
      }
      return preparedStatement.getResultSet();
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  public PostgresResultSet executeSimple(String query)
  {
    if (LOGGER.isDebugEnabled())
    {
      LOGGER.debug("Executing simple {} ", query);
    }
    try
    {
      PostgresStatement statement = sessionHandler.createStatement();
      boolean results = statement.execute(query);
      if (!results)
      {
        return null;
      }
      return statement.getResultSet();
    }
    catch (SQLException e)
    {
      throw new RuntimeException(e);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }


  public void clearState()
  {
    LOGGER.info("clear state for session");
    for (String name : new ArrayList<>(portals.keySet()))
    {
      close('P', name);
    }
    for (String name : new ArrayList<>(parsed.keySet()))
    {
      close('S', name);
    }
  }


  private Prepared getSafeStmt(String statementName)
  {
    Prepared prepared = parsed.get(statementName);
    if (prepared == null)
    {
      throw new IllegalArgumentException("No statement found with name: " + statementName);
    }
    return prepared;
  }

  private Portal getSafePortal(String portalName)
  {
    Portal portal = portals.get(portalName);
    if (portal == null)
    {
      throw new IllegalArgumentException("Cannot find portal: " + portalName);
    }
    return portal;
  }


  /**
   * Represents a PostgeSQL Prepared Obj
   */
  static class Prepared
  {

    /**
     * Object name
     */
    String name;

    /**
     * The SQL Statement
     */
    String sql;

    /**
     * The prepared Statment
     */
    PostgresPreparedStatement prep;

    /**
     * The list of param types
     */
    Integer[] paramType;
  }

  /**
   * Represents a PostgreSQL Portal object
   */
  static class Portal
  {

    /**
     * The portal name
     */
    String name;

    /**
     * The format use in the result set column  (if set)
     */
    FormatCodes.FormatCode[] resultColumnFormat;

    /**
     * The prepared object
     */
    Prepared prep;


    public Portal(String portalName, Prepared preparedStmt,
        FormatCodes.FormatCode[] resultColumnFormat)
    {
      this.name = portalName;
      this.prep = preparedStmt;
      this.resultColumnFormat = resultColumnFormat;
    }
  }

}
