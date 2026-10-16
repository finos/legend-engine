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

//! `parse_with_offset` reports AST and error positions in document
//! coordinates, the way Legend Engine section parsers receive a section
//! body plus the header's line offset.

use legend_pure_parser_ast::SourceOffset;
use legend_pure_parser_ast::element::PackageableElement;
use legend_pure_parser_ast::source_info::Spanned;

#[test]
fn element_positions_are_shifted_into_the_document() {
    // Body of a section whose header is on document line 4: the body text
    // starts with the rest of the header line (a newline).
    let offset = SourceOffset::new(3, 0).expect("valid");
    let file =
        legend_pure_parser_parser::parse_with_offset("\nClass a::A {}\n", "doc.pure", offset)
            .expect("parses");
    let class = file.all_elements().next().expect("one element");
    let si = class.source_info();
    assert_eq!((si.start_line, si.start_column), (5, 1));
    assert_eq!(si.source, "doc.pure");
    assert_eq!(class.name(), "A");
}

#[test]
fn parse_error_positions_are_shifted_into_the_document() {
    let offset = SourceOffset::new(-1, 0).expect("valid");
    let partial =
        legend_pure_parser_parser::parse_with_offset("\nClass a::A { p: }\n", "doc.pure", offset)
            .expect_err("does not parse");
    let si = partial.errors[0].source_info().expect("positioned");
    assert_eq!(si.start_line, 1);
}
