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

//! Type references, identifiers, multiplicities, and package paths.
//!
//! This module defines the core type system primitives shared across all AST nodes.

use smol_str::SmolStr;

use crate::source_info::{SourceInfo, Spanned};

/// Interned identifier — cheap to clone, compare, and hash.
///
/// Most Pure identifiers (class names, property names, keywords) fit within
/// `SmolStr`'s 24-byte inline buffer, avoiding heap allocation entirely.
pub type Identifier = SmolStr;

/// A package in the Package hierarchy, with an optional parent.
///
/// This models the recursive nature of packages: `meta::pure::profiles` is
/// represented as `Package("profiles", parent=Package("pure", parent=Package("meta")))`.
///
/// # Design Rationale
///
/// Unlike a flat `Vec<Identifier>`, this recursive structure:
/// - Naturally models parent-child relationships
/// - Makes parent traversal trivial (`package.parent()`)
/// - Enables sharing common parent prefixes via `Arc` (future optimization)
///
/// # Examples
///
/// ```
/// use legend_pure_parser_ast::type_ref::Package;
/// use legend_pure_parser_ast::SourceInfo;
/// use smol_str::SmolStr;
///
/// let src = SourceInfo::new("test.pure", 1, 1, 1, 20);
/// let meta = Package::new(SmolStr::new("meta"), None, src.clone());
/// let pure = Package::new(SmolStr::new("pure"), Some(Box::new(meta)), src.clone());
/// let profiles = Package::new(SmolStr::new("profiles"), Some(Box::new(pure)), src);
///
/// assert_eq!(profiles.to_string(), "meta::pure::profiles");
/// assert_eq!(profiles.parent().unwrap().name(), "pure");
/// ```
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Package {
    /// The name of this package segment.
    name: Identifier,
    /// The parent package, or `None` for root-level packages.
    parent: Option<Box<Package>>,
    /// Source location of this package reference.
    source_info: SourceInfo,
}

impl Package {
    /// Creates a new package with an optional parent.
    #[must_use]
    pub fn new(name: Identifier, parent: Option<Box<Package>>, source_info: SourceInfo) -> Self {
        Self {
            name,
            parent,
            source_info,
        }
    }

    /// Creates a root package (no parent).
    #[must_use]
    pub fn root(name: Identifier, source_info: SourceInfo) -> Self {
        Self {
            name,
            parent: None,
            source_info,
        }
    }

    /// Creates a child package under `self`.
    #[must_use]
    pub fn child(self, name: Identifier, source_info: SourceInfo) -> Self {
        Self {
            name,
            parent: Some(Box::new(self)),
            source_info,
        }
    }

    /// Returns the name of this package segment.
    #[must_use]
    pub fn name(&self) -> &Identifier {
        &self.name
    }

    /// Returns the parent package, if any.
    #[must_use]
    pub fn parent(&self) -> Option<&Package> {
        self.parent.as_deref()
    }

    /// Returns the depth of this package (0 for root).
    #[must_use]
    pub fn depth(&self) -> usize {
        match &self.parent {
            None => 0,
            Some(p) => 1 + p.depth(),
        }
    }

    /// Collects all segments from root to self as a vector of identifiers.
    #[must_use]
    pub fn segments(&self) -> Vec<&Identifier> {
        let mut parts = Vec::with_capacity(self.depth() + 1);
        self.collect_segments(&mut parts);
        parts
    }

    fn collect_segments<'a>(&'a self, parts: &mut Vec<&'a Identifier>) {
        if let Some(parent) = &self.parent {
            parent.collect_segments(parts);
        }
        parts.push(&self.name);
    }
}

impl std::fmt::Display for Package {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        if let Some(parent) = &self.parent {
            write!(f, "{parent}::")?;
        }
        write!(f, "{}", self.name)
    }
}

impl Spanned for Package {
    fn source_info(&self) -> &SourceInfo {
        &self.source_info
    }
}

/// Multiplicity specification for properties and parameters.
///
/// Uses an enum with common well-known multiplicities for ergonomics and pattern
/// matching, plus a `Range` variant for arbitrary bounds.
///
/// The internal representation is opaque — construction is via factory functions,
/// and access to bounds is via the [`HasMultiplicity`] trait.
///
/// # Examples
///
/// ```
/// use legend_pure_parser_ast::type_ref::{Multiplicity, HasMultiplicity};
///
/// let exactly_one = Multiplicity::one();
/// assert_eq!(exactly_one.lower(), 1);
/// assert_eq!(exactly_one.upper(), Some(1));
///
/// let optional = Multiplicity::zero_or_one();
/// assert_eq!(optional.lower(), 0);
/// assert_eq!(optional.upper(), Some(1));
///
/// let many = Multiplicity::zero_or_many();
/// assert_eq!(many.lower(), 0);
/// assert!(many.upper().is_none());
/// ```
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Multiplicity {
    /// Exactly one: `[1]` — `lower=1, upper=1`.
    PureOne,
    /// Zero or one: `[0..1]` — `lower=0, upper=1`.
    ZeroOrOne,
    /// Zero or more: `[*]` — `lower=0, upper=None`.
    ZeroOrMany,
    /// One or more: `[1..*]` — `lower=1, upper=None`.
    OneOrMany,
    /// Arbitrary range: `[lower..upper]`.
    Range {
        /// Lower bound (inclusive).
        lower: u32,
        /// Upper bound (inclusive), `None` = unbounded.
        upper: Option<u32>,
    },
    /// A named multiplicity variable: `[m]` where `m` is declared in `<T|m>`.
    ///
    /// Has no concrete bounds at parse time — the compiler resolves it by
    /// binding it to the corresponding multiplicity parameter declaration.
    Variable(Identifier),
}

/// Trait for accessing multiplicity bounds.
///
/// This abstracts over the internal representation of [`Multiplicity`],
/// allowing pattern-matching on common cases while still supporting arbitrary ranges.
pub trait HasMultiplicity {
    /// Returns the lower bound (inclusive).
    fn lower(&self) -> u32;
    /// Returns the upper bound (inclusive), or `None` for unbounded (`*`).
    fn upper(&self) -> Option<u32>;
    /// Returns `true` if this multiplicity allows zero elements.
    fn is_optional(&self) -> bool {
        self.lower() == 0
    }
    /// Returns `true` if this multiplicity is unbounded.
    fn is_many(&self) -> bool {
        self.upper().is_none()
    }
    /// Returns `true` if this is exactly `[1]`.
    fn is_required_single(&self) -> bool {
        self.lower() == 1 && self.upper() == Some(1)
    }
}

impl Multiplicity {
    /// Creates `[1]` — exactly one.
    #[must_use]
    pub fn one() -> Self {
        Self::PureOne
    }

    /// Creates `[0..1]` — zero or one (optional).
    #[must_use]
    pub fn zero_or_one() -> Self {
        Self::ZeroOrOne
    }

    /// Creates `[*]` — zero or more.
    #[must_use]
    pub fn zero_or_many() -> Self {
        Self::ZeroOrMany
    }

    /// Creates `[1..*]` — one or more.
    #[must_use]
    pub fn one_or_many() -> Self {
        Self::OneOrMany
    }

    /// Creates an arbitrary multiplicity `[lower..upper]`.
    ///
    /// Returns a well-known variant if the range matches one.
    #[must_use]
    pub fn range(lower: u32, upper: Option<u32>) -> Self {
        match (lower, upper) {
            (1, Some(1)) => Self::PureOne,
            (0, Some(1)) => Self::ZeroOrOne,
            (0, None) => Self::ZeroOrMany,
            (1, None) => Self::OneOrMany,
            _ => Self::Range { lower, upper },
        }
    }
}

impl HasMultiplicity for Multiplicity {
    fn lower(&self) -> u32 {
        match self {
            Self::PureOne | Self::OneOrMany => 1,
            Self::ZeroOrOne | Self::ZeroOrMany | Self::Variable(_) => 0,
            Self::Range { lower, .. } => *lower,
        }
    }

    fn upper(&self) -> Option<u32> {
        match self {
            Self::ZeroOrOne | Self::PureOne => Some(1),
            Self::ZeroOrMany | Self::OneOrMany | Self::Variable(_) => None,
            Self::Range { upper, .. } => *upper,
        }
    }
}

impl std::fmt::Display for Multiplicity {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::PureOne => write!(f, "[1]"),
            Self::ZeroOrOne => write!(f, "[0..1]"),
            Self::ZeroOrMany => write!(f, "[*]"),
            Self::OneOrMany => write!(f, "[1..*]"),
            Self::Range {
                lower,
                upper: Some(u),
            } => {
                if lower == u {
                    write!(f, "[{lower}]")
                } else {
                    write!(f, "[{lower}..{u}]")
                }
            }
            Self::Range { lower, upper: None } => write!(f, "[{lower}..*]"),
            Self::Variable(name) => write!(f, "[{name}]"),
        }
    }
}

/// A multiplicity argument inside a generic type's angle brackets.
///
/// Multiplicity arguments appear after a `|` in type argument lists:
/// `Function<Z|y>` — `Z` is a type argument, `y` is a multiplicity argument.
///
/// A multiplicity argument can be either a named variable (on both declaration
/// and usage sites) or a concrete multiplicity value (only on usage sites):
/// - `<T|m>` → `Identifier("m")` (variable)
/// - `<T|1>` → `Concrete(Multiplicity::one())` (concrete)
/// - `<T|*>` → `Concrete(Multiplicity::zero_or_many())` (concrete)
/// - `<T|0..1>` → `Concrete(Multiplicity::zero_or_one())` (concrete)
#[derive(Debug, Clone, PartialEq)]
pub enum MultiplicityArgument {
    /// A multiplicity variable: `y` in `<T|y>`.
    Identifier(Identifier, SourceInfo),
    /// A concrete multiplicity: `1` in `<T|1>`, `*` in `<T|*>`.
    Concrete(Multiplicity, SourceInfo),
}

impl Spanned for MultiplicityArgument {
    fn source_info(&self) -> &SourceInfo {
        match self {
            Self::Identifier(_, s) | Self::Concrete(_, s) => s,
        }
    }
}

impl std::fmt::Display for MultiplicityArgument {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Identifier(id, _) => write!(f, "{id}"),
            Self::Concrete(m, _) => {
                // Display without brackets: "1", "*", "0..1"
                match m {
                    Multiplicity::PureOne => write!(f, "1"),
                    Multiplicity::ZeroOrOne => write!(f, "0..1"),
                    Multiplicity::ZeroOrMany => write!(f, "*"),
                    Multiplicity::OneOrMany => write!(f, "1..*"),
                    Multiplicity::Range {
                        lower,
                        upper: Some(u),
                    } => {
                        if lower == u {
                            write!(f, "{lower}")
                        } else {
                            write!(f, "{lower}..{u}")
                        }
                    }
                    Multiplicity::Range { lower, upper: None } => write!(f, "{lower}..*"),
                    Multiplicity::Variable(name) => write!(f, "{name}"),
                }
            }
        }
    }
}

/// A type variable parameter declaration on a Class or Primitive.
///
/// e.g. `x:Integer[1]` in `Class Foo(x:Integer[1])`.
/// Structurally identical to `Parameter` but used for type-level constants.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct TypeVariableParameter {
    /// Variable name.
    pub name: Identifier,
    /// Parameter type.
    pub type_ref: TypeReference,
    /// Parameter multiplicity.
    pub multiplicity: Multiplicity,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A reference to a type, including optional type arguments and type variable values.
///
/// Every `TypeReference` is [`Spanned`] to enable precise error reporting.
///
/// # Examples
///
/// - `String` → name only
/// - `meta::pure::String` → package + name
/// - `Result<String>` → name + type arguments
/// - `VARCHAR(200)` → name + type variable values
/// - `Relation<(a:Integer, b:String)>` → name + a relation type argument
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct TypeReference {
    /// The package path, e.g., `meta::pure`. `None` for unqualified types.
    pub package: Option<Package>,
    /// The type name, e.g., `String`, `Result`, `Map`.
    pub name: Identifier,
    /// Generic type arguments: `<String, (a:Integer)>`.
    pub type_arguments: Vec<TypeArgument>,
    /// Multiplicity arguments: `y` in `<Z|y>`, `1` in `<T|1>`.
    ///
    /// These appear after a `|` separator inside `<...>` on type references.
    /// Empty for types without multiplicity arguments.
    pub multiplicity_arguments: Vec<MultiplicityArgument>,
    /// Type variable values: `(200, 'ok')`.
    pub type_variable_values: Vec<TypeVariableValue>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A type argument: any type (`String`, `(a:Integer)`, `{->T[1]}`), with,
/// in the Legend Pure grammar (`typeWithOperation`), a binding `Z=T`,
/// relation algebra `T - Z + V` and a bound `X⊆T`. The engine grammar has
/// the type alone.
#[derive(Debug, Clone, PartialEq)]
pub struct TypeArgument {
    /// The type; for `Z=T`, the bound type `T`.
    pub type_spec: TypeSpec,
    /// `Z` of `Z=T`.
    pub equal_binding: Option<Identifier>,
    /// Relation type-algebra steps, in source order: `T - Z + V` →
    /// `[Difference(Z), Union(V)]` (left-associative, as Java's
    /// `GenericTypeOperation`).
    pub algebra_ops: Vec<AlgebraOp>,
    /// Subtype-constraint bound `X⊆T` (U+2286): the relation `X` must be a
    /// subset of `T`, e.g. `ColSpec<(?:K)⊆T>`.
    pub subset_bound: Option<Box<TypeSpec>>,
}

impl TypeArgument {
    /// A plain type argument, without binding, algebra or bound.
    #[must_use]
    pub fn new(type_spec: TypeSpec) -> Self {
        Self {
            type_spec,
            equal_binding: None,
            algebra_ops: vec![],
            subset_bound: None,
        }
    }
}

impl Spanned for TypeArgument {
    fn source_info(&self) -> &SourceInfo {
        self.type_spec.source_info()
    }
}

/// One step in a relation type-algebra chain attached to a
/// [`TypeReference`]: `+ operand` (union) or `- operand` (difference).
/// Accumulated left-to-right (`(T - Z) + V`), mirroring Java's nested
/// `GenericTypeOperation`.
#[derive(Debug, Clone, PartialEq)]
pub struct AlgebraOp {
    /// Union (`+`) or difference (`-`).
    pub kind: AlgebraOpKind,
    /// The right-hand operand of the operator.
    pub operand: TypeSpec,
}

/// The operator of an [`AlgebraOp`].
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AlgebraOpKind {
    /// `+` — append the operand relation's columns.
    Union,
    /// `-` — remove the operand relation's columns (by name).
    Difference,
}

impl TypeReference {
    /// Returns the fully qualified name as a string (e.g., `meta::pure::String`).
    #[must_use]
    pub fn full_path(&self) -> String {
        if let Some(pkg) = &self.package {
            format!("{pkg}::{}", self.name)
        } else {
            self.name.to_string()
        }
    }
}

/// A reference to a unit of a Measure type: `NewMeasure~UnitOne`.
///
/// Corresponds to Java grammar rule `unitName: qualifiedName TILDE identifier`.
/// The measure is referenced as a [`TypeReference`], and the unit is a separate
/// identifier.
///
/// # Examples
///
/// - `NewMeasure~UnitOne` → measure `NewMeasure`, unit `UnitOne`
/// - `pkg::Measure~Kg` → measure `pkg::Measure`, unit `Kg`
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct UnitReference {
    /// The measure type being referenced.
    pub measure: TypeReference,
    /// The unit name within the measure.
    pub unit: Identifier,
    /// The whole reference, `Measure~Unit`.
    pub source_info: SourceInfo,
}

/// A single column in a relation type: `name:Type[mult]`.
///
/// # Examples
///
/// - `a:Integer` → name `a`, type `Integer`, no multiplicity
/// - `name:String[1]` → name `name`, type `String`, multiplicity `[1]`
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct RelationColumn {
    /// The column name.
    pub name: Identifier,
    /// The column type.
    pub type_ref: TypeSpec,
    /// Optional multiplicity (e.g., `[1]`, `[*]`).
    pub multiplicity: Option<Multiplicity>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A relation type: `(a:Integer, b:String)`.
///
/// This is a structural type representing a set of named, typed columns.
/// It can appear as a standalone type (`(a:Integer, b:String)`) or
/// wrapped (`Relation<(a:Integer, b:String)>`).
///
/// # Examples
///
/// ```text
/// (a:Integer, b:String)
/// (name:String[1], age:Integer[0..1])
/// ```
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct RelationType {
    /// The columns in this relation type.
    pub columns: Vec<RelationColumn>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A function type: `{ParamType[mult], ... -> ReturnType[mult]}`.
///
/// Function types appear inside type arguments in higher-order signatures:
/// ```text
/// f:Function<{Function<{->Z[y]}>[1]->Z[y]}>[1]
/// ```
///
/// # Examples
///
/// - `{->String[1]}` — no parameters, returns `String[1]`
/// - `{Integer[1]->Boolean[1]}` — takes `Integer[1]`, returns `Boolean[1]`
/// - `{String[1], Integer[1]->Boolean[1]}` — two parameters
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct FunctionType {
    /// Parameter types, each with a type reference and multiplicity.
    pub parameters: Vec<FunctionTypeParameter>,
    /// Return type.
    pub return_type: Box<TypeSpec>,
    /// Return multiplicity.
    pub return_multiplicity: Multiplicity,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A parameter in a function type: `Type[mult]`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct FunctionTypeParameter {
    /// Parameter type.
    pub type_ref: TypeSpec,
    /// Parameter multiplicity.
    pub multiplicity: Multiplicity,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A type specification that can be a type, a unit reference, relation type,
/// or function type.
///
/// Used in positions where the Pure grammar accepts type references,
/// unit names, relation types, or function types.
#[derive(Debug, Clone, PartialEq)]
pub enum TypeSpec {
    /// A regular type reference: `String`, `Map<K, V>`.
    Type(TypeReference),
    /// A unit reference: `NewMeasure~UnitOne`.
    Unit(UnitReference),
    /// A relation type: `(a:Integer, b:String)`.
    Relation(RelationType),
    /// A function type: `{Integer[1] -> Boolean[1]}`.
    Function(FunctionType),
}

impl TypeSpec {
    /// Returns the underlying `TypeReference` (for units, returns the measure type).
    ///
    /// # Panics
    ///
    /// Panics if called on a `Relation` or `Function` variant.
    #[must_use]
    pub fn type_ref(&self) -> &TypeReference {
        match self {
            Self::Type(tr) => tr,
            Self::Unit(ur) => &ur.measure,
            Self::Relation(_) => {
                unreachable!("type_ref() called on TypeSpec::Relation — use pattern matching")
            }
            Self::Function(_) => {
                unreachable!("type_ref() called on TypeSpec::Function — use pattern matching")
            }
        }
    }

    /// Returns the fully qualified name as a string.
    #[must_use]
    pub fn full_path(&self) -> String {
        match self {
            Self::Type(tr) => tr.full_path(),
            Self::Unit(ur) => format!("{}~{}", ur.measure.full_path(), ur.unit),
            Self::Relation(_) => "<RelationType>".to_string(),
            Self::Function(_) => "<FunctionType>".to_string(),
        }
    }
}

impl Spanned for TypeSpec {
    fn source_info(&self) -> &SourceInfo {
        match self {
            Self::Type(tr) => &tr.source_info,
            Self::Unit(ur) => &ur.source_info,
            Self::Relation(r) => &r.source_info,
            Self::Function(f) => &f.source_info,
        }
    }
}

/// A value in a type variable position, e.g., `VARCHAR(200)` or `Res(1, 'ok')`.
#[derive(Debug, Clone, PartialEq)]
pub enum TypeVariableValue {
    /// Integer value, e.g., `200` in `VARCHAR(200)`.
    Integer(i64, SourceInfo),
    /// String value, e.g., `'ok'` in `Res('ok')`.
    String(String, SourceInfo),
}

impl Spanned for TypeVariableValue {
    fn source_info(&self) -> &SourceInfo {
        match self {
            Self::Integer(_, s) | Self::String(_, s) => s,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn test_src() -> SourceInfo {
        SourceInfo::new("test.pure", 1, 1, 1, 10)
    }

    // -- Package tests --

    #[test]
    fn test_root_package() {
        let pkg = Package::root(SmolStr::new("meta"), test_src());
        assert_eq!(pkg.to_string(), "meta");
        assert!(pkg.parent().is_none());
        assert_eq!(pkg.depth(), 0);
    }

    #[test]
    fn test_nested_package() {
        let pkg = Package::root(SmolStr::new("meta"), test_src())
            .child(SmolStr::new("pure"), test_src())
            .child(SmolStr::new("profiles"), test_src());

        assert_eq!(pkg.to_string(), "meta::pure::profiles");
        assert_eq!(pkg.depth(), 2);
        assert_eq!(pkg.parent().unwrap().name(), "pure");
        assert_eq!(pkg.parent().unwrap().parent().unwrap().name(), "meta");
    }

    #[test]
    fn test_package_segments() {
        let pkg = Package::root(SmolStr::new("model"), test_src())
            .child(SmolStr::new("domain"), test_src())
            .child(SmolStr::new("Person"), test_src());

        let segments: Vec<&str> = pkg.segments().iter().map(|s| s.as_str()).collect();
        assert_eq!(segments, vec!["model", "domain", "Person"]);
    }

    #[test]
    fn test_package_is_spanned() {
        let pkg = Package::root(
            SmolStr::new("meta"),
            SourceInfo::new("file.pure", 3, 5, 3, 9),
        );
        assert_eq!(pkg.source_info().start_line, 3);
        assert_eq!(pkg.source_info().start_column, 5);
    }

    // -- Multiplicity tests --

    #[test]
    fn test_multiplicity_pure_one() {
        let m = Multiplicity::one();
        assert_eq!(m.lower(), 1);
        assert_eq!(m.upper(), Some(1));
        assert!(m.is_required_single());
        assert!(!m.is_optional());
        assert!(!m.is_many());
        assert_eq!(m.to_string(), "[1]");
    }

    #[test]
    fn test_multiplicity_zero_or_one() {
        let m = Multiplicity::zero_or_one();
        assert_eq!(m.lower(), 0);
        assert_eq!(m.upper(), Some(1));
        assert!(m.is_optional());
        assert!(!m.is_many());
        assert_eq!(m.to_string(), "[0..1]");
    }

    #[test]
    fn test_multiplicity_zero_or_many() {
        let m = Multiplicity::zero_or_many();
        assert_eq!(m.lower(), 0);
        assert!(m.upper().is_none());
        assert!(m.is_optional());
        assert!(m.is_many());
        assert_eq!(m.to_string(), "[*]");
    }

    #[test]
    fn test_multiplicity_one_or_many() {
        let m = Multiplicity::one_or_many();
        assert_eq!(m.lower(), 1);
        assert!(m.upper().is_none());
        assert!(!m.is_optional());
        assert!(m.is_many());
        assert_eq!(m.to_string(), "[1..*]");
    }

    #[test]
    fn test_multiplicity_range_normalizes() {
        // range(1, Some(1)) should normalize to PureOne
        let m = Multiplicity::range(1, Some(1));
        assert!(matches!(m, Multiplicity::PureOne));

        let m = Multiplicity::range(0, None);
        assert!(matches!(m, Multiplicity::ZeroOrMany));
    }

    #[test]
    fn test_multiplicity_custom_range() {
        let m = Multiplicity::range(2, Some(5));
        assert_eq!(m.lower(), 2);
        assert_eq!(m.upper(), Some(5));
        assert!(!m.is_optional());
        assert!(!m.is_many());
        assert_eq!(m.to_string(), "[2..5]");
    }

    #[test]
    fn test_multiplicity_same_bound_display() {
        let m = Multiplicity::range(3, Some(3));
        assert_eq!(m.to_string(), "[3]");
    }

    // -- TypeReference tests --

    #[test]
    fn test_type_reference_simple() {
        let type_ref = TypeReference {
            package: None,
            name: SmolStr::new("String"),
            type_arguments: vec![],
            multiplicity_arguments: vec![],
            type_variable_values: vec![],
            source_info: test_src(),
        };
        assert_eq!(type_ref.full_path(), "String");
        assert!(type_ref.type_arguments.is_empty());
    }

    #[test]
    fn test_type_reference_qualified() {
        let type_ref = TypeReference {
            package: Some(
                Package::root(SmolStr::new("meta"), test_src())
                    .child(SmolStr::new("pure"), test_src()),
            ),
            name: SmolStr::new("String"),
            type_arguments: vec![],
            multiplicity_arguments: vec![],
            type_variable_values: vec![],
            source_info: test_src(),
        };
        assert_eq!(type_ref.full_path(), "meta::pure::String");
    }

    #[test]
    fn test_type_reference_is_spanned() {
        let type_ref = TypeReference {
            package: None,
            name: SmolStr::new("String"),
            type_arguments: vec![],
            multiplicity_arguments: vec![],
            type_variable_values: vec![],
            source_info: SourceInfo::new("file.pure", 5, 3, 5, 9),
        };
        assert_eq!(type_ref.source_info().start_line, 5);
    }

    #[test]
    fn test_type_variable_value_is_spanned() {
        let val = TypeVariableValue::Integer(200, SourceInfo::new("f.pure", 1, 10, 1, 13));
        assert_eq!(val.source_info().start_column, 10);

        let val =
            TypeVariableValue::String("ok".to_string(), SourceInfo::new("f.pure", 2, 5, 2, 9));
        assert_eq!(val.source_info().start_line, 2);
    }
}
