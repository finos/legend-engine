package org.finos.legend.engine.pg.postgres;

import java.util.List;

public interface LegendExecutionClient
{
  public List<LegendColumn> getSchema(String query);

  public Iterable<TDSRow> executeQuery(String query);

}
