import sys
import os
import unittest
from datetime import date, datetime
from typing import List, Optional

# Add src/main/python to path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '../../main/python')))

from legend.dsl import *
from legend.dsl.transpiler import Transpiler

# --- Test Models ---

class ProfileA(Profile):
    pass

@ProfileA("st1")
@ProfileA("tag1", "val1")
class BaseClass(Class):
    base_prop: str

class SubClass(BaseClass):
    sub_prop: int

class ComplexTypes(Class):
    names: List[str]
    maybe_age: Optional[int]
    birth_date: date
    timestamp: datetime

class WithConstraint(Class):
    val: int

    @Constraint
    def positive_val(self) -> bool:
        return self.val > 0

class WithDerived(Class):
    first: str
    last: str

    @DerivedProperty
    def full_name(self) -> str:
        return self.first + " " + self.last

# --- Tests ---

class TestTranspilerComplex(unittest.TestCase):
    def setUp(self):
        self.transpiler = Transpiler()

    def test_inheritance_and_profiles(self):
        # Base Class with Profile
        pure_base = self.transpiler.to_pure(BaseClass)
        expected_base = """Class <<model::ProfileA.st1>> {model::ProfileA.tag1 = 'val1'} model::BaseClass
{
  base_prop: String [1];
}"""
        self.assertEqual(pure_base.strip(), expected_base.strip())

        # Sub Class with inheritance
        pure_sub = self.transpiler.to_pure(SubClass)
        expected_sub = """Class model::SubClass extends model::BaseClass
{
  sub_prop: Integer [1];
}"""
        self.assertEqual(pure_sub.strip(), expected_sub.strip())

    def test_complex_types(self):
        pure_code = self.transpiler.to_pure(ComplexTypes)
        expected = """Class model::ComplexTypes
{
  names: String [*];
  maybe_age: Integer [0..1];
  birth_date: Date [1];
  timestamp: DateTime [1];
}"""
        self.assertEqual(pure_code.strip(), expected.strip())

    def test_constraint(self):
        pure_code = self.transpiler.to_pure(WithConstraint)
        # Note: We are just checking if the constraint block definition exists for now, 
        # transpiling the python lambda to pure is a much larger task.
        expected_part = "positive_val"
        self.assertIn(expected_part, pure_code)
        self.assertIn("// TODO: Transpile Constraint Logic", pure_code)

    def test_derived_property(self):
        pure_code = self.transpiler.to_pure(WithDerived)
        # Checking signature generation
        expected_part = "full_name() : String[1]"
        self.assertIn(expected_part, pure_code)

if __name__ == '__main__':
    unittest.main()
