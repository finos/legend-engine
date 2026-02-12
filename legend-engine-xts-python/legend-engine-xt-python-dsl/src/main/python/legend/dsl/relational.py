from legend.dsl.runtime import Connection
from typing import Any, Dict
from enum import Enum

class RelationalDatabaseConnection(Connection):
    def __init__(self, store: str, type: Any, auth: Any, specification: Dict[str, Any]):
        self.store = store
        self.type = type
        self.auth = auth
        self.specification = specification

class DatabaseType(Enum):
    H2 = "H2"
    DuckDB = "DuckDB"
    Snowflake = "Snowflake"
    Postgres = "Postgres"
    SqlServer = "SqlServer"
    MariaDB = "MariaDB"
    Presto = "Presto"
    Trino = "Trino"

class AuthType(Enum):
    Test = "Test"
    Default = "Default"
