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

//! Top-level packageable elements: Class, Enum, Function, Profile, Association, Measure, Primitive.
//!
//! Every element is [`Spanned`], [`Annotated`], and [`PackageableElement`].
//! The [`Element`] enum wraps all element types for uniform handling.

use smol_str::SmolStr;

use crate::annotation::{
    PackageableElementPtr, Parameter, SpannedString, StereotypePtr, TaggedValue,
};
pub use crate::data::EmbeddedDataValue;
use crate::data::RelationElement;
use crate::expression::Expression;
use crate::source_info::{SourceInfo, Spanned};
use crate::type_ref::{
    Identifier, Multiplicity, Package, TypeReference, TypeSpec, TypeVariableParameter,
};

/// A top-level element that lives in a package and has a name.
///
/// Named `PackageableElement` to match the Java convention, easing transition
/// for developers moving between the Java and Rust implementations.
pub trait PackageableElement: Spanned + Annotated {
    /// Returns the package this element belongs to.
    fn package(&self) -> Option<&Package>;
    /// Returns the name of this element.
    ///
    /// Returns the identifier string only. Use `element.name.source_info` directly
    /// when the exact token position is needed (IDE hover, error underlining, etc.).
    fn name(&self) -> &Identifier;
}

/// An element that can carry stereotypes and tagged values.
pub trait Annotated {
    /// Returns the stereotypes applied to this element.
    fn stereotypes(&self) -> &[StereotypePtr];
    /// Returns the tagged values applied to this element.
    fn tagged_values(&self) -> &[TaggedValue];
}

/// A top-level packageable element in the Pure grammar.
///
/// This is the root type the parser produces — a source file parses into
/// a `Vec<Element>`.
///
/// The [`DSLElement`](Self::DSLElement) variant is the extensibility
/// hook for M2 DSLs. Each DSL crate (`dsl-diagram`, `dsl-mapping`, …)
/// defines its own concrete element types and stores them
/// type-erased as `Box<dyn dsl::DSLElement>`. Core never carries any
/// DSL-specific knowledge.
#[derive(Debug)]
pub enum Element {
    /// A class definition.
    Class(ClassDef),
    /// An enumeration definition.
    Enumeration(EnumDef),
    /// A function definition.
    Function(FunctionDef),
    /// A native function declaration (no body).
    NativeFunction(NativeFunctionDef),
    /// A profile definition.
    Profile(ProfileDef),
    /// An association definition.
    Association(AssociationDef),
    /// A measure definition (with units).
    Measure(MeasureDef),
    /// A primitive type definition.
    Primitive(PrimitiveDef),
    /// A DSL-defined element. The boxed trait object is owned by an
    /// out-of-core DSL crate (e.g. `dsl-diagram`'s `DiagramDef`).
    /// Consumers downcast via [`crate::dsl::DSLElement::as_any`].
    DSLElement(Box<dyn crate::dsl::DSLElement>),
}

impl Clone for Element {
    fn clone(&self) -> Self {
        match self {
            Self::Class(e) => Self::Class(e.clone()),
            Self::Enumeration(e) => Self::Enumeration(e.clone()),
            Self::Function(e) => Self::Function(e.clone()),
            Self::NativeFunction(e) => Self::NativeFunction(e.clone()),
            Self::Profile(e) => Self::Profile(e.clone()),
            Self::Association(e) => Self::Association(e.clone()),
            Self::Measure(e) => Self::Measure(e.clone()),
            Self::Primitive(e) => Self::Primitive(e.clone()),
            Self::DSLElement(e) => Self::DSLElement(e.clone_box()),
        }
    }
}

impl PartialEq for Element {
    fn eq(&self, other: &Self) -> bool {
        match (self, other) {
            (Self::Class(a), Self::Class(b)) => a == b,
            (Self::Enumeration(a), Self::Enumeration(b)) => a == b,
            (Self::Function(a), Self::Function(b)) => a == b,
            (Self::NativeFunction(a), Self::NativeFunction(b)) => a == b,
            (Self::Profile(a), Self::Profile(b)) => a == b,
            (Self::Association(a), Self::Association(b)) => a == b,
            (Self::Measure(a), Self::Measure(b)) => a == b,
            (Self::Primitive(a), Self::Primitive(b)) => a == b,
            (Self::DSLElement(a), Self::DSLElement(b)) => a.eq_content(b.as_ref()),
            _ => false,
        }
    }
}

impl Element {
    /// Moves the start of the element's span to where `start` begins: the
    /// element's grammar rule began before its keyword (a documentation
    /// literal). Plug-in (DSL) elements own their spans and are unchanged.
    pub fn widen_start_to(&mut self, start: &SourceInfo) {
        let si = match self {
            Self::Class(e) => &mut e.source_info,
            Self::Enumeration(e) => &mut e.source_info,
            Self::Function(e) => &mut e.source_info,
            Self::NativeFunction(e) => &mut e.source_info,
            Self::Profile(e) => &mut e.source_info,
            Self::Association(e) => &mut e.source_info,
            Self::Measure(e) => &mut e.source_info,
            Self::Primitive(e) => &mut e.source_info,
            Self::DSLElement(_) => return,
        };
        si.start_line = start.start_line;
        si.start_column = start.start_column;
    }
}

impl Spanned for Element {
    fn source_info(&self) -> &SourceInfo {
        match self {
            Self::Class(e) => e.source_info(),
            Self::Enumeration(e) => e.source_info(),
            Self::Function(e) => e.source_info(),
            Self::NativeFunction(e) => e.source_info(),
            Self::Profile(e) => e.source_info(),
            Self::Association(e) => e.source_info(),
            Self::Measure(e) => e.source_info(),
            Self::Primitive(e) => e.source_info(),
            Self::DSLElement(e) => e.source_info(),
        }
    }
}

impl PackageableElement for Element {
    fn package(&self) -> Option<&Package> {
        match self {
            Self::Class(e) => e.package(),
            Self::Enumeration(e) => e.package(),
            Self::Function(e) => e.package(),
            Self::NativeFunction(e) => e.package(),
            Self::Profile(e) => e.package(),
            Self::Association(e) => e.package(),
            Self::Measure(e) => e.package(),
            Self::Primitive(e) => e.package(),
            Self::DSLElement(e) => e.package(),
        }
    }

    fn name(&self) -> &Identifier {
        match self {
            Self::Class(e) => e.name(),
            Self::Enumeration(e) => e.name(),
            Self::Function(e) => e.name(),
            Self::NativeFunction(e) => e.name(),
            Self::Profile(e) => e.name(),
            Self::Association(e) => e.name(),
            Self::Measure(e) => e.name(),
            Self::Primitive(e) => e.name(),
            Self::DSLElement(e) => e.name(),
        }
    }
}

impl Annotated for Element {
    fn stereotypes(&self) -> &[StereotypePtr] {
        match self {
            Self::Class(e) => e.stereotypes(),
            Self::Enumeration(e) => e.stereotypes(),
            Self::Function(e) => e.stereotypes(),
            Self::NativeFunction(e) => e.stereotypes(),
            Self::Profile(e) => e.stereotypes(),
            Self::Association(e) => e.stereotypes(),
            Self::Measure(e) => e.stereotypes(),
            Self::Primitive(e) => e.stereotypes(),
            Self::DSLElement(e) => e.stereotypes(),
        }
    }

    fn tagged_values(&self) -> &[TaggedValue] {
        match self {
            Self::Class(e) => e.tagged_values(),
            Self::Enumeration(e) => e.tagged_values(),
            Self::Function(e) => e.tagged_values(),
            Self::NativeFunction(e) => e.tagged_values(),
            Self::Profile(e) => e.tagged_values(),
            Self::Association(e) => e.tagged_values(),
            Self::Measure(e) => e.tagged_values(),
            Self::Primitive(e) => e.tagged_values(),
            Self::DSLElement(e) => e.tagged_values(),
        }
    }
}

/// A primitive type definition: `Primitive meta::pure::MyInt extends Integer`.
///
/// Primitives may declare parametric-value inputs and constraints:
/// `Primitive P(x:Integer[1]) extends Integer [$this < $x]`. The
/// constraint expressions are checked at cast/new time, with `$this`
/// bound to the target value and `$x` (or whatever the type-variable
/// parameter was named) bound to the type-variable-VALUE supplied at
/// the reference site (`@P(8)` binds `x = 8`).
#[derive(Debug, Clone, PartialEq, crate::PackageableElement)]
pub struct PrimitiveDef {
    /// The package.
    pub package: Option<Package>,
    /// The primitive type name with its source location.
    pub name: SpannedString,
    /// The base type this primitive extends.
    pub super_type: TypeReference,
    /// Type variable parameters, e.g., `(x:Integer[1])`.
    pub type_variable_parameters: Vec<TypeVariableParameter>,
    /// Constraints declared in the `[…]` block after the base type.
    /// Evaluated at cast/new time against the value being produced.
    pub constraints: Vec<Constraint>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A profile definition: `Profile <<stereo>> meta::pure::profiles::doc { stereotypes: [...]; tags: [...]; }`.
#[derive(Debug, Clone, PartialEq, crate::PackageableElement)]
pub struct ProfileDef {
    /// The package this profile belongs to.
    pub package: Option<Package>,
    /// The profile name with its source location.
    pub name: SpannedString,
    /// Stereotype names declared (defined) by this profile.
    pub stereotype_names: Vec<SpannedString>,
    /// Tag names declared (defined) by this profile.
    pub tag_names: Vec<SpannedString>,
    /// Stereotypes applied to this profile element (annotations on the definition).
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values applied to this profile element (annotations on the definition).
    pub tagged_values: Vec<TaggedValue>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// An enumeration definition: `Enum <<stereo>> {tag='val'} MyEnum { A, B, C }`.
#[derive(Debug, Clone, PartialEq, crate::PackageableElement)]
pub struct EnumDef {
    /// The package.
    pub package: Option<Package>,
    /// The enum name with its source location.
    pub name: SpannedString,
    /// Enum values (members).
    pub values: Vec<EnumValue>,
    /// Stereotypes on the enum.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values on the enum.
    pub tagged_values: Vec<TaggedValue>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A single value (member) in an enumeration: `<<stereo>> {tag='val'} ValueName`.
#[derive(Debug, Clone, PartialEq, crate::Annotated)]
pub struct EnumValue {
    /// The value name.
    pub name: Identifier,
    /// Stereotypes on this enum value.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values on this enum value.
    pub tagged_values: Vec<TaggedValue>,
    /// The whole value: documentation, stereotypes and tagged values, then
    /// the name.
    pub source_info: SourceInfo,
}

/// Aggregation kind for properties.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AggregationKind {
    /// No aggregation — `(none)`.
    None,
    /// Shared aggregation — `(shared)`.
    Shared,
    /// Composite aggregation — `(composite)`.
    Composite,
}

/// A property's default value: `= expr`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct DefaultValue {
    /// The value.
    pub value: Expression,
    /// The whole expression after `=` (the expression's own span may be
    /// narrower, e.g. the `^` of `^Class()`).
    pub source_info: SourceInfo,
}

/// A class property: `<<stereo>> {tag='val'} name: Type[mult]`.
#[derive(Debug, Clone, PartialEq, crate::Annotated)]
pub struct Property {
    /// Property name.
    pub name: Identifier,
    /// Property type (may be a regular type or a unit reference).
    pub type_ref: TypeSpec,
    /// Multiplicity.
    pub multiplicity: Multiplicity,
    /// Aggregation kind (if specified).
    pub aggregation: Option<AggregationKind>,
    /// Default value (if specified).
    pub default_value: Option<DefaultValue>,
    /// Stereotypes on this property.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values on this property.
    pub tagged_values: Vec<TaggedValue>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A qualified (derived) property: `name(params) { body }: ReturnType[mult]`.
#[derive(Debug, Clone, PartialEq, crate::Annotated)]
pub struct QualifiedProperty {
    /// Property name.
    pub name: Identifier,
    /// Parameters.
    pub parameters: Vec<Parameter>,
    /// Return type (may be a regular type or a unit reference).
    pub return_type: TypeSpec,
    /// Return multiplicity.
    pub return_multiplicity: Multiplicity,
    /// Body expressions.
    pub body: Vec<Expression>,
    /// Stereotypes on this qualified property.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values on this qualified property.
    pub tagged_values: Vec<TaggedValue>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A class constraint.
///
/// Constraints can be unnamed (just an expression) or named with optional
/// enforcement level, external ID, and message function.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct Constraint {
    /// Constraint name (optional — unnamed constraints are allowed).
    pub name: Option<Identifier>,
    /// Owner: `~owner: identifier`.
    pub owner: Option<Identifier>,
    /// The constraint function/expression.
    pub function_definition: Expression,
    /// Enforcement level, e.g., `Warn`, `Error`.
    pub enforcement_level: Option<Identifier>,
    /// External identifier.
    pub external_id: Option<String>,
    /// Message function (evaluated when constraint fails).
    pub message: Option<Expression>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A class's or function's type and multiplicity parameters: `<T, U|m>`.
#[derive(Debug, Clone, PartialEq, Eq, crate::Spanned)]
pub struct TypeAndMultiplicityParameters {
    /// Type parameters, e.g. `T` and `U` (a variance prefix is dropped).
    pub type_parameters: Vec<Identifier>,
    /// Multiplicity parameters, e.g. `m`.
    pub multiplicity_parameters: Vec<Identifier>,
    /// From `<` to `>`.
    pub source_info: SourceInfo,
}

/// A class definition with properties, constraints, stereotypes, tagged values,
/// qualified properties, and type parameters.
///
/// ```text
/// Class <<temporal.businesstemporal>> {doc.description = 'A person'}
///   model::domain::Person extends model::domain::LegalEntity
/// [
///   $this.name->isNotEmpty(),
///   ageConstraint: $this.age >= 0
/// ]
/// {
///   name: String[1];
///   age: Integer[0..1];
///   fullName(sep: String[1]) { $this.firstName + $sep + $this.lastName }: String[1];
/// }
/// ```
#[derive(Debug, Clone, PartialEq, crate::PackageableElement)]
pub struct ClassDef {
    /// The package.
    pub package: Option<Package>,
    /// The class name with its source location.
    pub name: SpannedString,
    /// Type variable parameters, e.g., `(x:Integer[1])`.
    pub type_variable_parameters: Vec<TypeVariableParameter>,
    /// Type and multiplicity parameters, e.g. `<T, U|m>`.
    pub type_and_multiplicity_parameters: Option<TypeAndMultiplicityParameters>,
    /// Super types (`extends`).
    pub super_types: Vec<TypeReference>,
    /// Regular properties.
    pub properties: Vec<Property>,
    /// Qualified (derived) properties.
    pub qualified_properties: Vec<QualifiedProperty>,
    /// Constraints.
    pub constraints: Vec<Constraint>,
    /// Stereotypes.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values.
    pub tagged_values: Vec<TaggedValue>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// An association definition linking two classes.
///
/// ```text
/// Association <<stereo>> model::Person_Firm {
///   employee: model::Person[*];
///   employer: model::Firm[*];
/// }
/// ```
#[derive(Debug, Clone, PartialEq, crate::PackageableElement)]
pub struct AssociationDef {
    /// The package.
    pub package: Option<Package>,
    /// The association name with its source location.
    pub name: SpannedString,
    /// Properties (typically exactly two).
    pub properties: Vec<Property>,
    /// Qualified properties.
    pub qualified_properties: Vec<QualifiedProperty>,
    /// Stereotypes.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values.
    pub tagged_values: Vec<TaggedValue>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A unit definition within a measure.
///
/// Can be canonical (the `*` unit) or non-canonical with a conversion function.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct UnitDef {
    /// Unit name.
    pub name: Identifier,
    /// Conversion parameter name (e.g., `x` in `x -> $x * 1000`).
    pub conversion_param: Option<Identifier>,
    /// Conversion body expression.
    pub conversion_body: Option<Expression>,
    /// From the unit name to its closing `;` (a canonical unit's `*` is
    /// outside it).
    pub source_info: SourceInfo,
}

/// A measure definition with a canonical unit and non-canonical units.
///
/// ```text
/// Measure pkg::NewMeasure {
///   *UnitOne: x -> $x;
///   UnitTwo: x -> $x * 1000;
/// }
/// ```
#[derive(Debug, Clone, PartialEq, crate::PackageableElement)]
pub struct MeasureDef {
    /// The package.
    pub package: Option<Package>,
    /// The measure name with its source location.
    pub name: SpannedString,
    /// The canonical unit (marked with `*`); `None` for a non-convertible
    /// measure, whose units have no conversion functions.
    pub canonical_unit: Option<UnitDef>,
    /// Non-canonical units.
    pub non_canonical_units: Vec<UnitDef>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// Test data for a function test: `store: data;`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct FunctionTestData {
    /// The store, e.g. `ModelStore` or `store::MyStore`.
    pub store: PackageableElementPtr,
    /// The data.
    pub data: FunctionTestDataValue,
    /// From the store to the closing `;`.
    pub source_info: SourceInfo,
}

/// The data of a function test's store.
#[derive(Debug, Clone, PartialEq)]
pub enum FunctionTestDataValue {
    /// Inline data: `(JSON) '{}'`.
    ExternalFormat(ExternalFormatValue),
    /// A data element: `testing::MyReference`.
    Reference(PackageableElementPtr),
    /// Embedded data: `Relation #{ content }#`.
    Embedded(EmbeddedDataValue),
}

/// Data in an external format: `(JSON) '{"a": 1}'`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct ExternalFormatValue {
    /// The format, e.g. `JSON` or `XML`.
    pub format: Identifier,
    /// The data, unescaped.
    pub content: String,
    /// From `(` to the end of the string.
    pub source_info: SourceInfo,
}

/// A function test: `testName 'doc' | funcName(args) => expected;`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct FunctionTestAssertion {
    /// Test name.
    pub name: Identifier,
    /// Optional documentation string for this test.
    pub doc: Option<SmolStr>,
    /// The function invocation expression.
    pub invocation: Expression,
    /// The expected result.
    pub expected: FunctionTestExpected,
    /// From the test name to the closing `;`.
    pub source_info: SourceInfo,
}

/// A function test's expected result.
#[derive(Debug, Clone, PartialEq)]
pub enum FunctionTestExpected {
    /// A primitive value or a collection of them: `'Hello'`, `[1, 2]`.
    Value(Box<Expression>),
    /// Data in an external format: `(JSON) '{}'`.
    ExternalFormat(ExternalFormatValue),
    /// A relation: `Relation #{ id, name ⏎ 1, John; }#`.
    Relation(ExpectedRelation),
    /// Embedded data of another type, kept as written: the engine accepts
    /// only relations here.
    Embedded(EmbeddedDataValue),
}

/// A test's expected relation: `Relation #{ id, name ⏎ 1, John; }#`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct ExpectedRelation {
    /// The relation: the first table of the body read as one without a path.
    /// The engine reads it from the body, trimmed, behind `: `, and positions
    /// it there, as if that text started right after `#{`.
    pub element: RelationElement,
    /// From `Relation` to `}#`.
    pub source_info: SourceInfo,
}

/// A function test suite: the tests and data of a function's test block,
/// or of one named suite within it.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct FunctionTest {
    /// Suite name; `None` for the block's own tests and data.
    pub name: Option<Identifier>,
    /// Test data (store bindings).
    pub data: Vec<FunctionTestData>,
    /// Tests.
    pub assertions: Vec<FunctionTestAssertion>,
    /// The test block `{…}`, or the named suite `Name(…)`.
    pub source_info: SourceInfo,
}

/// A function definition.
///
/// ```text
/// function <<stereo>> {tag='val'}
///   model::hello(name: String[1]): String[1]
/// {
///   'Hello ' + $name
/// }
/// ```
#[derive(Debug, Clone, PartialEq, crate::PackageableElement)]
pub struct FunctionDef {
    /// The package.
    pub package: Option<Package>,
    /// The function name with its source location.
    pub name: SpannedString,
    /// Type and multiplicity parameters, e.g. `<Z|y>`.
    pub type_and_multiplicity_parameters: Option<TypeAndMultiplicityParameters>,
    /// Parameters.
    pub parameters: Vec<Parameter>,
    /// Return type (may be a regular type or a unit reference).
    pub return_type: TypeSpec,
    /// Return multiplicity.
    pub return_multiplicity: Multiplicity,
    /// Optional constraint block: `[name : expr, ...]` between the
    /// return type and the body. Mirrors `ClassDef.constraints`.
    pub constraints: Vec<Constraint>,
    /// Body expressions.
    pub body: Vec<Expression>,
    /// Stereotypes.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values.
    pub tagged_values: Vec<TaggedValue>,
    /// Function tests.
    pub tests: Vec<FunctionTest>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A native function declaration (no body, semicolon-terminated).
///
/// ```text
/// native function meta::pure::functions::string::plus(strings: String[*]): String[1];
/// ```
#[derive(Debug, Clone, PartialEq, crate::PackageableElement)]
pub struct NativeFunctionDef {
    /// The package.
    pub package: Option<Package>,
    /// The function name with its source location.
    pub name: SpannedString,
    /// Type and multiplicity parameters, e.g. `<T|m>`.
    pub type_and_multiplicity_parameters: Option<TypeAndMultiplicityParameters>,
    /// Parameters.
    pub parameters: Vec<Parameter>,
    /// Return type.
    pub return_type: TypeSpec,
    /// Return multiplicity.
    pub return_multiplicity: Multiplicity,
    /// Stereotypes.
    pub stereotypes: Vec<StereotypePtr>,
    /// Tagged values.
    pub tagged_values: Vec<TaggedValue>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// Shared accessors for function-like elements.
///
/// Implemented by both [`FunctionDef`] and [`NativeFunctionDef`].
pub trait FunctionSignature: PackageableElement {
    /// The function parameters.
    fn parameters(&self) -> &[Parameter];
    /// The return type specification.
    fn return_type(&self) -> &TypeSpec;
    /// The return multiplicity.
    fn return_multiplicity(&self) -> &Multiplicity;

    /// Compute the mangled function name (FQN).
    ///
    /// Mirrors Java's `ConcreteFunctionDefinitionNameProcessor`:
    /// ```text
    /// funcName_ParamType_Mult__ParamType_Mult__ReturnType_Mult_
    /// ```
    ///
    /// Each parameter contributes `Type + multSig + "_"`, producing a
    /// double-underscore `__` between consecutive parameters.
    /// If there are no parameters, an extra `_` is added (`funcName__`).
    fn mangled_name(&self) -> String {
        let mut b = String::with_capacity(64);
        b.push_str(self.name());
        b.push('_');

        let params = self.parameters();
        if params.is_empty() {
            b.push('_');
        } else {
            for p in params {
                match &p.type_ref {
                    Some(TypeSpec::Type(tr)) => append_type_name(&mut b, tr),
                    Some(TypeSpec::Unit(_)) | None => b.push_str("Any"),
                    Some(TypeSpec::Relation(_)) => b.push_str("Relation"),
                    Some(TypeSpec::Function(_)) => b.push_str("Function"),
                }
                match &p.multiplicity {
                    Some(m) => append_mult(&mut b, m),
                    None => b.push_str("_MANY_"),
                }
                b.push('_');
            }
        }

        // Return type
        match self.return_type() {
            TypeSpec::Type(tr) => append_type_name(&mut b, tr),
            TypeSpec::Unit(_) => b.push_str("Any"),
            TypeSpec::Relation(_) => b.push_str("Relation"),
            TypeSpec::Function(_) => b.push_str("Function"),
        }
        append_mult(&mut b, self.return_multiplicity());

        b
    }
}

impl FunctionSignature for FunctionDef {
    fn parameters(&self) -> &[Parameter] {
        &self.parameters
    }
    fn return_type(&self) -> &TypeSpec {
        &self.return_type
    }
    fn return_multiplicity(&self) -> &Multiplicity {
        &self.return_multiplicity
    }
}

impl FunctionSignature for NativeFunctionDef {
    fn parameters(&self) -> &[Parameter] {
        &self.parameters
    }
    fn return_type(&self) -> &TypeSpec {
        &self.return_type
    }
    fn return_multiplicity(&self) -> &Multiplicity {
        &self.return_multiplicity
    }
}

/// Append the type name from a `TypeReference` to the FQN builder.
fn append_type_name(b: &mut String, tr: &TypeReference) {
    b.push_str(&tr.name);
}

/// Append the multiplicity signature to the FQN builder.
///
/// Encoding (matching Java's `Multiplicity.multiplicityToSignatureString`):
/// - `[1]` → `_1_`
/// - `[0..1]` → `_$0_1$_`
/// - `[*]` → `_MANY_`
/// - `[1..*]` → `_$1_MANY$_`
/// - `[n..m]` → `_$n_m$_`
fn append_mult(b: &mut String, m: &Multiplicity) {
    match m {
        Multiplicity::PureOne => b.push_str("_1_"),
        Multiplicity::ZeroOrOne => b.push_str("_$0_1$_"),
        Multiplicity::ZeroOrMany => b.push_str("_MANY_"),
        Multiplicity::OneOrMany => b.push_str("_$1_MANY$_"),
        Multiplicity::Variable(name) => {
            b.push('_');
            b.push_str(name);
            b.push('_');
        }
        Multiplicity::Range { lower, upper } => match upper {
            None => {
                if *lower == 0 {
                    b.push_str("_MANY_");
                } else {
                    b.push_str("_$");
                    b.push_str(&lower.to_string());
                    b.push_str("_MANY$_");
                }
            }
            Some(u) => {
                if lower == u {
                    b.push('_');
                    b.push_str(&u.to_string());
                    b.push('_');
                } else {
                    b.push_str("_$");
                    b.push_str(&lower.to_string());
                    b.push('_');
                    b.push_str(&u.to_string());
                    b.push_str("$_");
                }
            }
        },
    }
}

/// Visitor pattern for walking top-level elements.
///
/// Implement this for compiler passes, linters, protocol converters, etc.
pub trait ElementVisitor {
    /// Visit a class definition.
    fn visit_class(&mut self, class: &ClassDef);
    /// Visit an enumeration definition.
    fn visit_enum(&mut self, enum_def: &EnumDef);
    /// Visit a function definition.
    fn visit_function(&mut self, func: &FunctionDef);
    /// Visit a native function declaration.
    fn visit_native_function(&mut self, func: &NativeFunctionDef);
    /// Visit a profile definition.
    fn visit_profile(&mut self, profile: &ProfileDef);
    /// Visit an association definition.
    fn visit_association(&mut self, assoc: &AssociationDef);
    /// Visit a measure definition.
    fn visit_measure(&mut self, measure: &MeasureDef);
    /// Visit a primitive type definition.
    fn visit_primitive(&mut self, primitive: &PrimitiveDef);
    /// Visit a DSL-defined element. Default no-op so pre-DSL visitors
    /// keep compiling unchanged; DSL-aware visitors override.
    fn visit_dsl_element(&mut self, _element: &dyn crate::dsl::DSLElement) {}
}

impl Element {
    /// Accepts a visitor and dispatches to the appropriate `visit_*` method.
    pub fn accept(&self, visitor: &mut dyn ElementVisitor) {
        match self {
            Self::Class(e) => visitor.visit_class(e),
            Self::Enumeration(e) => visitor.visit_enum(e),
            Self::Function(e) => visitor.visit_function(e),
            Self::NativeFunction(e) => visitor.visit_native_function(e),
            Self::Profile(e) => visitor.visit_profile(e),
            Self::Association(e) => visitor.visit_association(e),
            Self::Measure(e) => visitor.visit_measure(e),
            Self::Primitive(e) => visitor.visit_primitive(e),
            Self::DSLElement(e) => visitor.visit_dsl_element(e.as_ref()),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::annotation::PackageableElementPtr;
    use smol_str::SmolStr;

    use crate::test_utils::src;

    #[test]
    fn test_profile_def() {
        let profile = ProfileDef {
            package: Some(Package::root(SmolStr::new("meta"), src())),
            name: SpannedString {
                value: SmolStr::new("doc"),
                source_info: src(),
            },
            stereotype_names: vec![SpannedString {
                value: SmolStr::new("deprecated"),
                source_info: src(),
            }],
            tag_names: vec![SpannedString {
                value: SmolStr::new("description"),
                source_info: src(),
            }],
            stereotypes: vec![],
            tagged_values: vec![],
            source_info: src(),
        };

        assert_eq!(profile.name(), "doc");
        assert_eq!(profile.package().unwrap().name(), "meta");
        assert_eq!(profile.stereotype_names.len(), 1);
        assert_eq!(profile.tag_names.len(), 1);
    }

    #[test]
    fn test_enum_def() {
        let enum_def = EnumDef {
            package: Some(Package::root(SmolStr::new("model"), src())),
            name: SpannedString {
                value: SmolStr::new("Color"),
                source_info: src(),
            },
            values: vec![
                EnumValue {
                    name: SmolStr::new("RED"),
                    stereotypes: vec![],
                    tagged_values: vec![],
                    source_info: src(),
                },
                EnumValue {
                    name: SmolStr::new("GREEN"),
                    stereotypes: vec![],
                    tagged_values: vec![],
                    source_info: src(),
                },
            ],
            stereotypes: vec![],
            tagged_values: vec![],
            source_info: src(),
        };

        assert_eq!(enum_def.name(), "Color");
        assert_eq!(enum_def.values.len(), 2);
        assert!(enum_def.stereotypes().is_empty());
    }

    #[test]
    fn test_class_def_with_stereotype() {
        let class = ClassDef {
            package: Some(Package::root(SmolStr::new("model"), src())),
            name: SpannedString {
                value: SmolStr::new("Person"),
                source_info: src(),
            },
            type_variable_parameters: vec![],
            type_and_multiplicity_parameters: None,
            super_types: vec![],
            properties: vec![Property {
                name: SmolStr::new("name"),
                type_ref: TypeSpec::Type(TypeReference {
                    package: None,
                    name: SmolStr::new("String"),
                    type_arguments: vec![],
                    multiplicity_arguments: vec![],
                    type_variable_values: vec![],
                    source_info: src(),
                }),
                multiplicity: Multiplicity::one(),
                aggregation: None,
                default_value: None,
                stereotypes: vec![],
                tagged_values: vec![],
                source_info: src(),
            }],
            qualified_properties: vec![],
            constraints: vec![],
            stereotypes: vec![StereotypePtr {
                profile: PackageableElementPtr {
                    package: None,
                    name: SmolStr::new("temporal"),
                    source_info: src(),
                },
                value: SmolStr::new("businesstemporal"),
                value_source_info: src(),
                source_info: src(),
            }],
            tagged_values: vec![],
            source_info: src(),
        };

        assert_eq!(class.name(), "Person");
        assert_eq!(class.properties.len(), 1);
        assert_eq!(class.stereotypes().len(), 1);
        assert_eq!(class.stereotypes()[0].value, "businesstemporal");
    }

    #[test]
    fn test_element_enum_dispatch() {
        let profile = Element::Profile(ProfileDef {
            package: None,
            name: SpannedString {
                value: SmolStr::new("doc"),
                source_info: src(),
            },
            stereotype_names: vec![],
            tag_names: vec![],
            stereotypes: vec![],
            tagged_values: vec![],
            source_info: src(),
        });

        assert_eq!(profile.source_info().start_line, 1);
        assert_eq!(profile.name(), "doc");
        assert!(profile.package().is_none());
    }

    #[test]
    fn test_visitor_dispatch() {
        struct Counter {
            classes: u32,
            profiles: u32,
        }

        impl ElementVisitor for Counter {
            fn visit_class(&mut self, _: &ClassDef) {
                self.classes += 1;
            }
            fn visit_profile(&mut self, _: &ProfileDef) {
                self.profiles += 1;
            }
            fn visit_enum(&mut self, _: &EnumDef) {}
            fn visit_function(&mut self, _: &FunctionDef) {}
            fn visit_native_function(&mut self, _: &NativeFunctionDef) {}
            fn visit_association(&mut self, _: &AssociationDef) {}
            fn visit_measure(&mut self, _: &MeasureDef) {}
            fn visit_primitive(&mut self, _: &PrimitiveDef) {}
        }

        let elements = vec![
            Element::Profile(ProfileDef {
                package: None,
                name: SpannedString {
                    value: SmolStr::new("doc"),
                    source_info: src(),
                },
                stereotype_names: vec![],
                tag_names: vec![],
                stereotypes: vec![],
                tagged_values: vec![],
                source_info: src(),
            }),
            Element::Class(ClassDef {
                package: None,
                name: SpannedString {
                    value: SmolStr::new("Person"),
                    source_info: src(),
                },
                type_variable_parameters: vec![],
                type_and_multiplicity_parameters: None,
                super_types: vec![],
                properties: vec![],
                qualified_properties: vec![],
                constraints: vec![],
                stereotypes: vec![],
                tagged_values: vec![],
                source_info: src(),
            }),
        ];

        let mut counter = Counter {
            classes: 0,
            profiles: 0,
        };

        for element in &elements {
            element.accept(&mut counter);
        }

        assert_eq!(counter.classes, 1);
        assert_eq!(counter.profiles, 1);
    }
}
