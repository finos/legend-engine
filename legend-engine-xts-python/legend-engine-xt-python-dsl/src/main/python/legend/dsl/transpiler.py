from typing import Any, get_type_hints, Type, Optional
from datetime import date, datetime
from legend.dsl.core import LegendObject, Class, Association, Enum, Profile, LegendType
from legend.dsl.constraints import Constraint
import inspect

class Transpiler:
    """
    Translates Python DSL objects to Pure.
    """
    
    def __init__(self):
        self.package_map = {} # Can be used to map python modules to Pure packages

    def to_pure(self, obj: Any) -> str:
        if isinstance(obj, type):
            if issubclass(obj, Class):
                return self._transpile_class(obj)
            elif issubclass(obj, Association):
                return self._transpile_association(obj)
            elif issubclass(obj, Enum):
                return self._transpile_enum(obj)
            elif issubclass(obj, Profile):
                return self._transpile_profile(obj)
        
        return f"// Unsupported object type: {obj}"

    def _get_pure_type(self, py_type: Any) -> str:
        if py_type == str:
            return "String"
        elif py_type == int:
            return "Integer"
        elif py_type == float:
            return "Float"
        elif py_type == bool:
            return "Boolean"
        elif py_type == date:
            return "Date"
        elif py_type == datetime:
            return "DateTime"
        elif inspect.isclass(py_type) and issubclass(py_type, LegendType):
            return py_type.__name__
        
        # Handle Optional
        # Simple check for typing.Optional or Union[..., None]
        # For now, we assume simple types
        
        return "String" # Default fallback

    def _transpile_class(self, cls: Type[Class]) -> str:
        name = cls.__name__
        # Package derivation could be more complex
        package = "model" 
        
        lines = [f"Class {package}::{name}"]
        lines.append("{")
        
        # Properties
        type_hints = get_type_hints(cls)
        for prop_name, prop_type in type_hints.items():
            pure_type = self._get_pure_type(prop_type)
            # Default logic: Optional check (not fully robust here but works for demo)
            multiplicity = "[1]"
            if str(prop_type).startswith("typing.Optional"):
                 # This is a very rough check, normally we'd inspect the Union
                 multiplicity = "[0..1]"
            
            lines.append(f"  {prop_name}: {pure_type} {multiplicity};")
            
        lines.append("}")
        return "\n".join(lines)

    def _transpile_association(self, cls: Type[Association]) -> str:
        name = cls.__name__
        package = "model"
        
        lines = [f"Association {package}::{name}"]
        lines.append("{")
        
        type_hints = get_type_hints(cls)
        for prop_name, prop_type in type_hints.items():
             pure_type = self._get_pure_type(prop_type)
             # Default multiplicity stub
             multiplicity = "[1]"
             lines.append(f"  {prop_name}: {pure_type} {multiplicity};")

        lines.append("}")
        return "\n".join(lines)

    def _transpile_enum(self, cls: Type[Enum]) -> str:
        name = cls.__name__
        package = "model"
        
        lines = [f"Enum {package}::{name}"]
        lines.append("{")
        for member in cls:
            lines.append(f"  {member.name},")
        lines.append("}")
        return "\n".join(lines)

    def _transpile_profile(self, cls: Type[Profile]) -> str:
        name = cls.__name__
        package = "model"
        lines = [f"Profile {package}::{name}"]
        lines.append("{")
        # In a real implementation we would allow defining stereotypes/tags in the class body
        # For this prototype we can just list common ones or inspect the class for fields
        lines.append("}")
        return "\n".join(lines)
