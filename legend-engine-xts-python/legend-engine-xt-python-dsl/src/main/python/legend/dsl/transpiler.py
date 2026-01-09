from typing import Any, get_type_hints, Type, Optional, List, Union, _GenericAlias
from datetime import date, datetime
from legend.dsl.core import LegendObject, Class, Association, Enum, Profile, LegendType, DerivedProperty
from legend.dsl.constraints import Constraint
import inspect

class Transpiler:
    """
    Translates Python DSL objects to Pure.
    """
    
    def __init__(self):
        self.package_map = {} 

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

    def _get_pure_type_and_multiplicity(self, py_type: Any) -> tuple[str, str]:
        # Handle List
        if hasattr(py_type, "__origin__") and py_type.__origin__ is list:
             arg = py_type.__args__[0]
             p_type, _ = self._get_pure_type_and_multiplicity(arg)
             return p_type, "[*]"
        
        # Handle Optional
        if hasattr(py_type, "__origin__"):
            # Check for Union[..., NoneType] which matches Optional
            if py_type.__origin__ is Union:
                 args = py_type.__args__
                 non_none = [a for a in args if a is not type(None)]
                 if len(non_none) == 1:
                     p_type, _ = self._get_pure_type_and_multiplicity(non_none[0])
                     return p_type, "[0..1]"

        # Basic Types
        if py_type == str:
            return "String", "[1]"
        elif py_type == int:
            return "Integer", "[1]"
        elif py_type == float:
            return "Float", "[1]"
        elif py_type == bool:
            return "Boolean", "[1]"
        elif py_type == date:
            return "Date", "[1]"
        elif py_type == datetime:
            return "DateTime", "[1]"
        elif inspect.isclass(py_type) and issubclass(py_type, LegendType):
            return f"model::{py_type.__name__}", "[1]"
        
        # Fallback
        return "String", "[1]"


    def _get_stereotypes_and_tags(self, cls: Type[Class]) -> str:
        # Check if the metadata is actually on this class, not inherited
        if "__legend_metadata__" not in cls.__dict__:
            return ""
        
        metadata = cls.__legend_metadata__
        if "profiles" not in metadata:
             return ""
        
        parts = []
        for profile_name, data in metadata["profiles"].items():
            # Stereotypes
            for stereotype in data["stereotypes"]:
                parts.append(f"<<model::{profile_name}.{stereotype}>>")
            # Tags
            for tag, val in data["tags"].items():
                parts.append(f"{{model::{profile_name}.{tag} = '{val}'}}")
        
        return " " + " ".join(parts)

    def _transpile_class(self, cls: Type[Class]) -> str:
        name = cls.__name__
        package = "model"
        
        # Stereotypes/Tags
        metadata_str = self._get_stereotypes_and_tags(cls)

        # Inheritance
        extends_str = ""
        bases = cls.__bases__
        base_class = None
        if bases and bases[0] is not Class and issubclass(bases[0], Class):
            base_class = bases[0]
            extends_str = f" extends model::{base_class.__name__}"

        lines = [f"Class{metadata_str} {package}::{name}{extends_str}"]
        lines.append("{")
        
        # Properties
        # get_type_hints returns all types in MRO. We need to filter out ones present in base class.
        type_hints = get_type_hints(cls)
        base_hints = get_type_hints(base_class) if base_class else {}
        
        for prop_name, prop_type in type_hints.items():
            # Skip if defined in base class (and type hasn't changed/specialized - sophisticated check skipped for now)
            if prop_name in base_hints:
                continue
                
            pure_type, multiplicity = self._get_pure_type_and_multiplicity(prop_type)
            lines.append(f"  {prop_name}: {pure_type} {multiplicity};")

        # Constraints (local only)
        for name, member in cls.__dict__.items():
            if hasattr(member, "__legend_is_constraint__"):
                 lines.append(f"  {name}")
                 lines.append("  [")
                 lines.append("    // TODO: Transpile Constraint Logic")
                 lines.append("  ]")
        
        # Derived Properties (local only)
        for name, member in cls.__dict__.items():
             if isinstance(member, DerivedProperty):
                 # Assume return type is type hinted on function
                 # Note: in Python 3.10+ types are in __annotations__
                 ret_type = "String" # default
                 if hasattr(member.func, "__annotations__") and "return" in member.func.__annotations__:
                     r_type = member.func.__annotations__["return"]
                     ret_type, _ = self._get_pure_type_and_multiplicity(r_type)
                 
                 lines.append(f"  {name}() : {ret_type}[1]")
                 lines.append("  {")
                 lines.append("    // TODO: Transpile Body")
                 lines.append("  }")

        lines.append("}")
        return "\n".join(lines)

    def _transpile_association(self, cls: Type[Association]) -> str:
        name = cls.__name__
        package = "model"
        
        lines = [f"Association {package}::{name}"]
        lines.append("{")
        
        type_hints = get_type_hints(cls)
        for prop_name, prop_type in type_hints.items():
            pure_type, multiplicity = self._get_pure_type_and_multiplicity(prop_type)
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
