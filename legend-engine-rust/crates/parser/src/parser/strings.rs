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

//! String literal values, as the engine derives them from `STRING` tokens
//! (`PureGrammarParserUtility.fromGrammarString`, `processTextBlock`,
//! `canonicalizeDocumentation`), with Java's `StringEscapeUtils.unescapeJava`
//! for escapes. legend-pure's Java parser implements the same rules.

/// Whether a raw `STRING` token is a text block: `'''`, optional spaces or
/// tabs, a line terminator, …, `'''`.
#[must_use]
pub fn is_text_block(raw: &str) -> bool {
    if raw.chars().count() < 7 || !raw.starts_with("'''") || !raw.ends_with("'''") {
        return false;
    }
    let rest = raw[3..].trim_start_matches([' ', '\t']);
    rest.starts_with('\n') || rest.starts_with("\r\n")
}

/// The value of a raw `STRING` token and whether it was written as a text
/// block (the protocol's `multiLine`).
///
/// # Errors
///
/// A malformed `\u` escape, as `unescapeJava` rejects it.
pub fn string_value(raw: &str) -> Result<(String, bool), String> {
    if is_text_block(raw) {
        return Ok((unescape_java(&text_block_layout(raw))?, true));
    }
    // The engine unescapes the token, then drops its first and last character.
    let unescaped = unescape_java(raw)?;
    let mut chars = unescaped.chars();
    chars.next();
    chars.next_back();
    Ok((chars.as_str().to_string(), false))
}

/// The value of a raw `STRING` token without escape processing, as the
/// engine reads some (`fromGrammarString(token, false)`): a text block's
/// layout, or the text between the quotes as written.
#[must_use]
pub fn raw_string_value(raw: &str) -> String {
    if is_text_block(raw) {
        return text_block_layout(raw);
    }
    let mut chars = raw.chars();
    chars.next();
    chars.next_back();
    chars.as_str().to_string()
}

/// A documentation literal's value: the text block layout without escape
/// processing, minus leading and trailing blank lines.
#[must_use]
pub fn documentation_value(raw: &str) -> String {
    let layout = text_block_layout(raw);
    let lines: Vec<&str> = layout.split('\n').collect();
    let start = lines
        .iter()
        .position(|l| !l.is_empty())
        .unwrap_or(lines.len());
    let end = lines
        .iter()
        .rposition(|l| !l.is_empty())
        .map_or(start, |i| i + 1);
    lines[start..end].join("\n")
}

/// The Java text-block layout: normalize line terminators, drop the opening
/// delimiter line and the closing `'''`, strip the common indentation (every
/// non-blank line plus the closing-delimiter line), strip trailing
/// whitespace from each line.
fn text_block_layout(raw: &str) -> String {
    let normalized = raw.replace("\r\n", "\n").replace('\r', "\n");
    let after_opening = normalized.find('\n').map_or(normalized.len(), |i| i + 1);
    let body = &normalized[after_opening..normalized.len() - 3];
    let lines: Vec<&str> = body.split('\n').collect();
    let last = lines.len() - 1;
    let min_indent = lines
        .iter()
        .enumerate()
        .filter_map(|(i, line)| {
            let leading = leading_whitespace(line);
            (leading < line.chars().count() || i == last).then_some(leading)
        })
        .min()
        .unwrap_or(0);
    lines
        .iter()
        .map(|line| line.chars().skip(min_indent).collect::<String>())
        .map(|line| line.trim_end_matches(is_java_whitespace).to_string())
        .collect::<Vec<_>>()
        .join("\n")
}

fn leading_whitespace(line: &str) -> usize {
    line.chars().take_while(|&c| is_java_whitespace(c)).count()
}

/// Java's `Character.isWhitespace`: Unicode space separators except the
/// non-breaking ones, plus `\t \n \u{B} \f \r` and `\u{1C}`–`\u{1F}`.
fn is_java_whitespace(c: char) -> bool {
    matches!(
        c,
        '\t' | '\n' | '\u{0B}' | '\u{0C}' | '\r' | '\u{1C}'..='\u{1F}'
    ) || (c.is_whitespace() && !matches!(c, '\u{85}' | '\u{A0}' | '\u{2007}' | '\u{202F}'))
}

/// Apache Commons Text `StringEscapeUtils.unescapeJava`: octal (`\0`–`\377`),
/// unicode (`\uXXXX`, repeated `u`s and an optional `+` allowed), `\b \n \t
/// \f \r`, `\\ \" \'`; any other backslash is dropped (`\d` → `d`).
///
/// # Errors
///
/// A `\u` escape without four hex digits.
pub fn unescape_java(s: &str) -> Result<String, String> {
    let chars: Vec<char> = s.chars().collect();
    // Built as UTF-16, like the Java string, so `\uD83D\uDE00` escapes of a
    // surrogate pair combine into one character.
    let mut out: Vec<u16> = Vec::with_capacity(s.len());
    let push = |out: &mut Vec<u16>, c: char| {
        let mut buf = [0u16; 2];
        out.extend_from_slice(c.encode_utf16(&mut buf));
    };
    let mut i = 0;
    while i < chars.len() {
        let c = chars[i];
        if c != '\\' {
            push(&mut out, c);
            i += 1;
            continue;
        }
        let Some(&next) = chars.get(i + 1) else {
            // A lone trailing backslash is dropped.
            i += 1;
            continue;
        };
        match next {
            '0'..='7' => {
                let start = i + 1;
                let mut end = start + 1;
                if chars.get(end).is_some_and(|c| c.is_digit(8)) {
                    end += 1;
                    if matches!(chars[start], '0'..='3')
                        && chars.get(end).is_some_and(|c| c.is_digit(8))
                    {
                        end += 1;
                    }
                }
                let digits: String = chars[start..end].iter().collect();
                let code = u16::from_str_radix(&digits, 8).map_err(|e| e.to_string())?;
                out.push(code);
                i = end;
            }
            'u' => {
                let mut j = i + 1;
                while chars.get(j) == Some(&'u') {
                    j += 1;
                }
                if chars.get(j) == Some(&'+') {
                    j += 1;
                }
                let hex: String = chars.iter().skip(j).take(4).collect();
                if hex.chars().count() < 4 {
                    return Err(format!(
                        "Less than 4 hex digits in unicode value: '{}' due to end of CharSequence",
                        chars[i..].iter().collect::<String>()
                    ));
                }
                let code = u16::from_str_radix(&hex, 16)
                    .map_err(|_| format!("Unable to parse unicode value: {hex}"))?;
                out.push(code);
                i = j + 4;
            }
            'b' => {
                push(&mut out, '\u{08}');
                i += 2;
            }
            'n' => {
                push(&mut out, '\n');
                i += 2;
            }
            't' => {
                push(&mut out, '\t');
                i += 2;
            }
            'f' => {
                push(&mut out, '\u{0C}');
                i += 2;
            }
            'r' => {
                push(&mut out, '\r');
                i += 2;
            }
            '\\' | '"' | '\'' => {
                push(&mut out, next);
                i += 2;
            }
            // Any other escape: the backslash is dropped, the character kept.
            _ => i += 1,
        }
    }
    Ok(String::from_utf16_lossy(&out))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn ordinary_strings_unescape_like_java() {
        assert_eq!(
            string_value(r"'it\'s'").unwrap(),
            ("it's".to_string(), false)
        );
        assert_eq!(string_value(r"'a\nb\tc'").unwrap().0, "a\nb\tc");
        // Unknown escapes lose their backslash (Commons Text).
        assert_eq!(string_value(r"'\d+'").unwrap().0, "d+");
        assert_eq!(string_value(r"'\u0041\101'").unwrap().0, "AA");
        assert_eq!(string_value(r"'a\\b'").unwrap().0, r"a\b");
        assert!(string_value(r"'\u12'").is_err());
        assert_eq!(string_value(r"'\uD83D\uDE00'").unwrap().0, "\u{1F600}");
    }

    #[test]
    fn text_blocks_follow_the_java_layout() {
        let raw = "'''\n    hello\n      world  \n    '''";
        assert!(is_text_block(raw));
        assert_eq!(
            string_value(raw).unwrap(),
            ("hello\n  world\n".to_string(), true)
        );
        // The closing delimiter's indentation sets a floor.
        let raw = "'''\n    a\n  '''";
        assert_eq!(string_value(raw).unwrap().0, "  a\n");
        // Escapes apply after the layout.
        assert_eq!(string_value("'''\n  a\\rb\n  '''").unwrap().0, "a\rb\n");
        assert!(!is_text_block("'''abc'''"));
    }

    #[test]
    fn documentation_keeps_escapes_and_drops_blank_edges() {
        let raw = "'''\n\n  A person.\n  Matches \\d+.\n\n  '''";
        assert_eq!(documentation_value(raw), "A person.\nMatches \\d+.");
    }
}
