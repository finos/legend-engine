import sys
import os
import unittest
from datetime import date

# Add src/main/python to path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '../../main/python')))

from legend.dsl import Class, Association, Enum, Profile
from legend.dsl.transpiler import Transpiler

class Person(Class):
    name: str
    age: int

class Colors(Enum):
    RED = "RED"
    BLUE = "BLUE"

class TestTranspiler(unittest.TestCase):
    def setUp(self):
        self.transpiler = Transpiler()

    def test_transpile_class(self):
        pure_code = self.transpiler.to_pure(Person)
        expected = """Class model::Person
{
  name: String [1];
  age: Integer [1];
}"""
        self.assertEqual(pure_code.strip(), expected.strip())

    def test_transpile_enum(self):
        pure_code = self.transpiler.to_pure(Colors)
        expected = """Enum model::Colors
{
  RED,
  BLUE,
}"""
        self.assertEqual(pure_code.strip(), expected.strip())

if __name__ == '__main__':
    unittest.main()
