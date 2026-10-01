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

use legend_pure_parser_protocol::v1;

#[test]
fn full_ast_roundtrip() {
    let source = r"
Profile meta::pure::profiles::temporal
{
    stereotypes: [businesstemporal, processingtemporal];
    tags: [date, author];
}

Class <<meta::pure::profiles::temporal.businesstemporal>> meta::pure::domain::Person extends meta::pure::domain::LegalEntity
{
    {meta::pure::profiles::temporal.date = '2024-01-01'} name: String[1];
    age: Integer[0..1];
    address: meta::pure::domain::Address[0..*];
}

Class meta::pure::domain::LegalEntity {
    id: Integer[1];
}

Class meta::pure::domain::Address {
    street: String[1];
}

Enum meta::pure::domain::Status {
    ACTIVE,
    INACTIVE
}

Association meta::pure::domain::PersonAddress {
    person: meta::pure::domain::Person[1];
    address: meta::pure::domain::Address[*];
}

Measure meta::pure::domain::Distance {
    *Meter: x -> $x;
    Kilometer: x -> $x * 1000;
}

function meta::pure::domain::greet(person: meta::pure::domain::Person[1]): String[1] {
    let s = 'Hello: ' + $person.name;
    let b = true && false || (1 == 2) || (10 < 20);
    let m = [1.23, 4.56, 123.456];
    let lst = [
        'Peter', 'Mary'
    ];
    let t = %2024-01-01T00:00:00Z;
    let q = {| $person.age};
    $s;
}
";

    let sf = legend_pure_parser_parser::parse(source, "test.pure").expect("parse failed");

    // 1. AST -> Protocol
    let mut protocol_elements = vec![];
    for ast_elem in sf.all_elements() {
        let p_elem =
            v1::convert::convert_element(ast_elem).expect("Failed to convert AST to Protocol");
        protocol_elements.push(p_elem);
    }

    for p_elem in &protocol_elements {
        let _ast_elem_opt = match v1::from_protocol::convert_element(p_elem) {
            Ok(Some(el)) => el,
            Ok(None) => continue,
            Err(e) => {
                let json = serde_json::to_string_pretty(&p_elem).unwrap();
                std::fs::write("/tmp/err.json", json).unwrap();
                panic!("Failed to convert piece (written to /tmp/err.json), Error: {e:?}");
            }
        };
    }
}

#[test]
fn context_roundtrip() {
    let source = r"
###Pure
import meta::pure::domain::*;
import meta::pure::profiles::*;

Profile meta::pure::profiles::temporal { stereotypes: [businesstemporal]; }

Class <<meta::pure::profiles::temporal.businesstemporal>> meta::pure::domain::Person {
    name: String[1];
}

Class meta::pure::domain::Employee extends meta::pure::domain::Person [
    validName: $this.name != ''
]
{
    details: meta::pure::domain::Details[1];
    
    fullName(title: String[1]) {
        $title + ' ' + $this.name
    }: String[1];
}

Class meta::pure::domain::Details {
}

Association meta::pure::domain::PersonDetails {
    person: meta::pure::domain::Person[1];
    details: meta::pure::domain::Details[1];
}

###Pure
import meta::pure::other::*;

Enum meta::pure::other::MyEnum {
    A, B
}
";
    let sf = legend_pure_parser_parser::parse(source, "test.pure").expect("parse failed");
    let context =
        v1::convert::convert_source_file(&sf).expect("Failed to convert source file to context");

    let sf2 = v1::from_protocol::convert_context_to_source_file(&context)
        .expect("Failed to convert context to source file");

    assert_eq!(sf2.sections.len(), 2, "Should have 2 sections");
    assert_eq!(sf2.sections[0].kind, "Pure");
    assert_eq!(sf2.sections[1].kind, "Pure");
    assert_eq!(sf2.element_count(), 6, "Should have 6 elements");
}

// Graph-fetch protocol-conversion test (the `#{ … }#` island) lives in
// `crates/dsl-graph/tests/protocol_roundtrip_tests.rs`. The core
// protocol crate carries no graph-fetch identifiers.
