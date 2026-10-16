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

//! SQL-island AST: a single node type, [`SqlExpression`], implementing
//! [`IslandContent`].

use std::any::Any;

use legend_pure_parser_ast::island::IslandContent;
use legend_pure_parser_ast::source_info::SourceInfo;
use smol_str::SmolStr;

/// The island tag this grammar claims.
pub const TAG: &str = "SQL";

/// `#SQL{ … }#`: a SQL expression, its text kept as written.
#[derive(Debug, Clone, PartialEq)]
pub struct SqlExpression {
    /// The island's body, without `#SQL{` and `}#`.
    pub text: SmolStr,
    /// Source location of the entire `#SQL{…}#` island.
    pub source_info: SourceInfo,
}

impl IslandContent for SqlExpression {
    #[allow(clippy::unnecessary_literal_bound)]
    fn tag(&self) -> &str {
        TAG
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
