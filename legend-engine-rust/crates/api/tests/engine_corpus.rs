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

//! Golden test against Legend Engine's Java parser: every section body in
//! `tests/engine-corpus/sections`, and every standalone expression in
//! `tests/engine-corpus/expressions`, must parse to the protocol the engine
//! produced for it, positions included, unless `known-failures.txt` lists
//! it. See `tests/engine-corpus/README.md`.

use legend_pure_dsl_authentication as _;
use legend_pure_dsl_bigquery_function as _;
use legend_pure_dsl_connection as _;
use legend_pure_dsl_data as _;
use legend_pure_dsl_data_space as _;
use legend_pure_dsl_dataquality as _;
use legend_pure_dsl_deephaven as _;
use legend_pure_dsl_diagram as _;
use legend_pure_dsl_elasticsearch as _;
use legend_pure_dsl_external_format as _;
use legend_pure_dsl_generation as _;
use legend_pure_dsl_graph as _;
use legend_pure_dsl_hosted_service as _;
use legend_pure_dsl_mapping as _;
use legend_pure_dsl_memsql_function as _;
use legend_pure_dsl_mongodb as _;
use legend_pure_dsl_persistence as _;
use legend_pure_dsl_persistence_cloud as _;
use legend_pure_dsl_persistence_relational as _;
use legend_pure_dsl_relational as _;
use legend_pure_dsl_relational_snowflake as _;
use legend_pure_dsl_runtime as _;
use legend_pure_dsl_service as _;
use legend_pure_dsl_service_store as _;
use legend_pure_dsl_snowflake as _;
use legend_pure_dsl_sql as _;
use legend_pure_dsl_store as _;
use legend_pure_dsl_tds as _;
use legend_pure_parser_api::{
    ErrorKind, ExpressionKind, ExpressionRequest, SectionError, SectionRequest, parse_expression,
    parse_section,
};
use serde_json::{Value, json};
use std::collections::BTreeMap;
use std::fs;
use std::path::{Path, PathBuf};

const CORPUS: &str = concat!(env!("CARGO_MANIFEST_DIR"), "/../../tests/engine-corpus");

/// The corpus directories, each with how its entries are parsed.
const PARTS: [Part; 2] = [
    Part {
        dir: "sections",
        parse: parse_section_entry,
    },
    Part {
        dir: "expressions",
        parse: parse_expression_entry,
    },
];

/// A corpus directory, and how its entries are parsed.
struct Part {
    dir: &'static str,
    /// The Java parser's result and the Rust parser's, both reduced to what
    /// is compared.
    parse: fn(&Entry) -> (Value, Result<Value, SectionError>),
}

/// One engine section or standalone expression: its text, the engine's
/// context (section type or expression kind, source id, offsets) and the
/// Java parser's result.
struct Entry {
    hash: String,
    code: String,
    meta: Value,
    java: Value,
}

#[test]
fn engine_corpus_matches_java_parser() {
    // `ENGINE_CORPUS=<dir>` compares a freshly dumped corpus instead (its
    // `sections/` and `expressions/`, and its `known-failures.txt` if there
    // is one).
    let override_dir = std::env::var_os("ENGINE_CORPUS").map(PathBuf::from);
    let corpus = override_dir.as_deref().unwrap_or_else(|| Path::new(CORPUS));
    let mut parts = Vec::new();
    for part in &PARTS {
        let dir = corpus.join(part.dir);
        // A fresh dump may hold sections or expressions only.
        if override_dir.is_none() || dir.exists() {
            parts.push((part, load_entries(&dir)));
        }
    }
    assert!(
        !parts.is_empty(),
        "engine corpus {} has neither sections/ nor expressions/",
        corpus.display()
    );
    let known_failures = corpus.join("known-failures.txt");
    let known = if override_dir.is_some() && !known_failures.exists() {
        BTreeMap::new()
    } else {
        load_known_failures(&known_failures)
    };
    let report_dir = Path::new(env!("CARGO_TARGET_TMPDIR")).join("engine-corpus");
    // A fresh report per run: only the current failures are in it.
    let _ = fs::remove_dir_all(&report_dir);

    let mut matched = 0;
    let mut unexpected = Vec::new();
    let mut fixed = Vec::new();
    for (part, entries) in &parts {
        for entry in entries {
            let name = format!("{}/{}", part.dir, entry.hash);
            match check(entry, part.parse, &report_dir.join(part.dir)) {
                Ok(()) if known.contains_key(&entry.hash) => fixed.push(name),
                Ok(()) => matched += 1,
                Err(difference) if !known.contains_key(&entry.hash) => {
                    unexpected.push(format!("{name}  {difference}"));
                }
                Err(_) => {}
            }
        }
    }
    let stale: Vec<&str> = known
        .keys()
        .filter(|hash| {
            !parts
                .iter()
                .any(|(_, entries)| entries.iter().any(|e| &e.hash == *hash))
        })
        .map(String::as_str)
        .collect();

    let problems: String = [
        problem(
            &unexpected,
            "difference(s) from the engine (fix them, or list them in known-failures.txt with a \
             reason)",
        ),
        problem(
            &fixed,
            "known failure(s) now match; remove them from known-failures.txt",
        ),
        problem(
            &stale,
            "known failure(s) are not in the corpus; remove them from known-failures.txt",
        ),
    ]
    .into_iter()
    .flatten()
    .collect();
    let sizes: Vec<String> = parts
        .iter()
        .map(|(part, entries)| format!("{} {}", entries.len(), part.dir))
        .collect();
    assert!(
        problems.is_empty(),
        "engine corpus ({}, {matched} matching, {} known failures): {problems}\n\
         corpus: {}\ncanonical Java and Rust results of each failure: {}",
        sizes.join(", "),
        known.len(),
        corpus.display(),
        report_dir.display()
    );
}

/// One block of the failure message: a count and heading, then one line per
/// entry.
fn problem(lines: &[impl AsRef<str>], heading: &str) -> Option<String> {
    let lines: Vec<&str> = lines.iter().map(AsRef::as_ref).collect();
    (!lines.is_empty()).then(|| format!("\n{} {heading}:\n{}\n", lines.len(), lines.join("\n")))
}

/// A section as the engine's section parser would have the Rust parser parse
/// it, positions on: its elements and imports.
fn parse_section_entry(entry: &Entry) -> (Value, Result<Value, SectionError>) {
    let request = SectionRequest {
        section_type: text(&entry.meta, "sectionType"),
        code: &entry.code,
        source_id: text(&entry.meta, "sourceId"),
        line_offset: offset(&entry.meta, "lineOffset"),
        column_offset: offset(&entry.meta, "columnOffset"),
        return_source_info: true,
    };
    // `imports` is absent for a section whose grammar has none (the engine's
    // `DefaultCodeSection`), and `[]` compares as absent: `sectionClass`
    // keeps the difference.
    let section_class = |has_imports: bool| {
        if has_imports {
            "importAware"
        } else {
            "default"
        }
    };
    let rust = parse_section(&request).map(|result| {
        json!({
            "sectionClass": section_class(result.imports.is_some()),
            "elements": result.elements,
            "imports": result.imports,
        })
    });
    let java = json!({
        "sectionClass": section_class(entry.java.get("imports").is_some()),
        "elements": entry.java["elements"],
        "imports": entry.java["imports"],
    });
    (java, rust)
}

/// A standalone expression as the engine's expression parser would have the
/// Rust parser parse it, positions on: its value.
fn parse_expression_entry(entry: &Entry) -> (Value, Result<Value, SectionError>) {
    let kind = text(&entry.meta, "kind");
    let request = ExpressionRequest {
        kind: ExpressionKind::from_name(kind)
            .unwrap_or_else(|| panic!("meta.json: unknown kind {kind}")),
        code: &entry.code,
        source_id: text(&entry.meta, "sourceId"),
        line_offset: offset(&entry.meta, "lineOffset"),
        column_offset: offset(&entry.meta, "columnOffset"),
        return_source_info: true,
    };
    let rust = parse_expression(&request).map(|value| json!({ "value": value }));
    (json!({ "value": entry.java["value"] }), rust)
}

/// Parses `entry` with the Rust parser and compares the result with the
/// Java parser's.
///
/// Returns the first difference, on one line.
fn check(
    entry: &Entry,
    parse: fn(&Entry) -> (Value, Result<Value, SectionError>),
    report_dir: &Path,
) -> Result<(), String> {
    let (java, rust) = parse(entry);
    match (entry.java.get("error"), rust) {
        (_, Err(e)) if e.kind == ErrorKind::Unsupported => {
            Err(format!("rust: unsupported: {}", one_line(&e.message)))
        }
        (None, Ok(rust)) => {
            let (java, rust) = (canonical(java), canonical(rust));
            if java == rust {
                return Ok(());
            }
            write_report(report_dir, &entry.hash, &java, &rust);
            Err(first_difference(&java, &rust, ""))
        }
        (None, Err(e)) => Err(format!(
            "java parsed it; rust reported {}: {}",
            kind_name(e.kind),
            one_line(&e.message)
        )),
        (Some(error), Ok(_)) => Err(format!(
            "java rejected it ({}); rust parsed it",
            one_line(error["message"].as_str().unwrap_or_default())
        )),
        (Some(error), Err(e)) => {
            let java_message = one_line(error["message"].as_str().unwrap_or_default());
            if e.kind != ErrorKind::Parser {
                return Err(format!(
                    "java rejected it ({java_message}); rust reported {}: {}",
                    kind_name(e.kind),
                    one_line(&e.message)
                ));
            }
            // As in the engine's parity harness: the error's start
            // position, not ANTLR's wording.
            let Some(java_si) = error.get("sourceInformation") else {
                return Ok(());
            };
            let java_start = (&java_si["startLine"], &java_si["startColumn"]);
            let rust_start = e.source_information.as_ref().map(|si| {
                let si = serde_json::to_value(si).expect("source information serializes");
                (si["startLine"].clone(), si["startColumn"].clone())
            });
            match rust_start {
                Some((line, column)) if (&line, &column) == java_start => Ok(()),
                _ => Err(format!(
                    "error position differs: java {}:{} ({java_message}), rust {} ({})",
                    java_start.0,
                    java_start.1,
                    rust_start.map_or_else(|| "(none)".to_string(), |(l, c)| format!("{l}:{c}")),
                    one_line(&e.message)
                )),
            }
        }
    }
}

/// Drops what the engine reads back as absent: nulls (its mapper omits
/// them) and empty arrays (its protocol lists default to empty). Nothing
/// else is normalized: any other difference is a protocol difference.
fn canonical(value: Value) -> Value {
    match value {
        Value::Object(map) => Value::Object(
            map.into_iter()
                .map(|(key, value)| (key, canonical(value)))
                .filter(|(_, value)| {
                    !value.is_null() && value.as_array().is_none_or(|a| !a.is_empty())
                })
                .collect(),
        ),
        Value::Array(items) => Value::Array(items.into_iter().map(canonical).collect()),
        other => other,
    }
}

/// The JSON path of the first difference between the two results, with
/// both values.
fn first_difference(java: &Value, rust: &Value, path: &str) -> String {
    match (java, rust) {
        (Value::Object(j), Value::Object(r)) => {
            for (name, java_value) in j {
                let child = format!("{path}/{name}");
                match r.get(name) {
                    Some(rust_value) if rust_value == java_value => {}
                    Some(rust_value) => return first_difference(java_value, rust_value, &child),
                    None => return describe(&child, Some(java_value), None),
                }
            }
            if let Some((name, rust_value)) = r.iter().find(|(name, _)| !j.contains_key(*name)) {
                return describe(&format!("{path}/{name}"), None, Some(rust_value));
            }
        }
        (Value::Array(j), Value::Array(r)) => {
            if let Some(i) = (0..j.len().min(r.len())).find(|&i| j[i] != r[i]) {
                return first_difference(&j[i], &r[i], &format!("{path}/{i}"));
            }
            if j.len() != r.len() {
                return format!("{path} (size): java={} rust={}", j.len(), r.len());
            }
        }
        _ => {}
    }
    describe(path, Some(java), Some(rust))
}

fn describe(path: &str, java: Option<&Value>, rust: Option<&Value>) -> String {
    let show = |v: Option<&Value>| {
        v.map_or_else(|| "(absent)".to_string(), |v| abbreviate(&v.to_string()))
    };
    let path = if path.is_empty() { "/" } else { path };
    format!("{path}: java={} rust={}", show(java), show(rust))
}

fn abbreviate(text: &str) -> String {
    match text.char_indices().nth(200) {
        Some((end, _)) => format!("{}...", &text[..end]),
        None => text.to_string(),
    }
}

fn one_line(message: &str) -> String {
    abbreviate(&message.replace('\n', "\\n"))
}

fn kind_name(kind: ErrorKind) -> &'static str {
    match kind {
        ErrorKind::Parser => "parser",
        ErrorKind::Unsupported => "unsupported",
        ErrorKind::Internal => "internal",
    }
}

/// Both canonical results, for diffing a failure.
fn write_report(dir: &Path, hash: &str, java: &Value, rust: &Value) {
    let pretty = |v: &Value| serde_json::to_string_pretty(v).expect("JSON renders");
    fs::create_dir_all(dir).expect("report directory");
    fs::write(dir.join(format!("{hash}.java.json")), pretty(java)).expect("report");
    fs::write(dir.join(format!("{hash}.rust.json")), pretty(rust)).expect("report");
}

fn text<'a>(meta: &'a Value, field: &str) -> &'a str {
    meta[field]
        .as_str()
        .unwrap_or_else(|| panic!("meta.json: missing {field}"))
}

fn offset(meta: &Value, field: &str) -> i32 {
    meta[field]
        .as_i64()
        .and_then(|n| i32::try_from(n).ok())
        .unwrap_or_else(|| panic!("meta.json: bad {field}: {}", meta[field]))
}

/// Every `<hash>.meta.json` with its `<hash>.pure` and `<hash>.java.json`.
/// An empty, missing or incomplete corpus fails the test rather than
/// passing it vacuously.
fn load_entries(dir: &Path) -> Vec<Entry> {
    let read = |path: PathBuf| {
        fs::read_to_string(&path).unwrap_or_else(|e| panic!("{}: {e}", path.display()))
    };
    let json = |path: PathBuf| {
        serde_json::from_str::<Value>(&read(path.clone()))
            .unwrap_or_else(|e| panic!("{}: {e}", path.display()))
    };
    let mut hashes: Vec<String> = fs::read_dir(dir)
        .unwrap_or_else(|e| panic!("engine corpus {}: {e}", dir.display()))
        .map(|entry| {
            entry
                .expect("corpus entry")
                .file_name()
                .to_string_lossy()
                .into_owned()
        })
        .filter_map(|name| name.strip_suffix(".meta.json").map(str::to_string))
        .collect();
    hashes.sort();
    assert!(
        !hashes.is_empty(),
        "engine corpus {} has no entries",
        dir.display()
    );
    hashes
        .into_iter()
        .map(|hash| Entry {
            code: read(dir.join(format!("{hash}.pure"))),
            meta: json(dir.join(format!("{hash}.meta.json"))),
            java: json(dir.join(format!("{hash}.java.json"))),
            hash,
        })
        .collect()
}

/// `<hash>  <category>: <reason>` lines; `#` starts a comment line.
fn load_known_failures(path: &Path) -> BTreeMap<String, String> {
    let text = fs::read_to_string(path).unwrap_or_else(|e| panic!("{}: {e}", path.display()));
    let mut known = BTreeMap::new();
    for line in text
        .lines()
        .map(str::trim)
        .filter(|l| !l.is_empty() && !l.starts_with('#'))
    {
        let (hash, reason) = line.split_once(char::is_whitespace).unwrap_or((line, ""));
        assert!(
            !reason.trim().is_empty(),
            "known-failures.txt: {hash} has no reason"
        );
        let previous = known.insert(hash.to_string(), reason.trim().to_string());
        assert!(
            previous.is_none(),
            "known-failures.txt: {hash} is listed twice"
        );
    }
    known
}
