package org.finos.legend.engine.persistence.components.relational.snowflake.jdbc;

import org.finos.legend.engine.persistence.components.executor.TabularData;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class SnowflakeJdbcHelper extends JdbcHelper
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeJdbcHelper.class);

	private SnowflakeJdbcHelper(Connection connection)
	{
		super(connection);
	}

	private SnowflakeJdbcTransactionManager transactionManager;

	@Override
	public void beginTransaction()
	{
		try
		{
			this.transactionManager = new SnowflakeJdbcTransactionManager(connection);
			this.transactionManager.beginTransaction();
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public void commitTransaction()
	{
		if (this.transactionManager != null)
		{
			try
			{
				this.transactionManager.commitTransaction();
			}
			catch (SQLException e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void revertTransaction()
	{
		if (this.transactionManager != null)
		{
			try
			{
				this.transactionManager.revertTransaction();
			}
			catch (SQLException e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void closeTransactionManager()
	{
		if (this.transactionManager != null)
		{
			try
			{
				this.transactionManager.close();
			}
			catch (SQLException e)
			{
				throw new RuntimeException(e);
			}
			finally
			{
				this.transactionManager = null;
			}
		}
	}

	@Override
	public TabularData executeQueryAsTabularData(String sql)
	{
		if (this.transactionManager != null)
		{
			return this.transactionManager.convertResultSetToTabularData(sql);
		}
		else
		{
			SnowflakeJdbcTransactionManager txManager = null;
			try
			{
				txManager = new SnowflakeJdbcTransactionManager(connection);
				return txManager.convertResultSetToTabularData(sql);
			}
			catch (Exception e)
			{
				throw new RuntimeException("Error executing SQL query: " + sql, e);
			}
			finally
			{
				if (txManager != null)
				{
					try
					{
						txManager.close();
					}
					catch (SQLException e)
					{
						LOGGER.error("Error closing transaction manager.", e);
					}
				}
			}
		}
	}

	@Override
	public TabularData executeQueryAsTabularData(String sql, int rows)
	{
		if (this.transactionManager != null)
		{
			return this.transactionManager.convertResultSetToTabularData(sql, rows);
		}
		else
		{
			SnowflakeJdbcTransactionManager txManager = null;
			try
			{
				txManager = new SnowflakeJdbcTransactionManager(connection);
				return txManager.convertResultSetToTabularData(sql, rows);
			}
			catch (Exception e)
			{
				throw new RuntimeException("Error executing SQL query: " + sql, e);
			}
			finally
			{
				if (txManager != null)
				{
					try
					{
						txManager.close();
					}
					catch (SQLException e)
					{
						LOGGER.error("Error closing transaction manager.", e);
					}
				}
			}
		}
	}
}
