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

//! The relational store's authentication strategies
//! (`AuthenticationStrategyParserGrammar` and its
//! `AuthenticationStrategyParseTreeWalker`):
//!
//! ```text
//! DefaultH2    Test    GCPApplicationDefaultCredentials
//! DelegatedKerberos { serverPrincipal: '…'; }?
//! MiddleTierUserNamePassword { vaultReference: '…'; }?
//! UserNamePassword { baseVaultReference: '…'; userNameVaultReference: '…'; passwordVaultReference: '…'; }
//! ApiToken { apiToken: '…'; }
//! GCPWorkloadIdentityFederation { serviceAccountEmail: '…'; additionalGcpScopes: [ '…', … ]; }
//! OAuth { oauthKey: '…'; scopeName: '…'; }
//! ```
//!
//! The fields in any order, positioned on the `auth` field, where the walker
//! reports one missing or written twice. `DelegatedKerberos`'s and
//! `MiddleTierUserNamePassword`'s walkers check their field before they know
//! where the strategy is, and fail outside the parser when it is missing:
//! the engine's to report.

use super::field_grammar::{Check, Field, FieldGrammar, ValueKind};
use super::specification::{BodyUse, SpecificationGrammar, SpecificationKind};

/// The relational store's authentication strategy grammars.
pub(crate) static CORE_AUTHENTICATION_STRATEGIES: [&(dyn SpecificationGrammar + Send + Sync); 9] = [
    &without_body("DefaultH2", "h2Default"),
    &without_body("Test", "test"),
    &without_body(
        "GCPApplicationDefaultCredentials",
        "gcpApplicationDefaultCredentials",
    ),
    &FieldGrammar {
        kind: SpecificationKind::AuthenticationStrategy,
        type_name: "DelegatedKerberos",
        json_type: "delegatedKerberos",
        body_use: BodyUse::Optional,
        fields: &[Field::string("serverPrincipal", Check::Unpositioned)],
    },
    &FieldGrammar {
        kind: SpecificationKind::AuthenticationStrategy,
        type_name: "MiddleTierUserNamePassword",
        json_type: "middleTierUserNamePassword",
        body_use: BodyUse::Optional,
        fields: &[Field::string("vaultReference", Check::Unpositioned)],
    },
    &FieldGrammar {
        kind: SpecificationKind::AuthenticationStrategy,
        type_name: "UserNamePassword",
        json_type: "userNamePassword",
        body_use: BodyUse::Required,
        fields: &[
            Field::string("baseVaultReference", Check::Optional),
            Field::string("userNameVaultReference", Check::Required),
            Field::string("passwordVaultReference", Check::Required),
        ],
    },
    &FieldGrammar {
        kind: SpecificationKind::AuthenticationStrategy,
        type_name: "ApiToken",
        json_type: "apiToken",
        body_use: BodyUse::Required,
        fields: &[Field::string("apiToken", Check::Required)],
    },
    &FieldGrammar {
        kind: SpecificationKind::AuthenticationStrategy,
        type_name: "GCPWorkloadIdentityFederation",
        json_type: "gcpWorkloadIdentityFederation",
        body_use: BodyUse::Required,
        fields: &[
            Field::string("serviceAccountEmail", Check::Required),
            Field::string("additionalGcpScopes", Check::Optional).of(ValueKind::Strings),
        ],
    },
    &FieldGrammar {
        kind: SpecificationKind::AuthenticationStrategy,
        type_name: "OAuth",
        json_type: "oauth",
        body_use: BodyUse::Required,
        fields: &[
            Field::string("oauthKey", Check::Required),
            Field::string("scopeName", Check::Required),
        ],
    },
];

/// A strategy without a body.
const fn without_body(type_name: &'static str, json_type: &'static str) -> FieldGrammar {
    FieldGrammar {
        kind: SpecificationKind::AuthenticationStrategy,
        type_name,
        json_type,
        body_use: BodyUse::None,
        fields: &[],
    }
}
