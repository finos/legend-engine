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

//! Protocol `Multiplicity` — cardinality bounds.
//!
//! Maps to Java `org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity`.
//!
//! JSON examples:
//! ```json
//! { "lowerBound": 1, "upperBound": 1 }       // [1]    — exactly one
//! { "lowerBound": 0, "upperBound": 1 }       // [0..1] — zero or one
//! { "lowerBound": 0 }                         // [*]    — zero to many
//! { "lowerBound": 1 }                         // [1..*] — one to many
//! ```
//!
//! When `upperBound` is infinite, Java's `getUpperBound()` returns `null` and
//! Jackson omits the field. We mirror this with `Option<u32>` + `skip_serializing_if`.

use serde::{Deserialize, Serialize};

/// Cardinality bounds for a property or parameter.
///
/// Maps 1:1 to Java `org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity`.
///
/// - `upperBound = None` means unbounded (infinite / `*`).
/// - `upperBound = Some(n)` means bounded to at most `n`.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Multiplicity {
    /// The minimum number of values (inclusive).
    pub lower_bound: u32,
    /// The maximum number of values (inclusive), or `None` for unbounded (`*`).
    ///
    /// When `None`, the field is omitted from JSON — Java interprets a missing
    /// `upperBound` as infinite.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub upper_bound: Option<u32>,
}

impl Multiplicity {
    /// Exactly one: `[1]`.
    pub const PURE_ONE: Self = Self {
        lower_bound: 1,
        upper_bound: Some(1),
    };

    /// Zero or one: `[0..1]`.
    pub const ZERO_ONE: Self = Self {
        lower_bound: 0,
        upper_bound: Some(1),
    };

    /// Zero to many: `[*]`.
    pub const ZERO_MANY: Self = Self {
        lower_bound: 0,
        upper_bound: None,
    };

    /// One to many: `[1..*]`.
    pub const ONE_MANY: Self = Self {
        lower_bound: 1,
        upper_bound: None,
    };

    /// The fully-qualified path of the well-known M3 multiplicity
    /// instance corresponding to this concrete bound pair, if one
    /// exists. Java parity: the predefined
    /// `meta::pure::metamodel::multiplicity::{PureOne, ZeroOne,
    /// ZeroMany, OneMany}` `PackageableMultiplicity` instances.
    /// Returns `None` for arbitrary ranges (e.g. `2..5`) that need a
    /// fresh `MultiplicityInstance` instead.
    ///
    /// Used by the multiplicity-literal protocol wire form
    /// (`@[m]` value specifications): a literal whose bounds match
    /// one of the four well-known instances emits as
    /// `packageableElementPtr`; everything else emits as
    /// `classInstance("multiplicity", { lowerBound, upperBound? })`.
    #[must_use]
    pub fn well_known_path(&self) -> Option<&'static str> {
        match (self.lower_bound, self.upper_bound) {
            (1, Some(1)) => Some("meta::pure::metamodel::multiplicity::PureOne"),
            (0, Some(1)) => Some("meta::pure::metamodel::multiplicity::ZeroOne"),
            (0, None) => Some("meta::pure::metamodel::multiplicity::ZeroMany"),
            (1, None) => Some("meta::pure::metamodel::multiplicity::OneMany"),
            _ => None,
        }
    }

    /// Returns `true` if unbounded (upper bound is `*`).
    #[must_use]
    pub fn is_infinite(&self) -> bool {
        self.upper_bound.is_none()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_pure_one_serialization() {
        let m = Multiplicity::PURE_ONE;
        let json = serde_json::to_value(&m).unwrap();
        assert_eq!(json["lowerBound"], 1);
        assert_eq!(json["upperBound"], 1);

        let obj = json.as_object().unwrap();
        assert_eq!(obj.len(), 2);
    }

    #[test]
    fn test_zero_one_serialization() {
        let m = Multiplicity::ZERO_ONE;
        let json = serde_json::to_value(&m).unwrap();
        assert_eq!(json["lowerBound"], 0);
        assert_eq!(json["upperBound"], 1);
    }

    #[test]
    fn test_infinite_omits_upper_bound() {
        // [*] — upperBound should be omitted, NOT null
        let m = Multiplicity::ZERO_MANY;
        let json = serde_json::to_value(&m).unwrap();
        assert_eq!(json["lowerBound"], 0);
        assert!(
            json.get("upperBound").is_none(),
            "upperBound must be omitted for [*]"
        );

        let obj = json.as_object().unwrap();
        assert_eq!(obj.len(), 1, "only lowerBound should be present");
    }

    #[test]
    fn test_one_to_many_omits_upper_bound() {
        // [1..*] — upperBound should be omitted
        let m = Multiplicity::ONE_MANY;
        let json = serde_json::to_value(&m).unwrap();
        assert_eq!(json["lowerBound"], 1);
        assert!(json.get("upperBound").is_none());
    }

    #[test]
    fn test_deserialize_with_upper_bound() {
        let java_json = r#"{"lowerBound": 1, "upperBound": 1}"#;
        let m: Multiplicity = serde_json::from_str(java_json).unwrap();
        assert_eq!(m, Multiplicity::PURE_ONE);
        assert!(!m.is_infinite());
    }

    #[test]
    fn test_deserialize_without_upper_bound() {
        // Java serializes [*] as {"lowerBound": 0} — no upperBound field
        let java_json = r#"{"lowerBound": 0}"#;
        let m: Multiplicity = serde_json::from_str(java_json).unwrap();
        assert_eq!(m, Multiplicity::ZERO_MANY);
        assert!(m.is_infinite());
    }

    #[test]
    fn test_roundtrip() {
        for m in [
            Multiplicity::PURE_ONE,
            Multiplicity::ZERO_ONE,
            Multiplicity::ZERO_MANY,
            Multiplicity::ONE_MANY,
        ] {
            let json_str = serde_json::to_string(&m).unwrap();
            let back: Multiplicity = serde_json::from_str(&json_str).unwrap();
            assert_eq!(back, m);
        }
    }
}
