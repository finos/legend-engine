from enum import Enum as PyEnum
from typing import Any, List, Optional, Type, Union, Dict

class LegendObject:
    """Base class for all Legend DSL objects."""
    pass

class LegendType(LegendObject):
    pass

class Class(LegendType):
    """Base class for Legend Classes."""
    pass

class Association(LegendObject):
    """Base class for Legend Associations."""
    pass

class Enum(PyEnum):
    """Base class for Legend Enums."""
    pass

class Profile(LegendObject):
    """
    Base class for Legend Profiles.
    Can be used as a decorator for Stereotypes (1 arg) and Tags (2 args).
    """
    def __new__(cls, *args, **kwargs):
        # logic to handle decorator usage usage: @MyProfile("stereotype") or @MyProfile("tag", "value")
        if len(args) > 0:
             # It's being used as a decorator
             return _ProfileDecorator(cls, *args)
        return super().__new__(cls)

class _ProfileDecorator:
    def __init__(self, profile_cls: Type[Profile], *args):
        self.profile_cls = profile_cls
        self.args = args

    def __call__(self, target):
        # Attach metadata to the target class
        if not hasattr(target, "__legend_metadata__"):
            target.__legend_metadata__ = {}
        
        metadata = target.__legend_metadata__
        profile_name = self.profile_cls.__name__
        
        if "profiles" not in metadata:
            metadata["profiles"] = {}
            
        if profile_name not in metadata["profiles"]:
            metadata["profiles"][profile_name] = {"stereotypes": [], "tags": {}}

        if len(self.args) == 1:
            # Stereotype
            metadata["profiles"][profile_name]["stereotypes"].append(self.args[0])
        elif len(self.args) == 2:
            # Tag
            metadata["profiles"][profile_name]["tags"][self.args[0]] = self.args[1]
        
        return target

class DerivedProperty:
    def __init__(self, func=None, return_type=None):
        self.func = func
        self.return_type = return_type

    def __call__(self, func):
        self.func = func
        return self

class Function(LegendObject):
    pass

def Function(func):
    # Decorator for functions
    func.__legend_is_function__ = True
    return func

# Basic Types (for when python types aren't enough or for explicit usage)
# In this DSL we prefer native python types, but we might want wrappers if needed.
# For now, we assume mapping from definitions in transpiler.
