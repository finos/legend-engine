// Copyright 2026 The Legend Authors
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

use super::Parser;
use super::R;
use super::{split_package_name, unquote_string};
use crate::error::ParseError;
use legend_pure_parser_ast::SourceInfo;
use legend_pure_parser_ast::element::TypeAndMultiplicityParameters;
use legend_pure_parser_ast::type_ref::{
    AlgebraOp, AlgebraOpKind, FunctionType, FunctionTypeParameter, Identifier, Multiplicity,
    MultiplicityArgument, Package, RelationColumn, RelationType, TypeArgument, TypeReference,
    TypeSpec, TypeVariableValue, UnitReference,
};
use legend_pure_parser_lexer::TokenKind;
use smol_str::SmolStr;

impl Parser {
    // ── Type references ─────────────────────────────────────────────────

    /// A named type: `String`, `pkg::Map<K, V>`, `VARCHAR(200)`. Positions
    /// that take any type (relation, function or unit types too) use
    /// [`Self::parse_type_spec`].
    pub(crate) fn parse_type_reference(&mut self) -> R<TypeReference> {
        // `start` covers the FIRST token of the path; `path` (the
        // outermost `Package`) carries the LAST segment's span.
        // Merge them so the resulting `TypeReference.source_info`
        // covers the WHOLE qualified name (`pkg::sub::Foo`), not
        // just the leading `pkg`. Required for the goto-def index:
        // `walk_type_expr` records a clickable region per
        // `TypeExpr::Named`, and `find_at(line, column)` only
        // matches positions inside that region. Without the merge,
        // clicking on `Foo` after `extends pkg::sub::` falls
        // outside the recorded span and goto silently no-ops.
        let start = self.cursor.current_source_info();
        let path = self.parse_package_path()?;
        let fqn_span = start.merge(legend_pure_parser_ast::source_info::Spanned::source_info(
            &path,
        ));
        let (pkg, name) = split_package_name(&path);
        self.finish_type_reference(fqn_span, pkg, name)
    }

    /// Completes type reference parsing after the package path has been consumed.
    ///
    /// This allows callers (like `parse_type_spec`) to parse the path first,
    /// inspect ahead for `<(` column-spec syntax, and then delegate here for
    /// the standard `<TypeArgs>(TypeVarValues)` continuation.
    pub(crate) fn finish_type_reference(
        &mut self,
        start: SourceInfo,
        pkg: Option<Package>,
        name: SmolStr,
    ) -> R<TypeReference> {
        let (type_arguments, multiplicity_arguments) = if self.cursor.eat(TokenKind::Less) {
            let mut args = Vec::new();
            if !self.cursor.check(TokenKind::Pipe) {
                loop {
                    args.push(self.parse_type_argument()?);
                    if !self.cursor.eat(TokenKind::Comma) {
                        break;
                    }
                }
            }
            // Multiplicity arguments: <TypeArgs | MultArgs>
            let mult_args = if self.cursor.eat(TokenKind::Pipe) {
                self.parse_multiplicity_arguments()?
            } else {
                vec![]
            };
            self.cursor.expect_closing_angle_bracket()?;
            (args, mult_args)
        } else {
            (vec![], vec![])
        };
        let type_variable_values = if self.cursor.eat(TokenKind::LParen) {
            let mut vals = Vec::new();
            loop {
                vals.push(self.parse_type_variable_value()?);
                if !self.cursor.eat(TokenKind::Comma) {
                    break;
                }
            }
            self.cursor.expect(TokenKind::RParen)?;
            vals
        } else {
            vec![]
        };
        Ok(TypeReference {
            package: pkg,
            name,
            type_arguments,
            multiplicity_arguments,
            type_variable_values,
            // The whole type text, arguments included (the engine's
            // `processGenericType` positions the raw type on it).
            source_info: self.cursor.span_since(&start),
        })
    }

    /// A type argument: the Legend Pure grammar's `typeWithOperation`,
    /// `type ('=' type)? (('+' | '-') type)* ('⊆' type)?`. For `Z=T` the
    /// binding is `Z` and the type `T`, e.g. `ColSpec<Z=(?:K)⊆T>` in
    /// `rename`; `T - Z + V` is relation algebra (`Relation<T-Z+V>`).
    pub(crate) fn parse_type_argument(&mut self) -> R<TypeArgument> {
        let mut type_spec = self.parse_type_spec()?;
        let mut equal_binding = None;
        if self.cursor.eat(TokenKind::Equals) {
            if let TypeSpec::Type(binding) = &type_spec
                && binding.package.is_none()
                && binding.type_arguments.is_empty()
                && binding.multiplicity_arguments.is_empty()
            {
                equal_binding = Some(binding.name.clone());
            }
            type_spec = self.parse_type_spec()?;
        }
        let mut algebra_ops = Vec::new();
        while self.cursor.check(TokenKind::Plus) || self.cursor.check(TokenKind::Minus) {
            let kind = if self.cursor.eat(TokenKind::Plus) {
                AlgebraOpKind::Union
            } else {
                self.cursor.expect(TokenKind::Minus)?;
                AlgebraOpKind::Difference
            };
            let operand = self.parse_type_spec()?;
            algebra_ops.push(AlgebraOp { kind, operand });
        }
        let subset_bound = if self.cursor.eat(TokenKind::Subset) {
            Some(Box::new(self.parse_type_spec()?))
        } else {
            None
        };
        Ok(TypeArgument {
            type_spec,
            equal_binding,
            algebra_ops,
            subset_bound,
        })
    }

    /// Parses a type specification that can be a type, unit reference, or relation type.
    ///
    /// Used in positions where the Pure grammar accepts:
    /// - Regular types: `String`, `Map<K, V>`
    /// - Unit references: `NewMeasure~UnitOne`
    /// - Relation types: `(a:Integer, b:String)` or `Relation<(a:Integer, b:String)>`
    pub(crate) fn parse_type_spec(&mut self) -> R<TypeSpec> {
        // Function type: {ParamType[mult] -> ReturnType[mult]}
        if self.cursor.check(TokenKind::LBrace) {
            return Ok(TypeSpec::Function(self.parse_function_type()?));
        }
        // Relation type: (col:Type, ...)
        if self.cursor.check(TokenKind::LParen) {
            return self.parse_relation_type();
        }
        let start = self.cursor.current_source_info();
        let path = self.parse_package_path()?;
        let fqn_span = start.merge(legend_pure_parser_ast::source_info::Spanned::source_info(
            &path,
        ));
        let (pkg, name) = split_package_name(&path);
        let type_ref = self.finish_type_reference(fqn_span, pkg, name)?;
        if self.cursor.eat(TokenKind::Tilde) {
            let (unit_name, _) = self.cursor.expect_identifier_or_keyword()?;
            Ok(TypeSpec::Unit(UnitReference {
                measure: type_ref,
                unit: unit_name,
                source_info: self.cursor.span_since(&start),
            }))
        } else {
            Ok(TypeSpec::Type(type_ref))
        }
    }

    /// Parse a bare relation type: `(col:Type[mult]?, ...)`
    pub(crate) fn parse_relation_type(&mut self) -> R<TypeSpec> {
        let start = self.cursor.current_source_info();
        let columns = self.parse_relation_columns()?;
        Ok(TypeSpec::Relation(RelationType {
            columns,
            source_info: self.cursor.span_since(&start),
        }))
    }

    /// Parse relation columns: `(name:Type[mult]?, ...)`
    ///
    /// Shared by both bare relation types and `Relation<(...)>` syntax.
    pub(crate) fn parse_relation_columns(&mut self) -> R<Vec<RelationColumn>> {
        self.cursor.expect(TokenKind::LParen)?;
        let mut cols = Vec::new();
        loop {
            let col_si = self.cursor.current_source_info();
            // `?` is the wildcard column-name placeholder (e.g.
            // `ColSpec<(?:Type)>` in eval.pure) — it stands for "any
            // column with this type, name unspecified". Capture as `?`
            // verbatim; the resolver treats wildcard names as positional.
            let col_name = if self.cursor.eat(TokenKind::Question) {
                SmolStr::new("?")
            } else {
                let (n, _) = self.cursor.expect_identifier_or_keyword()?;
                n
            };
            self.cursor.expect(TokenKind::Colon)?;
            // `?` is the wildcard column-type placeholder, e.g.
            // `SortInfo<(?:?)⊆T>` in `over.pure`'s OLAP overload set —
            // it stands for "any type, position-only constraint" and
            // pairs with the wildcard column-name `?` above. Synthesise
            // a `TypeReference { name: "?" }` so the AST captures the
            // shape verbatim; downstream resolution treats `?`-named
            // type references as `Any`-equivalent.
            let col_type = if self.cursor.check(TokenKind::Question) {
                let qsi = self.cursor.current_source_info();
                self.cursor.advance();
                TypeSpec::Type(TypeReference {
                    package: None,
                    name: SmolStr::new("?"),
                    type_arguments: vec![],
                    multiplicity_arguments: vec![],
                    type_variable_values: vec![],
                    source_info: qsi,
                })
            } else {
                self.parse_type_spec()?
            };
            let multiplicity = if self.cursor.check(TokenKind::LBracket) {
                self.cursor.expect(TokenKind::LBracket)?;
                let mult = self.parse_multiplicity()?;
                self.cursor.expect(TokenKind::RBracket)?;
                Some(mult)
            } else {
                None
            };
            cols.push(RelationColumn {
                name: col_name,
                type_ref: col_type,
                multiplicity,
                // `name: Type[mult]`, as the engine positions a column.
                source_info: self.cursor.span_since(&col_si),
            });
            if !self.cursor.eat(TokenKind::Comma) {
                break;
            }
        }
        self.cursor.expect(TokenKind::RParen)?;
        Ok(cols)
    }

    /// Parses a function type `{ParamType[mult], ... -> ReturnType[mult]}`.
    ///
    /// # Grammar
    ///
    /// ```text
    /// functionType: '{' (functionTypeParam (',' functionTypeParam)*)? '->' typeRef '[' mult ']' '}'
    /// functionTypeParam: typeRef '[' mult ']'
    /// ```
    pub(crate) fn parse_function_type(&mut self) -> R<FunctionType> {
        let start = self.cursor.current_source_info();
        self.cursor.expect(TokenKind::LBrace)?;
        let mut parameters = Vec::new();
        // `{-> R[m]}` has no parameters; otherwise `P[m], ... -> R[m]`.
        if !self.cursor.check(TokenKind::Arrow) {
            loop {
                let param_start = self.cursor.current_source_info();
                let type_ref = self.parse_type_spec()?;
                self.cursor.expect(TokenKind::LBracket)?;
                let multiplicity = self.parse_multiplicity()?;
                self.cursor.expect(TokenKind::RBracket)?;
                parameters.push(FunctionTypeParameter {
                    type_ref,
                    multiplicity,
                    source_info: self.cursor.span_since(&param_start),
                });
                if self.cursor.check(TokenKind::Arrow) {
                    break;
                }
                self.cursor.expect(TokenKind::Comma)?;
            }
        }
        self.cursor.expect(TokenKind::Arrow)?;
        let return_type = self.parse_type_spec()?;
        self.cursor.expect(TokenKind::LBracket)?;
        let return_multiplicity = self.parse_multiplicity()?;
        self.cursor.expect(TokenKind::RBracket)?;
        self.cursor.expect(TokenKind::RBrace)?;
        Ok(FunctionType {
            parameters,
            return_type: Box::new(return_type),
            return_multiplicity,
            source_info: self.cursor.span_since(&start),
        })
    }

    /// Parses optional type variable parameter definitions on classes and primitives: `(x:Integer[1], y:String[1])`.
    pub(crate) fn parse_type_variable_parameters(
        &mut self,
    ) -> R<Vec<legend_pure_parser_ast::type_ref::TypeVariableParameter>> {
        if !self.cursor.eat(TokenKind::LParen) {
            return Ok(vec![]);
        }

        let mut params = Vec::new();
        while !self.cursor.check(TokenKind::RParen) {
            let si = self.cursor.current_source_info();
            let (name, _) = self.cursor.expect_identifier_or_keyword()?;
            self.cursor.expect(TokenKind::Colon)?;
            let type_ref = self.parse_type_reference()?;
            self.cursor.expect(TokenKind::LBracket)?;
            let multiplicity = self.parse_multiplicity()?;
            self.cursor.expect(TokenKind::RBracket)?;

            params.push(legend_pure_parser_ast::type_ref::TypeVariableParameter {
                name,
                type_ref,
                multiplicity,
                source_info: si,
            });

            if !self.cursor.eat(TokenKind::Comma) {
                break;
            }
        }
        self.cursor.expect(TokenKind::RParen)?;
        Ok(params)
    }

    /// Parses optional `<TypeParams | MultParams>` on class/function declarations.
    ///
    /// Grammar:
    /// ```text
    /// typeAndMultiplicityParameters: '<' ((typeParameters multiplictyParameters?) | multiplictyParameters) '>'
    /// typeParameters:                typeParameter (',' typeParameter)*
    /// typeParameter:                 ('-' | '+')? identifier
    /// multiplictyParameters:         '|' identifier (',' identifier)*
    /// ```
    ///
    /// `-` / `+` prefixes mark contravariance / covariance respectively
    /// (e.g., `Path<-U,V|m>`). Only `path.pure` uses this in the platform
    /// today; we accept-and-discard since variance is documentary in
    /// current Pure semantics — the type system does not enforce or
    /// propagate the marker.
    ///
    /// Returns `(type_params, mult_params)`. Both may be empty if no `<` is present.
    pub(crate) fn parse_type_and_multiplicity_parameters(
        &mut self,
    ) -> R<Option<TypeAndMultiplicityParameters>> {
        let start = self.cursor.current_source_info();
        if !self.cursor.check(TokenKind::Less) {
            return Ok(None);
        }
        let (type_parameters, multiplicity_parameters) =
            self.parse_type_and_multiplicity_lists()?;
        Ok(Some(TypeAndMultiplicityParameters {
            type_parameters,
            multiplicity_parameters,
            source_info: self.cursor.span_since(&start),
        }))
    }

    /// `<T, U|m, n>`, from its `<`.
    fn parse_type_and_multiplicity_lists(&mut self) -> R<(Vec<Identifier>, Vec<Identifier>)> {
        self.cursor.expect(TokenKind::Less)?;

        // Check if it starts with `|` — that means no type params, only mult params.
        if self.cursor.check(TokenKind::Pipe) {
            self.cursor.advance(); // eat |
            let mut mult_params = Vec::new();
            loop {
                let (p, _) = self.cursor.expect_identifier()?;
                mult_params.push(p);
                if !self.cursor.eat(TokenKind::Comma) {
                    break;
                }
            }
            self.cursor.expect_closing_angle_bracket()?;
            return Ok((vec![], mult_params));
        }

        // Parse type parameters with optional variance prefix
        // (`-T` contravariant, `+T` covariant — accepted and dropped).
        let mut type_params = Vec::new();
        loop {
            let _variance = self.cursor.eat(TokenKind::Minus) || self.cursor.eat(TokenKind::Plus);
            let (p, _) = self.cursor.expect_identifier()?;
            type_params.push(p);
            if !self.cursor.eat(TokenKind::Comma) {
                break;
            }
        }

        // Optional multiplicity parameters after |
        let mult_params = if self.cursor.eat(TokenKind::Pipe) {
            let mut params = Vec::new();
            loop {
                let (p, _) = self.cursor.expect_identifier()?;
                params.push(p);
                if !self.cursor.eat(TokenKind::Comma) {
                    break;
                }
            }
            params
        } else {
            vec![]
        };

        self.cursor.expect_closing_angle_bracket()?;
        Ok((type_params, mult_params))
    }

    /// Parses multiplicity arguments after `|` in a type reference: `<TypeArgs | MultArgs>`.
    ///
    /// Grammar:
    /// ```text
    /// multiplicityArguments: multiplicityArgument (',' multiplicityArgument)*
    /// multiplicityArgument: identifier | ((fromMultiplicity '..')? toMultiplicity)
    /// ```
    pub(crate) fn parse_multiplicity_arguments(&mut self) -> R<Vec<MultiplicityArgument>> {
        let mut args = Vec::new();
        loop {
            args.push(self.parse_multiplicity_argument()?);
            if !self.cursor.eat(TokenKind::Comma) {
                break;
            }
        }
        Ok(args)
    }

    /// Parses a single multiplicity argument: either an identifier or a concrete multiplicity.
    pub(crate) fn parse_multiplicity_argument(&mut self) -> R<MultiplicityArgument> {
        let si = self.cursor.current_source_info();
        match self.cursor.peek_kind() {
            TokenKind::Star => {
                self.cursor.advance();
                Ok(MultiplicityArgument::Concrete(
                    Multiplicity::zero_or_many(),
                    si,
                ))
            }
            TokenKind::IntegerLiteral => {
                let lo_tok = self.cursor.advance().clone();
                let lo: u32 = lo_tok.text.parse().unwrap_or(0);
                if self.cursor.check(TokenKind::Dot) {
                    self.cursor.advance(); // first dot
                    self.cursor.expect(TokenKind::Dot)?; // second dot
                    if self.cursor.check(TokenKind::Star) {
                        self.cursor.advance();
                        Ok(MultiplicityArgument::Concrete(
                            Multiplicity::range(lo, None),
                            si,
                        ))
                    } else {
                        let hi_tok = self.cursor.expect(TokenKind::IntegerLiteral)?;
                        let hi: u32 = hi_tok.text.parse().unwrap_or(lo);
                        Ok(MultiplicityArgument::Concrete(
                            Multiplicity::range(lo, Some(hi)),
                            si,
                        ))
                    }
                } else {
                    Ok(MultiplicityArgument::Concrete(
                        Multiplicity::range(lo, Some(lo)),
                        si,
                    ))
                }
            }
            TokenKind::Identifier => {
                let (id, _) = self.cursor.expect_identifier()?;
                Ok(MultiplicityArgument::Identifier(id, si))
            }
            _ => Err(ParseError::expected(
                "multiplicity argument (identifier, integer, or *)",
                self.cursor.peek_kind(),
                si,
            )),
        }
    }

    pub(crate) fn parse_type_variable_value(&mut self) -> R<TypeVariableValue> {
        let si = self.cursor.current_source_info();
        match self.cursor.peek_kind() {
            TokenKind::IntegerLiteral => {
                let tok = self.cursor.advance().clone();
                let val: i64 = tok.text.parse().unwrap_or(0);
                Ok(TypeVariableValue::Integer(val, si))
            }
            TokenKind::StringLiteral => {
                let tok = self.cursor.advance().clone();
                Ok(TypeVariableValue::String(unquote_string(&tok.text), si))
            }
            _ => {
                let (id, _) = self.cursor.expect_identifier()?;
                Ok(TypeVariableValue::String(id.to_string(), si))
            }
        }
    }

    pub(crate) fn parse_multiplicity(&mut self) -> R<Multiplicity> {
        match self.cursor.peek_kind() {
            TokenKind::Star => {
                self.cursor.advance();
                Ok(Multiplicity::zero_or_many())
            }
            TokenKind::IntegerLiteral => {
                let lo_tok = self.cursor.advance().clone();
                let lo: u32 = lo_tok.text.parse().unwrap_or(0);
                if self.cursor.eat(TokenKind::Dot) {
                    self.cursor.expect(TokenKind::Dot)?;
                    if self.cursor.check(TokenKind::Star) {
                        self.cursor.advance();
                        Ok(Multiplicity::range(lo, None))
                    } else {
                        let hi_tok = self.cursor.expect(TokenKind::IntegerLiteral)?;
                        let hi: u32 = hi_tok.text.parse().unwrap_or(lo);
                        Ok(Multiplicity::range(lo, Some(hi)))
                    }
                } else {
                    Ok(Multiplicity::range(lo, Some(lo)))
                }
            }
            TokenKind::Identifier => {
                let tok = self.cursor.advance().clone();
                Ok(Multiplicity::Variable(SmolStr::from(&*tok.text)))
            }
            _ => Err(ParseError::expected(
                "multiplicity",
                self.cursor.peek_kind(),
                self.cursor.current_source_info(),
            )),
        }
    }
}
