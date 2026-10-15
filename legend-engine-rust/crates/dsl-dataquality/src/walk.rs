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

//! The walk (the engine's `DataQualityTreeWalker`): each element's fields
//! checked and built in the walker's order, into records shaped as the
//! engine's protocol.

use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_parser::ParserContext;
use legend_pure_parser_parser::blocks::{ReadBlock, ReadField, ReadValue};
use legend_pure_parser_parser::error::ParseError;
use legend_pure_parser_protocol::v1::record::{Record, Value};

use crate::ast::DataQualityKind;

type R<T> = Result<T, ParseError>;

/// A value the grammar can't have read where it stands: a bug.
fn misread(what: &str, at: &SourceInfo) -> ParseError {
    ParseError::Unexpected {
        message: format!("the data quality grammar misread {what}"),
        source_info: at.clone(),
    }
}

/// A pointer to an element of `element_type`, positioned on `at`.
fn pointer(element_type: &str, path: String, at: &SourceInfo) -> Record {
    Record::untyped()
        .with("type", element_type.to_string())
        .with("path", path)
        .at(at)
}

/// A field's expression as a lambda: the walkers' `visitLambda`, which
/// makes an expression that isn't one the body of a lambda.
fn lambda(field: ReadField) -> R<Value> {
    match field.value {
        ReadValue::Expression(expression) => Ok(Value::Lambda(expression)),
        _ => Err(misread("an expression", &field.source_info)),
    }
}

/// A list's names, unquoted (`fromIdentifier`), or as written
/// (`getText()`).
fn names(values: Vec<ReadValue>, as_written: bool, at: &SourceInfo) -> R<Vec<String>> {
    values
        .into_iter()
        .map(|value| match value {
            ReadValue::Identifier(name) if as_written => Ok(name.text.to_string()),
            ReadValue::Identifier(name) => Ok(name.value.to_string()),
            _ => Err(misread("a name", at)),
        })
        .collect()
}

/// The walker's `visitDataQualityValidation`, after the element's
/// annotations: its own record, without its package, name and annotations.
pub(crate) fn element(
    ctx: &mut ParserContext<'_>,
    kind: DataQualityKind,
    read: ReadBlock,
) -> R<Record> {
    match kind {
        DataQualityKind::Validation => validation(read),
        DataQualityKind::RelationValidation => relation_validation(ctx, read),
        DataQualityKind::RelationComparison => comparison(ctx, read),
    }
}

// ── Validations ──────────────────────────────────────────────────────────

/// The walker's `visitClassValidation`.
fn validation(mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let context = context(read.take("context", &at)?)?;
    let tree = root(read.take("validationTree", &at)?)?;
    // The first filter: the walker checks no more.
    let filter = read
        .all("filter")
        .into_iter()
        .next()
        .map(lambda)
        .transpose()?;
    Ok(Record::new("dataQualityValidation")
        .at(&at)
        .with("context", context)
        .with("dataQualityRootGraphFetchTree", tree)
        .with_some("filter", filter))
}

/// The walker's `visitDqContext`: a data space's context, or a mapping and
/// a runtime, unpositioned.
fn context(field: ReadField) -> R<Record> {
    let mut read = field.block()?;
    let at = read.source_info.clone();
    if read.keyword.text == "fromDataSpace" {
        let data_space = read.take("dataSpace", &at)?;
        let data_space_at = data_space.source_info.clone();
        let data_space = pointer("DATASPACE", data_space.path()?, &data_space_at);
        let name = read.take("context", &at)?.string()?;
        return Ok(Record::new("dataSpaceDataQualityExecutionContext")
            .with("dataSpace", data_space)
            .with("context", name));
    }
    let mapping = read.take("mapping", &at)?;
    let mapping_at = mapping.source_info.clone();
    let mapping = pointer("MAPPING", mapping.path()?, &mapping_at);
    let runtime = read.take("runtime", &at)?;
    let runtime_at = runtime.source_info.clone();
    let runtime = pointer("RUNTIME", runtime.path()?, &runtime_at);
    Ok(Record::new("mappingAndRuntimeDataQualityExecutionContext")
        .with("mapping", mapping)
        .with("runtime", runtime))
}

/// The walker's `visitRootGraphDefinition`, positioned on its class: its
/// constraints unquoted, without sub-type trees.
fn root(field: ReadField) -> R<Record> {
    let mut read = field.block()?;
    let at = read.source_info.clone();
    let class = read.take("class", &at)?;
    let class_at = class.source_info.clone();
    let constraints = match read.take_optional("constraints", &at)? {
        Some(constraints) => names(constraints.values()?, false, &at)?,
        None => Vec::new(),
    };
    let paths = graph_paths(&mut read, &at)?;
    Ok(Record::new("dataQualityRootGraphFetchTree")
        .at(&class_at)
        .with("class", class.path()?)
        .with("constraints", constraints)
        .with("subTrees", paths)
        .with("subTypeTrees", Vec::<Record>::new()))
}

/// A tree's paths, from its `subTrees`.
fn graph_paths(read: &mut ReadBlock, at: &SourceInfo) -> R<Vec<Record>> {
    match read.take_optional("subTrees", at)? {
        Some(paths) => paths
            .values()?
            .into_iter()
            .map(|path| graph_path(path, at))
            .collect(),
        None => Ok(Vec::new()),
    }
}

/// The walker's `visitGraphPathContext`, positioned on its property: its
/// constraints and sub-type as written, its alias without its quotes (not
/// unescaped), its parameters left out.
fn graph_path(value: ReadValue, at: &SourceInfo) -> R<Record> {
    let ReadValue::Block(mut read) = value else {
        return Err(misread("a path", at));
    };
    let at = read.source_info.clone();
    let alias = match read.take_optional("alias", &at)? {
        Some(ReadField {
            value: ReadValue::String(token),
            ..
        }) => {
            let text = token.text.as_str();
            Some(text.get(1..text.len() - 1).unwrap_or_default().to_string())
        }
        Some(_) => return Err(misread("an alias", &at)),
        None => None,
    };
    let property = read.take("property", &at)?;
    let property_at = property.source_info.clone();
    let property = property.identifier()?;
    let constraints = match read.take_optional("constraints", &at)? {
        Some(constraints) => names(constraints.values()?, true, &at)?,
        None => Vec::new(),
    };
    let sub_type = match read.take_optional("subType", &at)? {
        Some(ReadField {
            value: ReadValue::Path(path),
            ..
        }) => Some(path.text),
        Some(_) => return Err(misread("a sub-type", &at)),
        None => None,
    };
    let paths = graph_paths(&mut read, &at)?;
    Ok(Record::new("dataQualityPropertyGraphFetchTree")
        .at(&property_at)
        .with("property", property)
        .with("constraints", constraints)
        .with("parameters", Vec::<Record>::new())
        .with("subTrees", paths)
        .with("subTypeTrees", Vec::<Record>::new())
        .with_some("alias", alias)
        .with_some("subType", sub_type))
}

// ── Relation validations ─────────────────────────────────────────────────

/// The walker's `visitRelationValidation`.
fn relation_validation(ctx: &mut ParserContext<'_>, mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let query = lambda(read.take("query", &at)?)?;
    let validations = read
        .take("validations", &at)?
        .values()?
        .into_iter()
        .map(|value| validation_rule(value, &at))
        .collect::<R<Vec<_>>>()?;
    if let Some(strategy) = read.take_optional("persistenceStrategy", &at)? {
        return Err(persistence_strategy(strategy));
    }
    let test_suites = read
        .take_optional("testSuites", &at)?
        .map(|suites| test_suites(ctx, suites, DataQualityKind::RelationValidation))
        .transpose()?;
    Ok(Record::new("dataqualityRelationValidation")
        .at(&at)
        .with("query", query)
        .with("validations", validations)
        .with_some("testSuites", test_suites))
}

/// The walker's `visitValidation`, unpositioned: its fields checked on the
/// element's position, `at`.
fn validation_rule(value: ReadValue, at: &SourceInfo) -> R<Record> {
    let ReadValue::Block(mut read) = value else {
        return Err(misread("a validation", at));
    };
    let name = read.take("name", at)?.string()?;
    let description = read
        .take_optional("description", at)?
        .map(ReadField::string)
        .transpose()?;
    let assertion = lambda(read.take("assertion", at)?)?;
    let validation_type = read
        .take_optional("type", at)?
        .map(ReadField::identifier)
        .transpose()?;
    Ok(Record::untyped()
        .with("name", name)
        .with_some("description", description)
        .with("assertion", assertion)
        .with_some("type", validation_type))
}

/// A persistence strategy, `# Type { … }#`, which no linked grammar reads:
/// the engine reads one with an extension of its data quality grammar.
fn persistence_strategy(field: ReadField) -> ParseError {
    let ReadValue::Island(island) = field.value else {
        return misread("a persistence strategy", &field.source_info);
    };
    let type_name = island.tag().trim_matches(|c: char| c <= ' ');
    if type_name.is_empty() {
        return ParseError::Unexpected {
            message: "Missing persistence strategy type.  Expect '# TYPE_HERE { ... }#'"
                .to_string(),
            source_info: island.open_source_info().clone(),
        };
    }
    ParseError::Unsupported {
        message: format!(
            "no data quality persistence strategy grammar is linked for '{type_name}'"
        ),
        source_info: island.source_info.clone(),
    }
}

// ── Relation comparisons ─────────────────────────────────────────────────

/// The walker's `visitRelationComparison`.
fn comparison(ctx: &mut ParserContext<'_>, mut read: ReadBlock) -> R<Record> {
    let at = read.source_info.clone();
    let source = lambda(read.take("source", &at)?)?;
    let target = lambda(read.take("target", &at)?)?;
    let mut columns = |name: &'static str| -> R<Option<Vec<String>>> {
        read.take_optional(name, &at)?
            .map(|field| names(field.values()?, false, &at))
            .transpose()
    };
    let keys = columns("keys")?;
    let compared = columns("columnsToCompare")?;
    let persisted = columns("additionalColumnsToPersist")?;
    let strategy = strategy(read.take("strategy", &at)?)?;
    let expected_match = read
        .take_optional("expectedMatch", &at)?
        .map(expected_match)
        .transpose()?;
    let test_suites = read
        .take_optional("testSuites", &at)?
        .map(|suites| test_suites(ctx, suites, DataQualityKind::RelationComparison))
        .transpose()?;
    if let Some(strategy) = read.take_optional("persistenceStrategy", &at)? {
        return Err(persistence_strategy(strategy));
    }
    Ok(Record::new("dataQualityRelationComparison")
        .at(&at)
        .with("source", source)
        .with("target", target)
        .with_some("keys", keys)
        .with_some("columnsToCompare", compared)
        .with_some("additionalColumnsToPersist", persisted)
        .with("strategy", strategy)
        .with_some("expectedMatch", expected_match)
        .with_some("testSuites", test_suites))
}

/// The walker's `visitReconStrategy`, unpositioned: `MD5Hash`, its fields
/// checked on the strategy's field.
fn strategy(field: ReadField) -> R<Record> {
    let at = field.source_info.clone();
    let mut read = field.block()?;
    let source = read
        .take_optional("sourceHashColumn", &at)?
        .map(ReadField::identifier)
        .transpose()?;
    let target = read
        .take_optional("targetHashColumn", &at)?
        .map(ReadField::identifier)
        .transpose()?;
    let aggregated = match read.take_optional("aggregatedHash", &at)? {
        Some(ReadField {
            value: ReadValue::Boolean(value),
            ..
        }) => Some(value),
        Some(_) => return Err(misread("a boolean", &at)),
        None => None,
    };
    Ok(Record::new("md5Hash")
        .with_some("sourceHashColumn", source)
        .with_some("targetHashColumn", target)
        .with_some("aggregatedHash", aggregated))
}

/// A float, as the walker's `Double.parseDouble` reads it.
fn expected_match(field: ReadField) -> R<Value> {
    let ReadValue::Text(text, at) = field.value else {
        return Err(misread("a float", &field.source_info));
    };
    let value: f64 = text.parse().map_err(|_| misread("a float", &at))?;
    Ok(Value::Double(value))
}

// ── Test suites ──────────────────────────────────────────────────────────

/// The walker's `visitRelationValidationTestSuite` or
/// `visitRelationComparisonTestSuite`, each suite's.
fn test_suites(
    ctx: &mut ParserContext<'_>,
    field: ReadField,
    kind: DataQualityKind,
) -> R<Vec<Record>> {
    let (suite_type, test_type) = match kind {
        DataQualityKind::RelationComparison => (
            "dataQualityRelationComparisonTestSuite",
            "dataQualityRelationComparisonTest",
        ),
        _ => (
            "dataQualityRelationValidationTestSuite",
            "dataQualityRelationValidationTest",
        ),
    };
    let at = field.source_info.clone();
    field
        .values()?
        .into_iter()
        .map(|suite| {
            let ReadValue::Named(id, suite, suite_at) = suite else {
                return Err(misread("a test suite", &at));
            };
            let ReadValue::Block(mut read) = *suite else {
                return Err(misread("a test suite", &suite_at));
            };
            let data = read
                .take_optional("data", &suite_at)?
                .map(|data| test_data(ctx, data))
                .transpose()?;
            let tests = read
                .take("tests", &suite_at)?
                .values()?
                .into_iter()
                .map(|test| atomic_test(ctx, test, test_type, &suite_at))
                .collect::<R<Vec<_>>>()?;
            Ok(Record::new(suite_type)
                .at(&suite_at)
                .with("id", id.value.to_string())
                .with_some("testData", data)
                .with("tests", tests))
        })
        .collect()
}

/// A suite's data, positioned on its field: each store's embedded data.
fn test_data(ctx: &mut ParserContext<'_>, field: ReadField) -> R<Record> {
    let at = field.source_info.clone();
    let entries = field
        .values()?
        .into_iter()
        .map(|entry| {
            let ReadValue::Pair(store, data, entry_at) = entry else {
                return Err(misread("test data", &at));
            };
            let (ReadValue::Path(store), ReadValue::TypedIsland(island)) = (*store, *data) else {
                return Err(misread("test data", &entry_at));
            };
            let data = ctx.parse_embedded_data_island(&island)?;
            Ok(Record::untyped()
                .at(&entry_at)
                .with(
                    "packageableElementPointer",
                    pointer("STORE", store.path, &store.source_info),
                )
                .with("data", Value::EmbeddedData(Box::new(data))))
        })
        .collect::<R<Vec<_>>>()?;
    Ok(Record::untyped().at(&at).with("testData", entries))
}

/// The walker's `visitAtomicTest`: its assertions, each with its id.
fn atomic_test(
    ctx: &mut ParserContext<'_>,
    test: ReadValue,
    test_type: &'static str,
    at: &SourceInfo,
) -> R<Record> {
    let ReadValue::Named(id, test, test_at) = test else {
        return Err(misread("a test", at));
    };
    let ReadValue::Block(mut read) = *test else {
        return Err(misread("a test", &test_at));
    };
    let assertions = read
        .take("asserts", &test_at)?
        .values()?
        .into_iter()
        .map(|assertion| {
            let ReadValue::Named(id, assertion, assertion_at) = assertion else {
                return Err(misread("an assertion", &test_at));
            };
            let ReadValue::TypedIsland(island) = *assertion else {
                return Err(misread("an assertion", &assertion_at));
            };
            Ok((id.value, ctx.parse_test_assertion(&island)?))
        })
        .collect::<R<Vec<_>>>()?;
    Ok(Record::new(test_type)
        .at(&test_at)
        .with("id", id.value.to_string())
        .with("assertions", Value::Assertions(assertions)))
}
