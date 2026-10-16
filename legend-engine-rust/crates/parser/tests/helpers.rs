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

//! Shared test utilities for parser integration tests.

use legend_pure_parser_ast::SourceFile;

/// Parse Pure source text successfully, returning the AST.
///
/// # Panics
///
/// Panics if the source cannot be parsed (including partial results).
#[allow(dead_code)]
#[must_use]
pub fn parse_ok(source: &str) -> SourceFile {
    legend_pure_parser_parser::parse(source, "test.pure").unwrap_or_else(|e| {
        panic!(
            "Expected parse to succeed, but got error(s): {:?}",
            e.errors.iter().map(ToString::to_string).collect::<Vec<_>>()
        )
    })
}

/// Parse Pure source text and assert it produces errors containing `expected_msg`.
///
/// # Panics
///
/// Panics if parsing succeeds fully or if no error contains the expected message.
#[allow(dead_code)]
pub fn parse_err(source: &str, expected_msg: &str) {
    match legend_pure_parser_parser::parse(source, "test.pure") {
        Ok(_) => panic!("Expected parse error containing '{expected_msg}', but parsing succeeded"),
        Err(partial) => {
            let any_match = partial.errors.iter().any(|e| {
                e.to_string()
                    .to_lowercase()
                    .contains(&expected_msg.to_lowercase())
            });
            assert!(
                any_match,
                "No error contains '{expected_msg}'. Errors: {:?}",
                partial
                    .errors
                    .iter()
                    .map(ToString::to_string)
                    .collect::<Vec<_>>()
            );
        }
    }
}

/// Load a `.pure` corpus file from the `tests/corpus/` directory.
///
/// # Panics
///
/// Panics if the corpus file cannot be read.
#[allow(dead_code)]
#[must_use]
pub fn corpus(name: &str) -> String {
    let path = format!("{}/tests/corpus/{name}", env!("CARGO_MANIFEST_DIR"));
    std::fs::read_to_string(&path)
        .unwrap_or_else(|e| panic!("Failed to read corpus file {path}: {e}"))
}
