from legend.dsl.core import (
    Class,
    Association,
    Enum,
    Profile,
    Function,
    DerivedProperty
)
from legend.dsl.constraints import Constraint
from legend.dsl.service import (
    Service,
    SingleExecution,
    TestSuite
)
from legend.dsl.runtime import (
    Runtime,
    EngineRuntime,
    Connection,
    JsonModelConnection
)
from legend.dsl.relational import (
    RelationalDatabaseConnection,
    DatabaseType,
    AuthType
)
from legend.dsl.dataspace import (
    DataSpace,
    ExecutionContext,
    DataSpaceSupportEmail
)

# Helpers for creating profile instances to act as decorators
def profile():
    return Profile()
