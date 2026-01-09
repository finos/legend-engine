import sys
import os

# Add src/main/python to path so we can import legend.dsl
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '../../main/python')))

from legend.dsl import *
from legend.dsl.relational import DatabaseType, AuthType
from datetime import date
from typing import Optional, List

# 1. Profile
class DocProfile(Profile):
    pass

# 2. Class
@DocProfile("deprecated")
@DocProfile("author", "John Doe")
class Person(Class):
    first_name: str
    last_name: str
    age: int
    birth_date: date
    
    @DerivedProperty
    def full_name(self) -> str:
         return f"{self.first_name} {self.last_name}"

    @Constraint
    def valid_age(self) -> bool:
        return self.age >= 0

class Company(Class):
    name: str

# 3. Association
class Employment(Association):
    employee: Person
    employer: Optional[Company]

# 4. Function
@Function
def get_adult_users(min_age: int) -> List[Person]:
    # This would need a proper query builder in the future
    return [] 

# 5. Connection & Runtime
json_connection = JsonModelConnection(
    class_=Person,
    url="data.json"
)

db_connection = RelationalDatabaseConnection(
    store="MyDatabaseStore",
    type=DatabaseType.H2,
    auth=AuthType.Test,
    specification={
        "type": "LocalH2DataSourceSpecification",
        "testDataSetupCsv": "data.csv"
    }
)

runtime = EngineRuntime(
    mappings=[], # Placeholder
    connections=[json_connection, db_connection]
)

# 6. Service
class MyService(Service):
    pattern = "/my/service"
    documentation = "My service docs"
    execution = SingleExecution(
        func=lambda: [],
        mapping=None,
        runtime=runtime
    )

# 7. DataSpace
class MyDataSpace(DataSpace):
    execution_contexts = [
        ExecutionContext(
            name="default_context",
            mapping=None,
            default_runtime=runtime
        )
    ]
    default_execution_context = "default_context"
    description = "My DataSpace Description"
    support_info = DataSpaceSupportEmail(
        email_address="support@example.com"
    )

print("DSL Demo ran successfully!")
