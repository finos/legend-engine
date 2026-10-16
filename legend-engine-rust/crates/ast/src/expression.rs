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

//! Expression and value specification types.
//!
//! ## Design Decisions
//!
//! - **`Literal` enum**: All literal types grouped under a single enum so callers
//!   can match on "is this a literal?" without enumerating every type.
//! - **`MemberAccess` enum**: Simple (`$x.name`) vs qualified (`$x.derived('arg')`)
//!   access are separate variants for type-safe handling.
//! - **Unified dot-access**: Both property access and enum value access parse
//!   identically (`expr.identifier`); disambiguation is semantic.
//! - **No `Cast`/`InstanceOf`/`If` nodes**: These are ordinary function/arrow calls.
//! - **Island grammars**: `GraphFetchTree` etc. live in `Expression::Island`.
//! - **Bitwise operators**: Supported from day one.

use crate::annotation::{PackageableElementPtr, Parameter};
use crate::island::IslandExpression;
use crate::source_info::{SourceInfo, Spanned};
use crate::type_ref::{Identifier, Multiplicity, TypeReference};

/// An expression in the Pure grammar.
///
/// This is a recursive type — expressions contain sub-expressions.
/// All variants carry [`SourceInfo`] for precise error reporting.
#[derive(Debug, Clone, PartialEq)]
pub enum Expression {
    // -- Literals --
    /// Any literal value (integer, float, string, boolean, date, etc.).
    Literal(Literal),

    // -- Variables --
    /// Variable reference: `$name`.
    Variable(Variable),

    // -- Operators --
    /// Arithmetic: `+`, `-`, `*`, `/`.
    Arithmetic(ArithmeticExpr),
    /// Comparison: `==`, `!=`, `<`, `<=`, `>`, `>=`.
    Comparison(ComparisonExpr),
    /// Logical: `&&`, `||`.
    Logical(LogicalExpr),
    /// Bitwise: `&`, `|`, `^`, `<<`, `>>`.
    Bitwise(BitwiseExpr),
    /// Unary not: `!expr`.
    Not(NotExpr),
    /// Unary minus: `-expr`.
    UnaryMinus(UnaryMinusExpr),
    /// Bitwise complement: `~expr`.
    BitwiseNot(BitwiseNotExpr),

    // -- Function & member access --
    /// Function application: `func(args)` or `pkg::func(args)`.
    FunctionApplication(FunctionApplication),
    /// Arrow (collection) function: `x->filter(...)`.
    ArrowFunction(ArrowFunction),
    /// Member access (dot): `$x.name` or `$x.derived('arg')`.
    MemberAccess(MemberAccess),
    /// Bare packageable element reference: `String`, `my::Enum`, `MyClass`.
    ///
    /// Distinguished from `FunctionApplication` by having no argument list.
    /// Java grammar: `instanceReference: qualifiedName` (without `allOrFunction`).
    PackageableElementRef(PackageableElementRef),
    /// All instances of a class: `Person.all()`, `Person.all(%latest)`,
    /// `Person.allVersions()`, `Person.allVersionsInRange(%2020, %2021)`.
    ///
    /// Java grammar: `instanceReference: qualifiedName allOrFunction`.
    AllInstances(AllInstancesExpr),

    // -- Type reference --
    /// Type reference expression: `@MyType`.
    TypeReferenceExpr(TypeReferenceExpr),

    /// Bare multiplicity literal: `@[1]`, `@[0..1]`, `@[1..*]`, `@[*]`,
    /// `@[m]` (where `m` is a declared multiplicity parameter).
    ///
    /// Sibling to `TypeReferenceExpr` — Java grammar
    /// `M3CoreParser.g4:311 AT (type | multiplicity)`. Used primarily as
    /// the second argument to `toMultiplicity<T|z>(...,Any[z])` where the
    /// literal's multiplicity binds `z` at the call site.
    MultiplicityReferenceExpr(MultiplicityReferenceExpr),

    // -- Complex expressions --
    /// Lambda: `{x: String[1] | $x + 'hello'}` or `x | $x + 1`.
    Lambda(Lambda),
    /// Let binding: `let x = expr; ...`.
    Let(LetExpr),
    /// Collection literal: `[1, 2, 3]`.
    Collection(CollectionExpr),
    /// Slice expression: `[0:5]`, `[0:5:1]`, `[:5]`.
    Slice(SliceExpr),
    /// New instance: `^MyClass(name='John')`.
    NewInstance(NewInstanceExpr),
    /// Copy with overrides: `^$var(prop='new value')`.
    Copy(CopyExpr),
    /// Unit instance: `5 RomanLength~Pes` — a number with a unit.
    UnitInstance(UnitInstanceExpr),
    /// A unit as a value: `RomanLength~Pes`, as in `newUnit(RomanLength~Pes, 5)`.
    ///
    /// Java grammar: `instanceReference: unitName` (without `allOrFunction`).
    UnitType(UnitTypeRef),

    // -- Column specification (TDS) --
    /// Column builder expression: `~col` or `~[col1, col2]`.
    Column(ColumnBuilderExpr),

    // -- Island grammar --
    /// Island grammar expression: `#tag{ content }#`.
    ///
    /// Includes graph fetch trees (`#{}#`), path expressions (`#>{}#`),
    /// and other extensible island grammars.
    Island(IslandExpression),

    // -- Navigation path --
    /// Navigation path expression: `#/Type/prop1/prop2(args)/prop3!alias#`.
    ///
    /// Compiles to an instance of `meta::pure::metamodel::path::Path<U,V|m>`,
    /// where `U` is the start type, `V` is the type of the last property in
    /// the chain, and `m` is the combined multiplicity of all property steps.
    NavigationPath(NavigationPath),

    // -- Grouping --
    /// Explicit parenthesized grouping: `(expr)`.
    ///
    /// Preserves source parentheses for faithful roundtripping. Semantically
    /// transparent — the inner expression is the value.
    Group(Box<Expression>),
}

impl Spanned for Expression {
    fn source_info(&self) -> &SourceInfo {
        match self {
            Self::Literal(e) => e.source_info(),
            Self::Variable(e) => &e.source_info,
            Self::Arithmetic(e) => &e.source_info,
            Self::Comparison(e) => &e.source_info,
            Self::Logical(e) => &e.source_info,
            Self::Bitwise(e) => &e.source_info,
            Self::Not(e) => &e.source_info,
            Self::UnaryMinus(e) => &e.source_info,
            Self::BitwiseNot(e) => &e.source_info,
            Self::FunctionApplication(e) => &e.source_info,
            Self::ArrowFunction(e) => &e.source_info,
            Self::MemberAccess(e) => e.source_info(),
            Self::PackageableElementRef(e) => &e.source_info,
            Self::AllInstances(e) => &e.source_info,
            Self::TypeReferenceExpr(e) => &e.source_info,
            Self::MultiplicityReferenceExpr(e) => &e.source_info,
            Self::Lambda(e) => &e.source_info,
            Self::Let(e) => &e.source_info,
            Self::Collection(e) => &e.source_info,
            Self::Slice(e) => &e.source_info,
            Self::NewInstance(e) => &e.source_info,
            Self::Copy(e) => &e.source_info,
            Self::Column(e) => e.source_info(),
            Self::Island(e) => &e.source_info,
            Self::NavigationPath(e) => &e.source_info,
            Self::UnitInstance(e) => &e.source_info,
            Self::UnitType(e) => &e.source_info,
            Self::Group(e) => e.source_info(),
        }
    }
}

/// All literal types grouped together.
///
/// Enables pattern matching on "is this any literal?" without enumerating
/// every literal kind in calling code.
#[derive(Debug, Clone, PartialEq)]
pub enum Literal {
    /// Integer literal: `42`, `-1`.
    Integer(IntegerLiteral),
    /// Float literal: `3.14`, `-0.5`.
    Float(FloatLiteral),
    /// Decimal literal: `3.14D`.
    Decimal(DecimalLiteral),
    /// String literal: `'hello'`.
    String(StringLiteral),
    /// Boolean literal: `true`, `false`.
    Boolean(BooleanLiteral),
    /// Strict date literal: `%2024-01-15`.
    StrictDate(StrictDateLiteral),
    /// Date-time literal: `%2024-01-15T10:30:00`.
    DateTime(DateTimeLiteral),
    /// Strict time literal: `%10:30:00`.
    StrictTime(StrictTimeLiteral),
    /// The milestoning date `%latest`.
    LatestDate(LatestDateLiteral),
    /// Bytes: `toBytes('text')`, the text's in UTF-8, which only the
    /// engine's primitive values (a test's parameters and expected values)
    /// write.
    ByteArray(ByteArrayLiteral),
}

impl Spanned for Literal {
    fn source_info(&self) -> &SourceInfo {
        match self {
            Self::Integer(e) => &e.source_info,
            Self::Float(e) => &e.source_info,
            Self::Decimal(e) => &e.source_info,
            Self::String(e) => &e.source_info,
            Self::Boolean(e) => &e.source_info,
            Self::StrictDate(e) => &e.source_info,
            Self::DateTime(e) => &e.source_info,
            Self::StrictTime(e) => &e.source_info,
            Self::LatestDate(e) => &e.source_info,
            Self::ByteArray(e) => &e.source_info,
        }
    }
}

/// Integer literal: `42`.
#[derive(Debug, Clone, PartialEq, Eq, crate::Spanned)]
pub struct IntegerLiteral {
    /// The integer value.
    pub value: i64,
    /// Source location.
    pub source_info: SourceInfo,
}

/// Float literal: `3.14`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct FloatLiteral {
    /// The float value.
    pub value: f64,
    /// Source location.
    pub source_info: SourceInfo,
}

/// Decimal literal: `3.14D`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct DecimalLiteral {
    /// The raw decimal string (preserves precision beyond f64).
    pub value: String,
    /// Source location.
    pub source_info: SourceInfo,
}

/// String literal: `'hello'`.
#[derive(Debug, Clone, PartialEq, Eq, crate::Spanned)]
pub struct StringLiteral {
    /// The string value (unescaped).
    pub value: smol_str::SmolStr,
    /// Written as a text block (`'''…'''`): the protocol's `multiLine`.
    pub multi_line: bool,
    /// Source location.
    pub source_info: SourceInfo,
}

/// Boolean literal: `true` or `false`.
#[derive(Debug, Clone, PartialEq, Eq, crate::Spanned)]
pub struct BooleanLiteral {
    /// The boolean value.
    pub value: bool,
    /// Source location.
    pub source_info: SourceInfo,
}

/// Strict date literal: `%2024-01-15`.
#[derive(Debug, Clone, PartialEq, Eq, crate::Spanned)]
pub struct StrictDateLiteral {
    /// The raw date string.
    pub value: String,
    /// Source location.
    pub source_info: SourceInfo,
}

/// Bytes: `toBytes('text')`.
#[derive(Debug, Clone, PartialEq, Eq, crate::Spanned)]
pub struct ByteArrayLiteral {
    /// The text, unescaped, whose UTF-8 bytes these are.
    pub value: smol_str::SmolStr,
    /// From `toBytes` to `)`.
    pub source_info: SourceInfo,
}

/// The milestoning date `%latest`: the latest version of milestoned data.
#[derive(Debug, Clone, PartialEq, Eq, crate::Spanned)]
pub struct LatestDateLiteral {
    /// Source location.
    pub source_info: SourceInfo,
}

/// Date-time literal: `%2024-01-15T10:30:00`.
#[derive(Debug, Clone, PartialEq, Eq, crate::Spanned)]
pub struct DateTimeLiteral {
    /// The raw datetime string.
    pub value: String,
    /// Source location.
    pub source_info: SourceInfo,
}

/// Strict time literal: `%10:30:00`.
#[derive(Debug, Clone, PartialEq, Eq, crate::Spanned)]
pub struct StrictTimeLiteral {
    /// The raw time string.
    pub value: String,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A variable reference: `$name`.
#[derive(Debug, Clone, PartialEq, Eq, crate::Spanned)]
pub struct Variable {
    /// Variable name (without the `$` prefix).
    pub name: Identifier,
    /// Source location.
    pub source_info: SourceInfo,
}

/// Arithmetic operator.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ArithmeticOp {
    /// `+`
    Plus,
    /// `-`
    Minus,
    /// `*`
    Times,
    /// `/`
    Divide,
}

/// An arithmetic expression: `left op right`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct ArithmeticExpr {
    /// Left operand.
    pub left: Box<Expression>,
    /// The operator.
    pub op: ArithmeticOp,
    /// Right operand.
    pub right: Box<Expression>,
    /// Full extent of the left operand, first token to last (parentheses
    /// included).
    pub left_span: SourceInfo,
    /// Full extent of the right operand, first token to last (parentheses
    /// included).
    pub right_span: SourceInfo,
    /// The operator token.
    pub source_info: SourceInfo,
}

/// Comparison operator.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ComparisonOp {
    /// `==`
    Equal,
    /// `!=`
    NotEqual,
    /// `<`
    LessThan,
    /// `<=`
    LessThanOrEqual,
    /// `>`
    GreaterThan,
    /// `>=`
    GreaterThanOrEqual,
}

/// A comparison expression: `left op right`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct ComparisonExpr {
    /// Left operand.
    pub left: Box<Expression>,
    /// The operator.
    pub op: ComparisonOp,
    /// Right operand.
    pub right: Box<Expression>,
    /// Full extent of the left operand, first token to last (parentheses
    /// included).
    pub left_span: SourceInfo,
    /// Full extent of the right operand, first token to last (parentheses
    /// included).
    pub right_span: SourceInfo,
    /// The operator token.
    pub source_info: SourceInfo,
}

/// Logical operator.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LogicalOp {
    /// `&&`
    And,
    /// `||`
    Or,
}

/// A logical expression: `left op right`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct LogicalExpr {
    /// Left operand.
    pub left: Box<Expression>,
    /// The operator.
    pub op: LogicalOp,
    /// Right operand.
    pub right: Box<Expression>,
    /// Full extent of the left operand, first token to last (parentheses
    /// included).
    pub left_span: SourceInfo,
    /// Full extent of the right operand, first token to last (parentheses
    /// included).
    pub right_span: SourceInfo,
    /// The operator token.
    pub source_info: SourceInfo,
}

/// Bitwise operator.
///
/// Uses F#-style triple operators to avoid ambiguity with existing Pure
/// syntax: `|` (lambda pipe), `^` (new instance), `<<`/`>>` (stereotypes),
/// and `&&`/`||` (logical AND/OR).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BitwiseOp {
    /// `&&&`
    And,
    /// `|||`
    Or,
    /// `^^^`
    Xor,
    /// `<<<`
    ShiftLeft,
    /// `>>>`
    ShiftRight,
}

/// A bitwise expression: `left op right`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct BitwiseExpr {
    /// Left operand.
    pub left: Box<Expression>,
    /// The operator.
    pub op: BitwiseOp,
    /// Right operand.
    pub right: Box<Expression>,
    /// Full extent of the left operand, first token to last (parentheses
    /// included).
    pub left_span: SourceInfo,
    /// Full extent of the right operand, first token to last (parentheses
    /// included).
    pub right_span: SourceInfo,
    /// The operator token.
    pub source_info: SourceInfo,
}

/// Unary not: `!expr`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct NotExpr {
    /// The operand.
    pub operand: Box<Expression>,
    /// Full extent of the operand, first token to last (parentheses
    /// included).
    pub operand_span: SourceInfo,
    /// The operator token.
    pub source_info: SourceInfo,
}

/// Unary minus: `-expr`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct UnaryMinusExpr {
    /// The operand.
    pub operand: Box<Expression>,
    /// Full extent of the operand, first token to last (parentheses
    /// included).
    pub operand_span: SourceInfo,
    /// The operator token.
    pub source_info: SourceInfo,
}

/// Bitwise complement: `~~~expr`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct BitwiseNotExpr {
    /// The operand.
    pub operand: Box<Expression>,
    /// Full extent of the operand, first token to last (parentheses
    /// included).
    pub operand_span: SourceInfo,
    /// The operator token.
    pub source_info: SourceInfo,
}

/// A function application: `func(args)` or `pkg::func(args)`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct FunctionApplication {
    /// The function being called (a packageable element reference).
    pub function: PackageableElementPtr,
    /// Arguments.
    pub arguments: Vec<Expression>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// Arrow (collection) function: `expr->func(args)`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct ArrowFunction {
    /// The left-hand side expression.
    pub target: Box<Expression>,
    /// The function being called (may be fully qualified, e.g., `meta::pure::functions::math::max`).
    pub function: PackageableElementPtr,
    /// Arguments (not including the implicit first argument).
    pub arguments: Vec<Expression>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A bare reference to a packageable element: `String`, `my::Enum`, `MyClass`.
///
/// This represents a name without an argument list — distinct from
/// `FunctionApplication` which always has parens.
/// Corresponds to Java grammar rule `instanceReference: qualifiedName`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct PackageableElementRef {
    /// The element being referenced.
    pub element: PackageableElementPtr,
    /// Source location.
    pub source_info: SourceInfo,
}

/// `Class.all()` and its variants: an element name followed by one of the
/// Java grammar's `allOrFunction` forms other than a plain call.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct AllInstancesExpr {
    /// The class.
    pub class: PackageableElementPtr,
    /// Which form.
    pub kind: AllInstancesKind,
    /// Milestoning dates: `%latest`, a date or a variable.
    pub arguments: Vec<Expression>,
    /// From the `.` to the closing `)`.
    pub call_source_info: SourceInfo,
    /// The whole expression.
    pub source_info: SourceInfo,
}

/// The forms of [`AllInstancesExpr`].
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AllInstancesKind {
    /// `.all()`, or `.all(dates)` for milestoned classes.
    All,
    /// `.allVersions()`: every version of a milestoned class's instances.
    AllVersions,
    /// `.allVersionsInRange(from, to)`.
    AllVersionsInRange,
}

impl AllInstancesKind {
    /// The form written after the class name's `.`, e.g. `all`.
    #[must_use]
    pub fn from_member(member: &str) -> Option<Self> {
        match member {
            "all" => Some(Self::All),
            "allVersions" => Some(Self::AllVersions),
            "allVersionsInRange" => Some(Self::AllVersionsInRange),
            _ => None,
        }
    }
}

/// Member access (dot) covering simple and qualified variants.
///
/// Both property access and enum value access parse identically
/// (`expr.identifier`); disambiguation is semantic.
#[derive(Debug, Clone, PartialEq)]
pub enum MemberAccess {
    /// Simple member access: `$x.name`, `MyEnum.VALUE`.
    /// No arguments — just `target.member`.
    Simple(SimpleMemberAccess),
    /// Qualified member access: `$x.derived('arg', 42)`.
    /// Has arguments — `target.member(args)`.
    Qualified(QualifiedMemberAccess),
}

impl Spanned for MemberAccess {
    fn source_info(&self) -> &SourceInfo {
        match self {
            Self::Simple(m) => &m.source_info,
            Self::Qualified(m) => &m.source_info,
        }
    }
}

/// Simple member access: `expr.member` (no arguments).
///
/// Covers property access (`$x.name`) and enum value reference (`MyEnum.VALUE`).
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct SimpleMemberAccess {
    /// The target expression (left of the dot).
    pub target: Box<Expression>,
    /// The member name (right of the dot).
    pub member: Identifier,
    /// Source span covering the member identifier itself — the
    /// clickable region for goto-def on the member name (e.g. `A`
    /// in `MyEnum.A`, `name` in `$person.name`). The outer
    /// `source_info` below is the dot token's span. Splitting them
    /// lets the IDE narrow navigation to the member name without
    /// re-computing it from the receiver's span at every consumer.
    pub member_source_info: SourceInfo,
    /// Source location (the `.` token).
    pub source_info: SourceInfo,
}

/// Qualified member access: `expr.member(args)` (with arguments).
///
/// Covers qualified property calls like `$x.derivedProp('arg')`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct QualifiedMemberAccess {
    /// The target expression (left of the dot).
    pub target: Box<Expression>,
    /// The member name (right of the dot).
    pub member: Identifier,
    /// Source span covering the member identifier itself — see
    /// [`SimpleMemberAccess::member_source_info`].
    pub member_source_info: SourceInfo,
    /// Arguments.
    pub arguments: Vec<Expression>,
    /// Source location (the `.` token).
    pub source_info: SourceInfo,
}

/// A type reference expression: `@MyType` or the bare structural form
/// `(a:Integer, b:String)`.
///
/// Used as an argument to `cast` and `instanceOf` arrow functions:
/// `$x->cast(@MyType)`, `$x->instanceOf(@MyType)`. Also covers the bare
/// structural-relation literal at expression position
/// (`$row->cast((a:Integer, b:String))`) per the Java grammar's
/// `atomicExpression: ... | type` alternative — Java's processor
/// (`AntlrContextToM3CoreInstance.atomicExpression` line 1096) routes
/// both alternatives through the same `InstanceValue<Type[1]>`
/// construction, so the AST doesn't carry the surface distinction.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct TypeReferenceExpr {
    /// The referenced type.
    pub type_ref: crate::type_ref::TypeSpec,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A bare multiplicity literal: `@[1]`, `@[0..1]`, `@[1..*]`, `@[*]`,
/// or `@[m]` referencing a declared multiplicity parameter.
///
/// Primary use: the second argument of
/// `meta::pure::functions::lang::toMultiplicity<T|z>(source:T[*], object:Any[z]):T[z]`,
/// where the literal's multiplicity binds the parametric `z` at the call
/// site. Java grammar:
/// `M3CoreParser.g4:311 AT (type | multiplicity)` — the multiplicity
/// branch of the same production that produces `TypeReferenceExpr`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct MultiplicityReferenceExpr {
    /// The multiplicity carried by the literal. `Concrete(...)` for
    /// fixed forms (`@[1]`, `@[*]`, …); `Identifier(name, ...)` for
    /// references to a declared `<T|name>` multiplicity parameter.
    pub multiplicity: crate::type_ref::MultiplicityArgument,
    /// Source location (the `@` token through the closing `]`).
    pub source_info: SourceInfo,
}

/// A lambda expression: `{params | body}` or `params | body`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct Lambda {
    /// Lambda parameters.
    pub parameters: Vec<Parameter>,
    /// Body expressions.
    pub body: Vec<Expression>,
    /// From the `|` to the end of the body, a trailing `;` included:
    /// parameters and braces are outside it (the engine's `lambdaPipe`).
    pub source_info: SourceInfo,
}

/// A code block standing alone, as the engine's `parseLambda` reads a
/// lambda's text: statements separated by `;`, the last one's optional
/// when there is only one.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct CodeBlock {
    /// The statements.
    pub expressions: Vec<Expression>,
    /// From the first statement to the last token, a final `;` included.
    pub source_info: SourceInfo,
}

/// A let binding: `let x = expr`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct LetExpr {
    /// Variable name being bound.
    pub name: Identifier,
    /// Source span of just the **name** identifier (not the full
    /// `let x = expr` line). Drives IDE find-usages and goto-def for
    /// let-bound locals: the walker uses this as the navigation
    /// target and as the reverse-index key in `by_local`.
    ///
    /// Falls back to a copy of [`Self::source_info`] when the AST
    /// was reconstructed from a protocol JSON payload that doesn't
    /// carry per-identifier spans.
    pub name_source_info: SourceInfo,
    /// The expression being assigned.
    pub value: Box<Expression>,
    /// Source location of the whole `let x = expr` line.
    pub source_info: SourceInfo,
}

/// A collection literal: `[1, 2, 3]`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct CollectionExpr {
    /// Elements of the collection.
    pub elements: Vec<Expression>,
    /// Multiplicity (inferred or explicit).
    pub multiplicity: Option<Multiplicity>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A slice expression: `[start:stop:step]` or `[start:stop]` or `[:stop]`.
///
/// Named to match the Java ANTLR grammar rule `sliceExpression`.
/// Sugar for `range(start, stop, step)` function call.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct SliceExpr {
    /// The start expression (optional — `[:5]` has no start).
    pub start: Option<Box<Expression>>,
    /// The stop expression.
    pub stop: Box<Expression>,
    /// The step expression (optional — `[0:5]` has no step).
    pub step: Option<Box<Expression>>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A new instance expression: `^MyClass(prop1='val', prop2=42)` or
/// the named form `^MyClass name(prop1='val')`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct NewInstanceExpr {
    /// The class being instantiated (a packageable element reference).
    pub class: PackageableElementPtr,
    /// Optional type arguments: `^List<String>()`.
    pub type_arguments: Vec<crate::type_ref::TypeArgument>,
    /// Optional type variable values: `^Ext(10)()`.
    pub type_variable_values: Vec<crate::type_ref::TypeVariableValue>,
    /// Optional instance name: `^MyClass myName(props)`. Java-side this
    /// becomes a distinguishing key the test runtime can look up by.
    pub instance_name: Option<Identifier>,
    /// Property value assignments.
    pub assignments: Vec<KeyValuePair>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A copy expression: `^$source(prop1='new', prop2=42)`.
///
/// Creates a copy of the source variable with property overrides.
/// Syntactically: `^$variableName(overrides...)` or `^$variableName<TypeArgs>(overrides...)`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct CopyExpr {
    /// The source variable name (without `$` prefix).
    pub source: Identifier,
    /// Property value overrides.
    pub assignments: Vec<KeyValuePair>,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A key-value pair in a new instance: `propName = expr` or `propName += expr`.
///
/// `augmented` distinguishes the two assignment forms:
/// - `false` → `propName = expr` — replaces the slot's contents (Java
///   `KeyValue.add = false`, runtime `mutate_set`).
/// - `true` → `propName += expr` — appends to the slot, preserving any
///   carried-over source values (Java `KeyValue.add = true`, runtime
///   `mutate_add`). On `^Class(...)` constructions there is no prior
///   value, so `+=` and `=` produce the same result; on `^$src(...)` copy
///   expressions the distinction is load-bearing.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct KeyValuePair {
    /// The property name.
    pub key: Identifier,
    /// The value expression.
    pub value: Expression,
    /// `true` for `propName += expr` (append); `false` for `propName = expr`
    /// (replace). See struct-level docs for semantics.
    pub augmented: bool,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A navigation path expression: `#/StartType/prop1(args)/prop2!alias#`.
///
/// Path expressions describe a chain of property accesses through the type
/// graph as a first-class value. They compile to instances of
/// `meta::pure::metamodel::path::Path<-U,V|m> extends Function<{U[1]→V[m]}>`.
///
/// Invariant: `path` is non-empty — a bare `#/Type#` is rejected at parse
/// time, matching the Java grammar's "A path must contain at least one
/// navigation" rule.
///
/// # Examples
///
/// - `#/Person/name#` — single property step
/// - `#/Firm<Any>/employees/address#` — multi-step with type arguments on the start
/// - `#/Person/nameWithTitle('1')#` — qualified property with parameters
/// - `#/Product/synonymsByType(SynType.CUSIP)/value!cusip#` — params + alias
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct NavigationPath {
    /// The starting type — supports type arguments (`Firm<Any>`) so the
    /// chain's first hop can resolve under the right binding.
    pub start_type: TypeReference,
    /// The property steps. Always non-empty by parser construction.
    pub path: Vec<PropertyPathElement>,
    /// Optional alias suffix (`!myAlias`); becomes the `name` slot on the
    /// resulting `Path` instance.
    pub name: Option<Identifier>,
    /// Source location spanning the full `#/.../#` form.
    pub source_info: SourceInfo,
}

/// A single segment in a navigation path: `/property` or `/property(params)`.
///
/// `parameters` is empty for plain property steps and populated for qualified-
/// property steps (`/nameWithTitle('Mr')`). The lowering pass type-checks each
/// parameter against the resolved property's signature.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct PropertyPathElement {
    /// The property name being navigated.
    pub property: Identifier,
    /// Parameters for qualified properties — scalar literals, enum stubs
    /// (`SynType.CUSIP` parses as `MemberAccess::Simple`), or collection
    /// literals.
    pub parameters: Vec<Expression>,
    /// Source location for this `/property(...)` segment.
    pub source_info: SourceInfo,
}

/// A unit instance expression: `5 RomanLength~Pes`.
///
/// In Pure, a numeric literal followed by a `Measure~Unit` reference
/// creates a value of that unit. For example, `10.5D RomanLength~Cubitum`
/// represents 10.5 of the unit Cubitum.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct UnitInstanceExpr {
    /// The numeric value expression (integer, float, or decimal literal).
    pub value: Box<Expression>,
    /// The unit reference path (e.g., `RomanLength~Pes`).
    pub unit: PackageableElementPtr,
    /// Source location.
    pub source_info: SourceInfo,
}

/// A unit as a value: `RomanLength~Pes`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct UnitTypeRef {
    /// The unit: its measure's path, `~` and its name (e.g.
    /// `my::RomanLength~Pes`).
    pub unit: PackageableElementPtr,
    /// From the measure to the unit.
    pub source_info: SourceInfo,
}

/// A column specification: `~name` or `~name:Type[mult]` or `~name:x|$x+1`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct ColumnSpec {
    /// Column stereotypes.
    pub stereotypes: Vec<crate::annotation::StereotypePtr>,
    /// Column tagged values.
    pub tagged_values: Vec<crate::annotation::TaggedValue>,
    /// Column name.
    pub name: Identifier,
    /// Optional column type spec.
    pub type_spec: Option<ColumnTypeSpec>,
    /// Optional extra function.
    pub extra_function: Option<Box<Expression>>,
    /// The whole specification, stereotypes to last function.
    pub source_info: SourceInfo,
}

/// What follows the `:` in a column spec.
#[derive(Debug, Clone, PartialEq)]
pub enum ColumnTypeSpec {
    /// Type and optional multiplicity: `String[1]`
    Typed(
        crate::type_ref::TypeReference,
        Option<crate::type_ref::Multiplicity>,
    ),
    /// Lambda: `x|$x+1`
    Lambda(Lambda),
}

/// Column builder expression: `~col` or `~[col1, col2]`.
#[derive(Debug, Clone, PartialEq, crate::Spanned)]
pub struct ColumnBuilderExpr {
    /// Columns.
    pub columns: Vec<ColumnSpec>,
    /// Whether the source form was the array shape `~[…]` (true) or
    /// the single-column shape `~col` (false). Drives the lowering
    /// to `colSpecArray` vs `colSpec` (and their `funcColSpec*` /
    /// `aggColSpec*` variants once lambda forms are supported) per
    /// the platform's `PCT.grammarCharacters` annotations.
    pub is_array: bool,
    /// What follows the `~`: the column specification, or the `[…]`.
    pub source_info: SourceInfo,
}

/// Visitor pattern for walking expression trees.
///
/// Default implementations are no-ops — override only the variants you care about.
#[allow(unused_variables)]
pub trait ExpressionVisitor {
    /// Visit any expression — dispatches to specific `visit_*` methods.
    fn visit(&mut self, expr: &Expression) {
        match expr {
            Expression::Literal(e) => self.visit_literal(e),
            Expression::Variable(e) => self.visit_variable(e),
            Expression::Arithmetic(e) => self.visit_arithmetic(e),
            Expression::Comparison(e) => self.visit_comparison(e),
            Expression::Logical(e) => self.visit_logical(e),
            Expression::Bitwise(e) => self.visit_bitwise(e),
            Expression::Not(e) => self.visit_not(e),
            Expression::UnaryMinus(e) => self.visit_unary_minus(e),
            Expression::BitwiseNot(e) => self.visit_bitwise_not(e),
            Expression::FunctionApplication(e) => self.visit_function_application(e),
            Expression::ArrowFunction(e) => self.visit_arrow_function(e),
            Expression::MemberAccess(e) => self.visit_member_access(e),
            Expression::TypeReferenceExpr(e) => self.visit_type_reference(e),
            Expression::MultiplicityReferenceExpr(e) => self.visit_multiplicity_reference(e),
            Expression::Lambda(e) => self.visit_lambda(e),
            Expression::Let(e) => self.visit_let(e),
            Expression::Collection(e) => self.visit_collection(e),
            Expression::Slice(e) => self.visit_slice(e),
            Expression::NewInstance(e) => self.visit_new_instance(e),
            Expression::Copy(e) => self.visit_copy(e),
            Expression::Column(e) => self.visit_column(e),
            Expression::PackageableElementRef(e) => self.visit_element_ref(e),
            Expression::AllInstances(e) => self.visit_all_instances(e),
            Expression::Island(e) => self.visit_island(e),
            Expression::NavigationPath(e) => self.visit_navigation_path(e),
            Expression::UnitInstance(e) => self.visit_unit_instance(e),
            Expression::UnitType(e) => self.visit_unit_type(e),
            Expression::Group(e) => self.visit(e),
        }
    }

    /// Visit any literal.
    fn visit_literal(&mut self, expr: &Literal) {}
    /// Visit a variable reference.
    fn visit_variable(&mut self, expr: &Variable) {}
    /// Visit an arithmetic expression.
    fn visit_arithmetic(&mut self, expr: &ArithmeticExpr) {}
    /// Visit a comparison expression.
    fn visit_comparison(&mut self, expr: &ComparisonExpr) {}
    /// Visit a logical expression.
    fn visit_logical(&mut self, expr: &LogicalExpr) {}
    /// Visit a bitwise expression.
    fn visit_bitwise(&mut self, expr: &BitwiseExpr) {}
    /// Visit a not expression.
    fn visit_not(&mut self, expr: &NotExpr) {}
    /// Visit a unary minus expression.
    fn visit_unary_minus(&mut self, expr: &UnaryMinusExpr) {}
    /// Visit a bitwise complement expression.
    fn visit_bitwise_not(&mut self, expr: &BitwiseNotExpr) {}
    /// Visit a function application.
    fn visit_function_application(&mut self, expr: &FunctionApplication) {}
    /// Visit an arrow function.
    fn visit_arrow_function(&mut self, expr: &ArrowFunction) {}
    /// Visit a member access (simple or qualified).
    fn visit_member_access(&mut self, expr: &MemberAccess) {}
    /// Visit a type reference expression.
    fn visit_type_reference(&mut self, expr: &TypeReferenceExpr) {}
    /// Visit a bare multiplicity literal expression: `@[m]`.
    fn visit_multiplicity_reference(&mut self, expr: &MultiplicityReferenceExpr) {}
    /// Visit a lambda.
    fn visit_lambda(&mut self, expr: &Lambda) {}
    /// Visit a let expression.
    fn visit_let(&mut self, expr: &LetExpr) {}
    /// Visit a collection expression.
    fn visit_collection(&mut self, expr: &CollectionExpr) {}
    /// Visit a slice expression.
    fn visit_slice(&mut self, expr: &SliceExpr) {
        if let Some(s) = &expr.start {
            self.visit(s);
        }
        self.visit(&expr.stop);
        if let Some(s) = &expr.step {
            self.visit(s);
        }
    }
    /// Visit a new instance expression.
    fn visit_new_instance(&mut self, expr: &NewInstanceExpr) {}
    /// Visit a copy expression.
    fn visit_copy(&mut self, expr: &CopyExpr) {}
    /// Visit a column builder expression.
    fn visit_column(&mut self, expr: &ColumnBuilderExpr) {}
    /// Visit a bare packageable element reference.
    fn visit_element_ref(&mut self, expr: &PackageableElementRef) {}
    /// Visit a `Class.all()` expression.
    fn visit_all_instances(&mut self, expr: &AllInstancesExpr) {}
    /// Visit an island grammar expression.
    fn visit_island(&mut self, expr: &IslandExpression) {}
    /// Visit a navigation path expression. Default walks parameter
    /// expressions on each step so visitors that scan for sub-expressions
    /// (variable refs, literals, etc.) see them without an override.
    fn visit_navigation_path(&mut self, expr: &NavigationPath) {
        for step in &expr.path {
            for param in &step.parameters {
                self.visit(param);
            }
        }
    }
    /// Visit a unit instance expression.
    fn visit_unit_instance(&mut self, expr: &UnitInstanceExpr) {}
    /// Visit a unit used as a value.
    fn visit_unit_type(&mut self, expr: &UnitTypeRef) {}
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::type_ref::{Package, TypeReference};
    use smol_str::SmolStr;

    use crate::test_utils::src;

    fn elem_ptr(name: &str) -> PackageableElementPtr {
        PackageableElementPtr {
            package: None,
            name: SmolStr::new(name),
            source_info: src(),
        }
    }

    #[test]
    fn test_literal_integer() {
        let expr = Expression::Literal(Literal::Integer(IntegerLiteral {
            value: 42,
            source_info: SourceInfo::new("test.pure", 1, 5, 1, 7),
        }));
        assert_eq!(expr.source_info().start_column, 5);
    }

    #[test]
    fn test_literal_enum_matching() {
        let lit = Literal::String(StringLiteral {
            value: smol_str::SmolStr::new("hello"),
            multi_line: false,
            source_info: src(),
        });
        // Can match "is literal?" without caring about type
        assert!(matches!(lit, Literal::String(_)));
        // Can also match at the Expression level
        let expr = Expression::Literal(lit);
        assert!(matches!(expr, Expression::Literal(_)));
    }

    #[test]
    fn test_arithmetic() {
        let left = Box::new(Expression::Literal(Literal::Integer(IntegerLiteral {
            value: 1,
            source_info: src(),
        })));
        let right = Box::new(Expression::Literal(Literal::Integer(IntegerLiteral {
            value: 2,
            source_info: src(),
        })));
        let expr = Expression::Arithmetic(ArithmeticExpr {
            left,
            op: ArithmeticOp::Plus,
            right,
            left_span: src(),
            right_span: src(),
            source_info: src(),
        });
        assert_eq!(expr.source_info().start_line, 1);
    }

    #[test]
    fn test_bitwise_operations() {
        let left = Box::new(Expression::Literal(Literal::Integer(IntegerLiteral {
            value: 0xFF,
            source_info: src(),
        })));
        let right = Box::new(Expression::Literal(Literal::Integer(IntegerLiteral {
            value: 0x0F,
            source_info: src(),
        })));
        let expr = Expression::Bitwise(BitwiseExpr {
            left,
            op: BitwiseOp::And,
            right,
            left_span: src(),
            right_span: src(),
            source_info: src(),
        });
        if let Expression::Bitwise(b) = &expr {
            assert_eq!(b.op, BitwiseOp::And);
        }
    }

    #[test]
    fn test_simple_member_access() {
        // $x.name
        let expr = Expression::MemberAccess(MemberAccess::Simple(SimpleMemberAccess {
            target: Box::new(Expression::Variable(Variable {
                name: SmolStr::new("x"),
                source_info: src(),
            })),
            member: SmolStr::new("name"),
            member_source_info: src(),
            source_info: src(),
        }));
        if let Expression::MemberAccess(MemberAccess::Simple(ma)) = &expr {
            assert_eq!(ma.member, "name");
        }
    }

    #[test]
    fn test_qualified_member_access() {
        // $x.derivedProp('arg')
        let expr = Expression::MemberAccess(MemberAccess::Qualified(QualifiedMemberAccess {
            target: Box::new(Expression::Variable(Variable {
                name: SmolStr::new("x"),
                source_info: src(),
            })),
            member: SmolStr::new("derivedProp"),
            member_source_info: src(),
            arguments: vec![Expression::Literal(Literal::String(StringLiteral {
                value: smol_str::SmolStr::new("arg"),
                multi_line: false,
                source_info: src(),
            }))],
            source_info: src(),
        }));
        if let Expression::MemberAccess(MemberAccess::Qualified(ma)) = &expr {
            assert_eq!(ma.member, "derivedProp");
            assert_eq!(ma.arguments.len(), 1);
        }
    }

    #[test]
    fn test_function_application_uses_element_ptr() {
        let expr = Expression::FunctionApplication(FunctionApplication {
            function: PackageableElementPtr {
                package: Some(Package::root(SmolStr::new("pkg"), src())),
                name: SmolStr::new("myFunc"),
                source_info: src(),
            },
            arguments: vec![],
            source_info: src(),
        });
        if let Expression::FunctionApplication(fa) = &expr {
            assert_eq!(fa.function.name, "myFunc");
            assert_eq!(fa.function.package.as_ref().unwrap().name(), "pkg");
        }
    }

    #[test]
    fn test_new_instance_uses_element_ptr() {
        let expr = Expression::NewInstance(NewInstanceExpr {
            class: elem_ptr("MyClass"),
            type_arguments: vec![],
            type_variable_values: vec![],
            instance_name: None,
            assignments: vec![],
            source_info: src(),
        });
        if let Expression::NewInstance(ni) = &expr {
            assert_eq!(ni.class.name, "MyClass");
        }
    }

    #[test]
    fn test_type_reference_expr() {
        let expr = Expression::TypeReferenceExpr(TypeReferenceExpr {
            type_ref: crate::type_ref::TypeSpec::Type(TypeReference {
                package: None,
                name: SmolStr::new("MyType"),
                type_arguments: vec![],
                multiplicity_arguments: vec![],
                type_variable_values: vec![],
                source_info: src(),
            }),
            source_info: src(),
        });
        if let Expression::TypeReferenceExpr(tr) = &expr {
            assert_eq!(tr.type_ref.full_path(), "MyType");
        }
    }

    #[test]
    fn test_lambda() {
        let lambda = Expression::Lambda(Lambda {
            parameters: vec![Parameter {
                name: SmolStr::new("x"),
                type_ref: Some(crate::type_ref::TypeSpec::Type(TypeReference {
                    package: None,
                    name: SmolStr::new("String"),
                    type_arguments: vec![],
                    multiplicity_arguments: vec![],
                    type_variable_values: vec![],
                    source_info: src(),
                })),
                multiplicity: Some(Multiplicity::one()),
                source_info: src(),
            }],
            body: vec![Expression::Variable(Variable {
                name: SmolStr::new("x"),
                source_info: src(),
            })],
            source_info: src(),
        });
        if let Expression::Lambda(l) = &lambda {
            assert_eq!(l.parameters.len(), 1);
        }
    }

    #[test]
    fn test_visitor_with_literal() {
        struct LitCounter {
            count: u32,
        }

        impl ExpressionVisitor for LitCounter {
            fn visit_literal(&mut self, _: &Literal) {
                self.count += 1;
            }
        }

        let expr = Expression::Literal(Literal::Integer(IntegerLiteral {
            value: 42,
            source_info: src(),
        }));

        let mut counter = LitCounter { count: 0 };
        counter.visit(&expr);
        assert_eq!(counter.count, 1);
    }
}
