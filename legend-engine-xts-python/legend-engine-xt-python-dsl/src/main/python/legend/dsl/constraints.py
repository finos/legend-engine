from typing import Any

class Constraint:
    def __init__(self, func=None):
        self.func = func

    def __call__(self, func):
        self.func = func
        func.__legend_is_constraint__ = True
        return func

# Allow using @Constraint without parens
def Constraint(arg):
    if callable(arg):
        arg.__legend_is_constraint__ = True
        return arg
    else:
        # If we ever support @Constraint(name="foo")
        pass
