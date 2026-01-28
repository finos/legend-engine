from typing import Any, List, Optional, Callable, Union
from legend.dsl.core import LegendObject
from legend.dsl.runtime import Runtime

class Execution(LegendObject):
    pass

class SingleExecution(Execution):
    def __init__(self, func: Callable, mapping: Any, runtime: Runtime):
        self.func = func
        self.mapping = mapping
        self.runtime = runtime

class TestSuite(LegendObject):
    pass

class Service(LegendObject):
    pattern: str
    documentation: str
    execution: Execution
    test_suites: List[TestSuite] = []
    
    # These are typically set in the class definition
    pattern = None
    documentation = ""
    execution = None
    test_suites = []
    
    def __init__(self):
        pass
