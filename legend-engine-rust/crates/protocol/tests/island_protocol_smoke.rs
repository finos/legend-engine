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

//! Locks the `IslandProtocol` plug-in contract on the protocol crate
//! side. A mock converter — defined entirely outside the protocol
//! crate, the way DSL crates would — registers for a tag, and the
//! `dispatch_island_convert` helper routes a matching island to it.

use std::any::Any;

use legend_pure_parser_ast::island::{IslandContent, IslandExpression};
use legend_pure_parser_ast::source_info::SourceInfo;
use legend_pure_parser_protocol::island_protocol::{IslandProtocol, dispatch_island_convert};
use legend_pure_parser_protocol::v1::value_spec::{ClassInstance, ValueSpecification};
use smol_str::SmolStr;

// ---------------------------------------------------------------------------
// Mock content + protocol converter (would live in a DSL crate)
// ---------------------------------------------------------------------------

#[derive(Debug, Clone, PartialEq)]
struct MockContent {
    payload: SmolStr,
}

impl IslandContent for MockContent {
    fn tag(&self) -> &'static str {
        "MOCK"
    }
    fn as_any(&self) -> &dyn Any {
        self
    }
    fn clone_box(&self) -> Box<dyn IslandContent> {
        Box::new(self.clone())
    }
    fn eq_content(&self, other: &dyn IslandContent) -> bool {
        other
            .as_any()
            .downcast_ref::<Self>()
            .is_some_and(|o| self == o)
    }
}

struct MockProtocol;

impl IslandProtocol for MockProtocol {
    fn tag(&self) -> &'static str {
        "MOCK"
    }
    fn convert(
        &self,
        content: &dyn IslandContent,
    ) -> legend_pure_parser_protocol::island_protocol::Result<ValueSpecification> {
        let m = content
            .as_any()
            .downcast_ref::<MockContent>()
            .expect("MockContent");
        Ok(ValueSpecification::ClassInstance(ClassInstance {
            type_name: "mockIsland".to_string(),
            value: serde_json::json!({ "payload": m.payload.as_str() }),
            source_information: None,
        }))
    }
}

#[test]
fn dispatch_routes_to_matching_protocol_converter() {
    let island = IslandExpression {
        content: Box::new(MockContent {
            payload: SmolStr::new("hello"),
        }),
        source_info: SourceInfo::new("smoke.pure", 1, 1, 1, 8),
    };

    let protocol = MockProtocol;
    let protocols: [&dyn IslandProtocol; 1] = [&protocol];

    let result = dispatch_island_convert(&island, &protocols).expect("convert ok");
    let value = result.expect("converter matched");
    let ValueSpecification::ClassInstance(ci) = value else {
        panic!("expected ClassInstance, got {value:?}");
    };
    assert_eq!(ci.type_name, "mockIsland");
    assert_eq!(ci.value["payload"].as_str(), Some("hello"));
}

#[test]
fn dispatch_returns_none_for_unregistered_tag() {
    let island = IslandExpression {
        content: Box::new(MockContent {
            payload: SmolStr::new("hello"),
        }),
        source_info: SourceInfo::new("smoke.pure", 1, 1, 1, 8),
    };

    // No protocol registered — the dispatcher should return None,
    // letting callers fall back to a built-in path or report an error.
    let result = dispatch_island_convert(&island, &[]).expect("no error");
    assert!(result.is_none(), "expected None for unregistered tag");
}
