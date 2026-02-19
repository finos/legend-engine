from typing import Any, List, Optional
from legend.dsl.core import LegendObject
from legend.dsl.runtime import Runtime

class ExecutionContext(LegendObject):
    def __init__(self, name: str, mapping: Any, default_runtime: Runtime):
        self.name = name
        self.mapping = mapping
        self.default_runtime = default_runtime

class DataSpaceSupportInfo(LegendObject):
    pass

class DataSpaceSupportEmail(DataSpaceSupportInfo):
    def __init__(self, email_address: str):
        self.email_address = email_address

class DataSpace(LegendObject):
    execution_contexts: List[ExecutionContext] = []
    default_execution_context: str = ""
    description: str = ""
    support_info: Optional[DataSpaceSupportInfo] = None
    
    # Class level defaults
    execution_contexts = []
    default_execution_context = ""
    description = ""
    support_info = None
