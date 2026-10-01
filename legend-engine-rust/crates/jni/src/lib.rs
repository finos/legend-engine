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

//! # Legend Pure Parser — JNI bridge
//!
//! The native half of `org.finos.legend.pure.rust.parser.NativeGrammarParser`
//! (in the engine module `legend-engine-language-pure-grammar-rust`), whose
//! static methods return JSON strings produced by `legend_pure_parser_api`:
//!
//! | Java | Rust |
//! |---|---|
//! | `String capabilities()` | [`legend_pure_parser_api::capabilities_json`] |
//! | `String parseSection(String sectionType, String code, String sourceId, int lineOffset, int columnOffset, boolean returnSourceInfo)` | [`legend_pure_parser_api::parse_section_json`] |
//! | `String parseExpression(String kind, String code, String sourceId, int lineOffset, int columnOffset, boolean returnSourceInfo)` | [`legend_pure_parser_api::parse_expression_json`] |
//!
//! The surface is generic: adding a grammar never changes it. The grammars
//! available are whatever crates are linked into the library — here every
//! grammar crate of the workspace (see the crate's README). An aggregator
//! (e.g. one linking legend-pure's grammars with the engine's, once they live
//! apart again) depends on this crate as an rlib, links its grammars, and
//! builds its own cdylib exporting the same symbols.
//!
//! Every failure is reported inside the returned JSON
//! (`{"error":{"kind","message",…}}`), panics included: nothing unwinds
//! across the FFI boundary. The only non-JSON outcome is a `null` string,
//! when the JVM cannot allocate the result; Java treats it as an internal
//! error.
//!
//! This is the only crate in the workspace with `unsafe` code: exporting an
//! unmangled symbol is `#[unsafe(no_mangle)]` in edition 2024.

#![deny(missing_docs)]

use std::panic::{AssertUnwindSafe, catch_unwind};

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jstring};
use legend_pure_parser_api::{
    ErrorKind, ExpressionKind, ExpressionRequest, SectionError, SectionRequest,
};

// Link the grammar plug-ins so their self-registrations are in this library.
use legend_pure_dsl_authentication as _;
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
use legend_pure_dsl_mapping as _;
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

/// The capabilities of the grammars linked into this library.
///
/// The same value `NativeGrammarParser.capabilities()` returns as JSON; a
/// safe entry point for tests and embedders.
#[must_use]
pub fn linked_capabilities() -> legend_pure_parser_api::Capabilities {
    legend_pure_parser_api::capabilities()
}

/// `static native String capabilities()`.
#[unsafe(no_mangle)]
pub extern "system" fn Java_org_finos_legend_pure_rust_parser_NativeGrammarParser_capabilities<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> jstring {
    respond(
        &mut env,
        |_| Ok(legend_pure_parser_api::capabilities_json()),
    )
}

/// `static native String parseSection(String sectionType, String code,
/// String sourceId, int lineOffset, int columnOffset,
/// boolean returnSourceInfo)`.
#[unsafe(no_mangle)]
#[allow(clippy::too_many_arguments)] // Mirrors the Java signature.
pub extern "system" fn Java_org_finos_legend_pure_rust_parser_NativeGrammarParser_parseSection<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    section_type: JString<'local>,
    code: JString<'local>,
    source_id: JString<'local>,
    line_offset: jint,
    column_offset: jint,
    return_source_info: jboolean,
) -> jstring {
    respond(&mut env, |env| {
        let section_type = java_string(env, &section_type, "sectionType")?;
        let code = java_string(env, &code, "code")?;
        let source_id = java_string(env, &source_id, "sourceId")?;
        Ok(legend_pure_parser_api::parse_section_json(
            &SectionRequest {
                section_type: &section_type,
                code: &code,
                source_id: &source_id,
                line_offset,
                column_offset,
                return_source_info: return_source_info != 0,
            },
        ))
    })
}

/// `static native String parseExpression(String kind, String code,
/// String sourceId, int lineOffset, int columnOffset,
/// boolean returnSourceInfo)`.
#[unsafe(no_mangle)]
#[allow(clippy::too_many_arguments)] // Mirrors the Java signature.
pub extern "system" fn Java_org_finos_legend_pure_rust_parser_NativeGrammarParser_parseExpression<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    kind: JString<'local>,
    code: JString<'local>,
    source_id: JString<'local>,
    line_offset: jint,
    column_offset: jint,
    return_source_info: jboolean,
) -> jstring {
    respond(&mut env, |env| {
        let kind_name = java_string(env, &kind, "kind")?;
        let code = java_string(env, &code, "code")?;
        let source_id = java_string(env, &source_id, "sourceId")?;
        let Some(kind) = ExpressionKind::from_name(&kind_name) else {
            return Ok(legend_pure_parser_api::error_json(&SectionError {
                kind: ErrorKind::Unsupported,
                message: format!("no expression kind '{kind_name}'"),
                source_information: None,
            }));
        };
        Ok(legend_pure_parser_api::parse_expression_json(
            &ExpressionRequest {
                kind,
                code: &code,
                source_id: &source_id,
                line_offset,
                column_offset,
                return_source_info: return_source_info != 0,
            },
        ))
    })
}

/// Runs `compute`, turning an argument error or a panic into the internal
/// error JSON, and hands the result to Java.
fn respond<'local>(
    env: &mut JNIEnv<'local>,
    compute: impl FnOnce(&mut JNIEnv<'local>) -> Result<String, String>,
) -> jstring {
    let json = match catch_unwind(AssertUnwindSafe(|| compute(env))) {
        Ok(Ok(json)) => json,
        Ok(Err(message)) => internal_error(message),
        Err(panic) => internal_error(format!(
            "panic in the Rust parser: {}",
            panic_message(&*panic)
        )),
    };
    env.new_string(json)
        .map_or(std::ptr::null_mut(), JString::into_raw)
}

fn java_string(env: &mut JNIEnv<'_>, value: &JString<'_>, name: &str) -> Result<String, String> {
    if value.is_null() {
        return Err(format!("{name} must not be null"));
    }
    env.get_string(value)
        .map(Into::into)
        .map_err(|e| format!("cannot read {name}: {e}"))
}

fn internal_error(message: String) -> String {
    legend_pure_parser_api::error_json(&SectionError {
        kind: ErrorKind::Internal,
        message,
        source_information: None,
    })
}

fn panic_message(panic: &(dyn std::any::Any + Send)) -> String {
    panic
        .downcast_ref::<&str>()
        .map(|s| (*s).to_string())
        .or_else(|| panic.downcast_ref::<String>().cloned())
        .unwrap_or_else(|| "unknown panic payload".to_string())
}
