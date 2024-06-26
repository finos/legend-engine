package org.finos.legend.engine.persistence.components.relational.snowflake.jdbc;

import net.snowflake.client.jdbc.SnowflakeResultSet;
import org.finos.legend.engine.persistence.components.executor.TabularData;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcTransactionManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SnowflakeJdbcTransactionManager extends JdbcTransactionManager
{
	public SnowflakeJdbcTransactionManager(Connection connection) throws SQLException
	{
		super(connection);
	}

	public TabularData convertResultSetToTabularData(String sql)
	{
		try
		{
			List<Map<String, Object>> resultList = new ArrayList<>();
			Optional<String> queryId;
			try (ResultSet resultSet = this.statement.executeQuery(sql))
			{
				while (resultSet.next())
				{
					extractResults(resultList, resultSet);
				}
				queryId = Optional.ofNullable(resultSet.unwrap(SnowflakeResultSet.class).getQueryID());
			}
			return TabularData.builder()
				.addAllData(resultList)
				.queryId(queryId)
				.build();
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	public TabularData convertResultSetToTabularData(String sql, int rows)
	{
		try
		{
			List<Map<String, Object>> resultList = new ArrayList<>();
			Optional<String> queryId;
			try (ResultSet resultSet = this.statement.executeQuery(sql))
			{
				int iter = 0;
				while (resultSet.next() && iter < rows)
				{
					iter++;
					extractResults(resultList, resultSet);
				}
				queryId = Optional.ofNullable(resultSet.unwrap(SnowflakeResultSet.class).getQueryID());
			}
			return TabularData.builder()
				.addAllData(resultList)
				.queryId(queryId)
				.build();
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}
}
