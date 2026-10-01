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

//! The native library carries the grammars it links: `capabilities()`
//! (what Java's `NativeGrammarParser.capabilities()` returns) lists them.

#[test]
fn library_links_the_section_and_island_grammars() {
    let caps = legend_pure_rust_parser::linked_capabilities();
    assert_eq!(
        caps.sections,
        [
            "Pure",
            "Connection",
            "Data",
            "DataQualityValidation",
            "DataSpace",
            "Diagram",
            "ExternalFormat",
            "Mapping",
            "Persistence",
            "Relational",
            "Runtime",
            "Service",
            "ServiceStore",
            "Snowflake"
        ]
    );
    assert!(
        caps.islands.contains(&String::new()),
        "graph fetch: {caps:?}"
    );
    assert!(
        caps.islands.contains(&">".to_string()),
        "store accessor: {caps:?}"
    );
    assert!(
        caps.islands.contains(&"SQL".to_string()),
        "SQL expression: {caps:?}"
    );
    assert!(
        caps.islands.contains(&"TDS".to_string()),
        "TDS literal: {caps:?}"
    );
}
