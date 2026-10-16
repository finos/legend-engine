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

//! What `java.net.URI`'s constructor makes of a string, which some
//! connections' walkers check their URLs with: Rust says where the answer is
//! plain, and leaves the rest to the engine.

/// What `java.net.URI`'s constructor makes of a string.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum JavaUri {
    /// It takes it.
    Valid,
    /// It rejects it.
    Invalid,
    /// Rust doesn't say.
    Unknown,
}

/// `URI` rejects a control, a space and ``"<>\^`{|}``, and `%` not starting
/// an escape; it takes a plain `scheme://authority/path?query` of the
/// characters it allows there.
#[must_use]
pub fn java_uri(text: &str) -> JavaUri {
    let bytes = text.as_bytes();
    for (index, c) in text.char_indices() {
        let illegal = c.is_control()
            || matches!(
                c,
                ' ' | '"' | '<' | '>' | '\\' | '^' | '`' | '{' | '|' | '}'
            );
        let bad_escape = c == '%'
            && !(bytes.get(index + 1).is_some_and(u8::is_ascii_hexdigit)
                && bytes.get(index + 2).is_some_and(u8::is_ascii_hexdigit));
        if illegal || bad_escape {
            return JavaUri::Invalid;
        }
    }
    let Some((scheme, rest)) = text.split_once("://") else {
        return JavaUri::Unknown;
    };
    let scheme_ok = scheme.starts_with(|c: char| c.is_ascii_alphabetic())
        && scheme
            .chars()
            .all(|c| c.is_ascii_alphanumeric() || matches!(c, '+' | '.' | '-'));
    let (authority, path) = rest
        .find(['/', '?'])
        .map_or((rest, ""), |at| rest.split_at(at));
    let allowed = |c: char| c.is_ascii_alphanumeric() || "-._~!$&'()*+,;=:@%".contains(c);
    let authority_ok = !authority.is_empty() && authority.chars().all(allowed);
    let path_ok = path.chars().all(|c| allowed(c) || matches!(c, '/' | '?'));
    if scheme_ok && authority_ok && path_ok {
        JavaUri::Valid
    } else {
        JavaUri::Unknown
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn plain_urls_are_uris_and_illegal_characters_are_not() {
        assert_eq!(java_uri("http://localhost:10000"), JavaUri::Valid);
        assert_eq!(java_uri("https://host.com:1234/api?a=b"), JavaUri::Valid);
        assert_eq!(java_uri("wrong url"), JavaUri::Invalid);
        assert_eq!(java_uri("http://a%zz"), JavaUri::Invalid);
        // Valid as an opaque URI, which Rust doesn't judge.
        assert_eq!(java_uri("mailto:someone"), JavaUri::Unknown);
    }
}
