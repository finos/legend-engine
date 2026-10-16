// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! Tests for function type parsing and `>>` token splitting in nested generics.

mod helpers;

use legend_pure_parser_ast::type_ref::TypeSpec;

// ---------------------------------------------------------------------------
// Function types in type arguments
// ---------------------------------------------------------------------------

#[test]
fn function_type_no_params() {
    // {->Boolean[1]} — zero-parameter function type
    let ast = helpers::parse_ok(
        "native function meta::pure::functions::lang::if<T|m>(test:Boolean[1], valid:Function<{->T[m]}>[1], invalid:Function<{->T[m]}>[1]):T[m];",
    );
    assert_eq!(ast.element_count(), 1);
}

#[test]
fn function_type_single_param() {
    // {T[1] -> Boolean[1]} — single-parameter function type
    let ast = helpers::parse_ok(
        "native function meta::pure::functions::collection::filter<T>(value:T[*], func:Function<{T[1]->Boolean[1]}>[1]):T[*];",
    );
    assert_eq!(ast.element_count(), 1);
}

#[test]
fn function_type_two_params() {
    // {T[1], U[1] -> V[1]} — two-parameter function type
    let ast = helpers::parse_ok(
        "native function pkg::foldWith<T,U,V>(val:T[*], init:U[1], func:Function<{T[1],U[1]->V[1]}>[1]):V[1];",
    );
    assert_eq!(ast.element_count(), 1);
}

#[test]
fn function_type_nested() {
    // Function<{Function<{->Z[y]}>[1]->Z[y]}> — nested function types
    let ast = helpers::parse_ok(
        "function meta::pure::tests::eval<Z|y>(f:Function<{Function<{->Z[y]}>[1]->Z[y]}>[1]):Boolean[1] { true }",
    );
    assert_eq!(ast.element_count(), 1);
}

#[test]
fn function_type_argument_is_a_function_type() {
    let ast = helpers::parse_ok(
        "native function pkg::f(func:Function<{String[1]->Integer[1]}>[1]):Integer[1];",
    );
    let elem = &ast.sections[0].elements[0];
    let legend_pure_parser_ast::element::Element::NativeFunction(nf) = elem else {
        panic!("expected NativeFunction");
    };
    let Some(TypeSpec::Type(tr)) = &nf.parameters[0].type_ref else {
        panic!("expected a named type");
    };
    assert_eq!(tr.name.as_str(), "Function");
    let TypeSpec::Function(function) = &tr.type_arguments[0].type_spec else {
        panic!("expected a function type argument");
    };
    assert_eq!(function.parameters.len(), 1);
    assert_eq!(function.return_type.full_path(), "Integer");
}

// ---------------------------------------------------------------------------
// `>>` token splitting in nested generics
// ---------------------------------------------------------------------------

#[test]
fn nested_generics_double_close() {
    // Pair<Function<{->Boolean[1]}>, String> ends with >>
    let ast = helpers::parse_ok(
        "native function pkg::f(x:Pair<Function<{->Boolean[1]}>, String>[1]):String[1];",
    );
    assert_eq!(ast.element_count(), 1);
}

#[test]
fn nested_generics_triple_close() {
    // Map<String, List<Set<Integer>>> ends with >>>
    let ast = helpers::parse_ok(
        "native function pkg::g(x:Map<String, List<Set<Integer>>>[1]):Integer[1];",
    );
    assert_eq!(ast.element_count(), 1);
}

#[test]
fn nested_function_type_double_close() {
    // if<T|m>(..., Function<{->T[m]}>, Function<{->T[m]}>):T[m]
    // The Pair<..., Function<{->T[m]}>> at the end has >>
    let ast = helpers::parse_ok(
        "native function pkg::h(pairs:Pair<Function<{->Boolean[1]}>, Function<{->Integer[1]}>>[*]):Boolean[1];",
    );
    assert_eq!(ast.element_count(), 1);
}

// ---------------------------------------------------------------------------
// Generic new-instance: ^Type<Args>(props)
// ---------------------------------------------------------------------------

#[test]
fn generic_new_instance() {
    // ^List<U>(values = $vals) — generic type argument on new-instance
    let ast = helpers::parse_ok(
        "function pkg::mkList<U>(vals:U[*]):List<U>[1] { ^List<U>(values = $vals) }",
    );
    assert_eq!(ast.element_count(), 1);
}

#[test]
fn generic_new_instance_nested() {
    // ^Map<String, List<Integer>>(entries = []) — nested generics on new-instance
    let ast = helpers::parse_ok(
        "function pkg::mkMap():Map<String, List<Integer>>[1] { ^Map<String, List<Integer>>(entries = []) }",
    );
    assert_eq!(ast.element_count(), 1);
}

// ---------------------------------------------------------------------------
// Copy from variable: ^$var(overrides)
// ---------------------------------------------------------------------------

#[test]
fn copy_from_variable() {
    // ^$p2(lastName = 'new') — copy variable with property overrides
    let ast = helpers::parse_ok("function pkg::f(p:Person[1]):Person[1] { ^$p(lastName = 'new') }");
    assert_eq!(ast.element_count(), 1);
}

#[test]
fn copy_from_variable_multiple_overrides() {
    // ^$obj(a = 1, b = 'hello') — copy with multiple overrides
    let ast = helpers::parse_ok("function pkg::g(x:Obj[1]):Obj[1] { ^$x(a = 1, b = 'hello') }");
    assert_eq!(ast.element_count(), 1);
}

// ---------------------------------------------------------------------------
// Error recovery
// ---------------------------------------------------------------------------

#[test]
fn error_recovery_preserves_valid_elements() {
    // First and third functions are valid, second line has a parse error (random junk).
    // Error recovery should preserve the valid functions.
    let source = r"
function pkg::valid(): String[1] { 'hello' }
Bogus pkg::broken extends Integer
function pkg::also_valid(): Integer[1] { 42 }
";
    match legend_pure_parser_parser::parse(source, "test.pure") {
        Ok(ast) => {
            panic!(
                "Expected partial parse, but got clean parse with {} elements",
                ast.element_count()
            );
        }
        Err(partial) => {
            // Should recover the two valid functions
            assert!(
                partial.source_file.element_count() >= 2,
                "Expected at least 2 recovered elements, got {}",
                partial.source_file.element_count()
            );
            assert!(
                !partial.errors.is_empty(),
                "Expected at least 1 parse error"
            );
        }
    }
}

#[test]
fn error_recovery_native_before_broken() {
    // Native function (valid) followed by unrecognised element keyword,
    // then another native. Recovery must preserve both natives.
    let source = r"
native function pkg::myNative(x:Integer[1]):String[1];
Bogus pkg::broken extends Integer
native function pkg::anotherNative(s:String[1]):Boolean[1];
";
    match legend_pure_parser_parser::parse(source, "test.pure") {
        Ok(ast) => {
            panic!(
                "Expected partial parse, but got clean parse with {} elements",
                ast.element_count()
            );
        }
        Err(partial) => {
            assert!(
                partial.source_file.element_count() >= 2,
                "Expected at least 2 recovered elements (native functions), got {}",
                partial.source_file.element_count()
            );
        }
    }
}
