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

//! # Legend Pure — Snowflake
//!
//! The relational store's Snowflake extension
//! (`legend-engine-xt-relationalStore-snowflake-grammar`'s
//! `SnowflakeGrammarParserExtension`), registered into `dsl-relational`'s
//! relational connection grammar: the `Snowflake` datasource specification,
//! the `SnowflakePublic` authentication strategy, and the connections to
//! Snowflake in `local` mode.
//!
//! ```pure
//! ###Connection
//! RelationalDatabaseConnection my::Snowflake
//! {
//!   store: my::Db;
//!   type: Snowflake;
//!   specification: Snowflake
//!   {
//!     name: 'database';
//!     account: 'account';
//!     warehouse: 'warehouse';
//!     region: 'region';
//!   };
//!   auth: SnowflakePublic
//!   {
//!     publicUserName: 'user';
//!     privateKeyVaultReference: 'key';
//!     passPhraseVaultReference: 'phrase';
//!   };
//! }
//! ```

#![forbid(unsafe_code)]
#![deny(missing_docs)]

use legend_pure_dsl_relational::connection::field_grammar::{
    Check, Field, FieldGrammar, ValueKind,
};
use legend_pure_dsl_relational::connection::specification::{
    BodyUse, LOCAL_MODE_GRAMMARS, LocalModeGrammar, LocalSpecification, SPECIFICATION_GRAMMARS,
    SpecificationGrammar, SpecificationKind,
};
use linkme::distributed_slice;

/// `Snowflake { name: '…'; account: '…'; warehouse: '…'; region: '…'; … }`,
/// the fields in the order the walker checks them.
pub static SNOWFLAKE_DATASOURCE_SPECIFICATION: FieldGrammar = FieldGrammar {
    kind: SpecificationKind::DatasourceSpecification,
    type_name: "Snowflake",
    json_type: "snowflake",
    body_use: BodyUse::Required,
    fields: &[
        Field::string("name", Check::Required).in_protocol("databaseName"),
        Field::string("account", Check::Required).in_protocol("accountName"),
        Field::string("warehouse", Check::Required).in_protocol("warehouseName"),
        Field::string("region", Check::Required),
        Field::string("cloudType", Check::Optional),
        Field::string("quotedIdentifiersIgnoreCase", Check::Optional).of(ValueKind::Boolean),
        Field::string("enableQueryTags", Check::Optional).of(ValueKind::Boolean),
        Field::string("proxyHost", Check::Optional),
        Field::string("proxyPort", Check::Optional),
        Field::string("nonProxyHosts", Check::Optional),
        Field::string("tempTableDb", Check::Optional),
        Field::string("tempTableSchema", Check::Optional),
        Field::string("accountType", Check::Optional).of(ValueKind::Word),
        Field::string("organization", Check::Optional),
        Field::string("role", Check::Optional),
    ],
};

/// `SnowflakePublic { publicUserName: '…'; privateKeyVaultReference: '…';
/// passPhraseVaultReference: '…'; }`.
pub static SNOWFLAKE_PUBLIC_AUTHENTICATION: FieldGrammar = FieldGrammar {
    kind: SpecificationKind::AuthenticationStrategy,
    type_name: "SnowflakePublic",
    json_type: "snowflakePublic",
    body_use: BodyUse::Required,
    fields: &[
        Field::string("publicUserName", Check::Required),
        Field::string("privateKeyVaultReference", Check::Required),
        Field::string("passPhraseVaultReference", Check::Required),
    ],
};

/// A connection to Snowflake in `local` mode: its specification's and
/// strategy's values name the store, and so need one (the engine fails
/// outside its parser without).
pub struct SnowflakeLocalMode;

impl LocalModeGrammar for SnowflakeLocalMode {
    #[allow(clippy::unnecessary_literal_bound)]
    fn database_type(&self) -> &str {
        "Snowflake"
    }

    fn specifications(
        &self,
        store: Option<&str>,
    ) -> Option<(LocalSpecification, LocalSpecification)> {
        // The extension's `normalizeName`.
        let store = store?.replace("::", "-");
        let local = |name: &str| format!("legend-local-snowflake-{name}-{store}");
        Some((
            LocalSpecification {
                json_type: "snowflake",
                fields: vec![
                    ("accountName", local("accountName")),
                    ("databaseName", local("databaseName")),
                    ("role", local("role")),
                    ("warehouseName", local("warehouseName")),
                    ("region", local("region")),
                    ("cloudType", local("cloudType")),
                ],
            },
            LocalSpecification {
                json_type: "snowflakePublic",
                fields: vec![
                    (
                        "privateKeyVaultReference",
                        local("privateKeyVaultReference"),
                    ),
                    (
                        "passPhraseVaultReference",
                        local("passphraseVaultReference"),
                    ),
                    ("publicUserName", local("publicuserName")),
                ],
            },
        ))
    }
}

#[distributed_slice(SPECIFICATION_GRAMMARS)]
static SNOWFLAKE_DATASOURCE: &(dyn SpecificationGrammar + Send + Sync) =
    &SNOWFLAKE_DATASOURCE_SPECIFICATION;

#[distributed_slice(SPECIFICATION_GRAMMARS)]
static SNOWFLAKE_PUBLIC: &(dyn SpecificationGrammar + Send + Sync) =
    &SNOWFLAKE_PUBLIC_AUTHENTICATION;

#[distributed_slice(LOCAL_MODE_GRAMMARS)]
static SNOWFLAKE_LOCAL_MODE: &(dyn LocalModeGrammar + Send + Sync) = &SnowflakeLocalMode;
