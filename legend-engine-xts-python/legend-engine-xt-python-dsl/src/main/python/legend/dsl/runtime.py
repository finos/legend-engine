from typing import Any, List, Union, Dict, Optional, Type
from legend.dsl.core import LegendObject, Class

class Connection(LegendObject):
    pass

class ModelConnection(Connection):
    pass

class JsonModelConnection(ModelConnection):
    def __init__(self, class_: Union[Type[Class], str], url: str):
        self.class_ = class_
        self.url = url

class Runtime(LegendObject):
    pass

class EngineRuntime(Runtime):
    def __init__(self, mappings: List[Any], connections: List[Connection]):
        self.mappings = mappings
        self.connections = connections
