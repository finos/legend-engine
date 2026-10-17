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

//! Whole models, as the engine's `PureGrammarParser.parseModel` parses them:
//! the code behind the engine's default `###Pure` header, split into
//! sections the way its `CodeParserGrammar` splits it, each section parsed
//! by [`parse_section`](crate::parse_section) with the position context the engine gives its
//! section parsers, then the section index.

use std::collections::HashSet;
use std::hash::Hash;

use legend_pure_parser_protocol::ConvertError;
use legend_pure_parser_protocol::v1::source_info::SourceInformation;
use serde::Serialize;
use serde_json::Value;
use serde_json::value::RawValue;

use crate::{Elements, ErrorKind, SectionError, SectionRequest, WrittenElements, section_elements};

/// What the engine puts in front of a model's code: code before any header
/// is a `###Pure` section.
const DEFAULT_SECTION_BEGIN: &str = "\n###Pure\n";

/// A model's code to parse, with the engine's position context.
#[derive(Debug, Clone, Copy)]
pub struct ModelRequest<'a> {
    /// The model's code: sections, each behind its `###` header, the first
    /// one's header optional (`###Pure`).
    pub code: &'a str,
    /// The engine's `sourceId`, reported in every source information.
    pub source_id: &'a str,
    /// Added to every line number.
    pub line_offset: i32,
    /// Added to column numbers on the code's first line.
    pub column_offset: i32,
}

/// The parsed model: the protocol elements of its sections, in order, then
/// the section index.
#[derive(Debug, Clone, PartialEq, Serialize)]
pub struct ModelResult {
    /// Protocol v1 elements, the section index last.
    pub elements: Vec<Value>,
}

/// Parses a model's code into protocol elements, as the engine's
/// `PureGrammarParser.parseModel` does.
///
/// The code is split into sections as the engine's `CodeLexerGrammar`
/// splits it, by text alone: a section starts at `###Name` written at the
/// start of a line, wherever that is (inside a string too), and any other
/// `###` is its section's text. Each section is parsed by
/// [`parse_section`](crate::parse_section), with the line offset the engine gives it (the
/// header's line, counted with the engine's default header in front of
/// the code, less two) and no column offset: code after a header, on its
/// line, is positioned from the end of the header. The section index
/// (`__internal__::SectionIndex`) lists each section's type, its elements'
/// paths and its imports, each once, and its span in the code behind the
/// default header, measured as the engine's parse tree walker measures it.
///
/// # Errors
///
/// The first section's error, in order, as [`parse_section`](crate::parse_section) reports it: a
/// section type without a linked grammar is [`ErrorKind::Unsupported`].
/// Offsets below zero are unsupported too.
pub fn parse_model(request: &ModelRequest<'_>) -> Result<ModelResult, SectionError> {
    Ok(ModelResult {
        elements: model_json(request)?
            .iter()
            .map(|element| crate::value(element))
            .collect::<Result<_, _>>()?,
    })
}

/// [`parse_model`]'s elements as JSON text, each written once, the section
/// index last.
pub(crate) fn model_json(request: &ModelRequest<'_>) -> Result<Vec<Box<RawValue>>, SectionError> {
    let mut elements = Vec::new();
    let index = model(request, &mut elements)?;
    let mut json: Vec<Box<RawValue>> = elements.into_iter().map(|element| element.json).collect();
    json.push(
        serde_json::value::to_raw_value(&index)
            .map_err(|e| SectionError::from(ConvertError::from(e)))?,
    );
    Ok(json)
}

/// [`parse_model`]'s JSON object, `{"elements":[…]}`, written at the end of
/// `out`: every element in place, then the section index.
pub(crate) fn model_json_into(
    request: &ModelRequest<'_>,
    out: &mut Vec<u8>,
) -> Result<(), SectionError> {
    out.extend_from_slice(br#"{"elements":["#);
    let mut elements = WrittenElements { out, count: 0 };
    let index = model(request, &mut elements)?;
    if elements.count > 0 {
        out.push(b',');
    }
    serde_json::to_writer(&mut *out, &index)
        .map_err(|e| SectionError::from(ConvertError::from(e)))?;
    out.extend_from_slice(b"]}");
    Ok(())
}

/// Parses the model's sections, converting their elements into `elements`;
/// returns the section index.
fn model(
    request: &ModelRequest<'_>,
    elements: &mut impl Elements,
) -> Result<SectionIndexJson, SectionError> {
    if request.line_offset < 0 || request.column_offset < 0 {
        return Err(SectionError::new(
            ErrorKind::Unsupported,
            format!(
                "source offsets (line {}, column {}) below zero",
                request.line_offset, request.column_offset
            ),
            None,
        ));
    }
    let text = format!("{DEFAULT_SECTION_BEGIN}{}", request.code);
    let mut sections = Vec::new();
    for section in split(&text) {
        let line_offset = i32::try_from(section.header.line)
            .ok()
            .and_then(|line| line.checked_add(request.line_offset - 2))
            .ok_or_else(|| {
                SectionError::new(ErrorKind::Unsupported, "the model is too long", None)
            })?;
        let (paths, imports) = section_elements(
            &SectionRequest {
                section_type: section.name,
                code: section.content,
                source_id: request.source_id,
                line_offset,
                column_offset: 0,
            },
            elements,
        )?;
        sections.push(SectionIndexEntryJson {
            section_type: if imports.is_some() {
                "importAware"
            } else {
                "default"
            },
            parser_name: section.name.to_string(),
            elements: distinct(paths),
            source_information: span(&section, request),
            imports: imports.map(distinct),
        });
    }
    Ok(SectionIndexJson {
        package: "__internal__",
        name: "SectionIndex",
        sections,
    })
}

/// The engine's `SectionIndex`: each section's type, its elements' paths
/// and its imports, each once, and its span in the code.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "sectionIndex")]
struct SectionIndexJson {
    package: &'static str,
    name: &'static str,
    sections: Vec<SectionIndexEntryJson>,
}

/// A section of the index: an `importAware` one, with its imports, or a
/// `default` one.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct SectionIndexEntryJson {
    #[serde(rename = "_type")]
    section_type: &'static str,
    parser_name: String,
    elements: Vec<String>,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    imports: Option<Vec<String>>,
}

/// One section of the code, as the engine's `CodeLexerGrammar` reads it.
struct SectionText<'a> {
    /// `\n###Name`: the lexer's `SECTION_START`.
    header: Token<'a>,
    /// The section type, `Name`.
    name: &'a str,
    /// Everything up to the next header: the section's code.
    content: &'a str,
    /// The content's last character, its last token, if any.
    last: Option<Token<'a>>,
}

/// A token as ANTLR positions it: on its first character's line and column
/// (counted from 0, in code points).
#[derive(Clone, Copy)]
struct Token<'a> {
    text: &'a str,
    line: u32,
    column: u32,
}

/// The sections of `text`, which starts with the default header: a header
/// is a line break, `###` and a name (the lexer's `ValidString`); every
/// other character, one token each, is its section's content.
///
/// Only line breaks can start a header, so the text is read from one line
/// break to the next (`memchr`); lines are counted at each, and code points
/// only for the two positions each section needs: its header's column and
/// its last character's.
fn split(text: &str) -> Vec<SectionText<'_>> {
    let mut sections = Vec::new();
    let mut current: Option<(Token<'_>, usize)> = None;
    // The line being read, from 1; where it starts, and where the line
    // before it does.
    let mut lines = Lines {
        line: 1,
        start: 0,
        previous_start: 0,
    };
    for at in memchr::memchr_iter(b'\n', text.as_bytes()) {
        if let Some(length) = header_length(&text[at..]) {
            if let Some((header, start)) = current.take() {
                sections.push(section(
                    text,
                    header,
                    start,
                    at,
                    lines.last(text, start, at),
                ));
            }
            current = Some((
                Token {
                    text: &text[at..at + length],
                    line: lines.line,
                    column: columns(&text[lines.start..at]),
                },
                at + length,
            ));
        }
        lines = Lines {
            line: lines.line + 1,
            start: at + 1,
            previous_start: lines.start,
        };
    }
    if let Some((header, start)) = current {
        sections.push(section(
            text,
            header,
            start,
            text.len(),
            lines.last(text, start, text.len()),
        ));
    }
    sections
}

/// Where `split` is: the line it reads and where that line and the one
/// before it start.
#[derive(Clone, Copy)]
struct Lines {
    line: u32,
    start: usize,
    previous_start: usize,
}

impl Lines {
    /// The last character of `text[start..end]`, as a token, if there is one;
    /// `end` is on the line being read.
    fn last(self, text: &str, start: usize, end: usize) -> Option<Token<'_>> {
        let (offset, c) = text[start..end].char_indices().next_back()?;
        let at = start + offset;
        // A line break ends the line before the one being read; any other
        // character is on it.
        let (line, line_start) = if c == '\n' {
            (self.line - 1, self.previous_start)
        } else {
            (self.line, self.start)
        };
        Some(Token {
            text: &text[at..at + c.len_utf8()],
            line,
            column: columns(&text[line_start..at]),
        })
    }
}

/// The column after `line`, the start of a line: its code points.
fn columns(line: &str) -> u32 {
    u32::try_from(line.chars().count()).unwrap_or(u32::MAX)
}

fn section<'a>(
    text: &'a str,
    header: Token<'a>,
    start: usize,
    end: usize,
    last: Option<Token<'a>>,
) -> SectionText<'a> {
    SectionText {
        name: &header.text[4..],
        header,
        content: &text[start..end],
        last,
    }
}

/// The length of the header `text` starts with, if it does: `\n###`, then
/// a name whose first character is a letter, a digit or `_`, and whose
/// others may also be `$`, all ASCII.
fn header_length(text: &str) -> Option<usize> {
    let name = text.strip_prefix("\n###")?.as_bytes();
    if !name
        .first()
        .is_some_and(|&c| c.is_ascii_alphanumeric() || c == b'_')
    {
        return None;
    }
    let length = name
        .iter()
        .take_while(|&&c| c.is_ascii_alphanumeric() || c == b'_' || c == b'$')
        .count();
    Some(4 + length)
}

/// The section's span in the code behind the default header, as the
/// engine's walker measures a parse tree: from its header (its line break,
/// which ends the line before) to its last token, which ends on the line
/// it starts on, its length in UTF-16 code units after its first column.
/// The model's column offset applies to the first line only, which holds
/// the default header's line break alone.
fn span(section: &SectionText<'_>, request: &ModelRequest<'_>) -> SourceInformation {
    let line =
        |token: &Token<'_>| token.line + u32::try_from(request.line_offset).unwrap_or_default();
    let column_offset = |token: &Token<'_>| {
        if token.line == 1 {
            u32::try_from(request.column_offset).unwrap_or_default()
        } else {
            0
        }
    };
    let stop = section.last.as_ref().unwrap_or(&section.header);
    let length = u32::try_from(stop.text.encode_utf16().count()).unwrap_or(u32::MAX);
    SourceInformation {
        source_id: request.source_id.to_string(),
        start_line: line(&section.header),
        start_column: section.header.column + 1 + column_offset(&section.header),
        end_line: line(stop),
        end_column: stop.column + length + column_offset(stop),
    }
}

/// `values` without repeats, each where it first is (the engine's
/// `ListIterate.distinct`).
fn distinct<T: Eq + Hash + Clone>(values: impl IntoIterator<Item = T>) -> Vec<T> {
    let mut seen = HashSet::new();
    values
        .into_iter()
        .filter(|value| seen.insert(value.clone()))
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A section as split: its name, its content, its header's line and
    /// column, its last token's.
    type Split = (String, String, (u32, u32), Option<(u32, u32)>);

    fn sections(code: &str) -> Vec<Split> {
        let text = format!("{DEFAULT_SECTION_BEGIN}{code}");
        split(&text)
            .into_iter()
            .map(|s| {
                (
                    s.name.to_string(),
                    s.content.to_string(),
                    (s.header.line, s.header.column),
                    s.last.map(|t| (t.line, t.column)),
                )
            })
            .collect()
    }

    /// The split as it was, one character at a time: what `split` must
    /// still find.
    fn split_by_character(text: &str) -> Vec<SectionText<'_>> {
        let mut sections = Vec::new();
        let mut current: Option<(Token<'_>, usize)> = None;
        let mut last = None;
        let (mut line, mut column) = (1_u32, 0_u32);
        let mut at = 0;
        while let Some(c) = text[at..].chars().next() {
            if let Some(length) = (c == '\n').then(|| header_length(&text[at..])).flatten() {
                if let Some((header, start)) = current.take() {
                    sections.push(section(text, header, start, at, last.take()));
                }
                current = Some((
                    Token {
                        text: &text[at..at + length],
                        line,
                        column,
                    },
                    at + length,
                ));
                last = None;
                line += 1;
                column = u32::try_from(length - 1).unwrap_or(u32::MAX);
                at += length;
                continue;
            }
            last = Some(Token {
                text: &text[at..at + c.len_utf8()],
                line,
                column,
            });
            if c == '\n' {
                line += 1;
                column = 0;
            } else {
                column += 1;
            }
            at += c.len_utf8();
        }
        if let Some((header, start)) = current {
            sections.push(section(text, header, start, text.len(), last));
        }
        sections
    }

    /// Everything a section holds, for comparing two splits.
    fn described(sections: &[SectionText<'_>]) -> Vec<String> {
        let token = |t: &Token<'_>| format!("{:?}@{}:{}", t.text, t.line, t.column);
        sections
            .iter()
            .map(|s| {
                format!(
                    "{} {} {:?} {:?}",
                    s.name,
                    token(&s.header),
                    s.content,
                    s.last.as_ref().map(token)
                )
            })
            .collect()
    }

    #[test]
    fn split_finds_what_reading_each_character_does() {
        let mut texts: Vec<String> = [
            "",
            "\n",
            "a",
            "Class a::A{}\n###Mapping\nMapping a::M()\n",
            "###Mapping",
            "\n###Pure\n###Mapping\n###Pure",
            "x\n\n###Mapping\n\n",
            "é\n###Mapping é\u{1F600}x\n###Pure\u{1F600}",
            "a\r\n###Mapping\r\nb\r\n",
            "a\n  ###Mapping\n",
            "a\n###\nb",
            "a\n###$x\n###Pure$x Class\n###1abc\n###Mappingé\n",
            "\u{1F600}\u{1F600}\n\u{1F600}###Pure\n###Pure\u{1F600}\n\u{1F600}",
        ]
        .iter()
        .map(|code| format!("{DEFAULT_SECTION_BEGIN}{code}"))
        .collect();
        // Every section of the engine corpus, as a model's code.
        let corpus = std::path::Path::new(env!("CARGO_MANIFEST_DIR"))
            .join("../../tests/engine-corpus/sections");
        if let Ok(entries) = std::fs::read_dir(&corpus) {
            for entry in entries.flatten() {
                if entry.path().extension().is_some_and(|e| e == "pure") {
                    let code = std::fs::read_to_string(entry.path()).expect("readable");
                    texts.push(format!("{DEFAULT_SECTION_BEGIN}{code}"));
                }
            }
        }
        assert!(texts.len() > 1000, "the engine corpus is missing");
        for text in &texts {
            assert_eq!(
                described(&split(text)),
                described(&split_by_character(text)),
                "{text:?}"
            );
        }
    }

    #[test]
    fn a_header_starts_a_line() {
        assert_eq!(
            sections("Class a::A{}\n###Mapping\nMapping a::M()\n"),
            vec![
                (
                    "Pure".into(),
                    "\nClass a::A{}".into(),
                    (1, 0),
                    Some((3, 11))
                ),
                (
                    "Mapping".into(),
                    "\nMapping a::M()\n".into(),
                    (3, 12),
                    Some((5, 14))
                ),
            ]
        );
        // Indented, in the middle of a line, or without a name: the
        // section's text.
        for code in ["a\n  ###Mapping\n", "a ###Mapping", "a\n###\nb", "a\n###$x"] {
            assert_eq!(sections(code).len(), 1, "{code:?}");
        }
    }

    #[test]
    fn a_name_is_ascii_and_may_hold_dollars() {
        let split = sections("\n###Pure$x Class\n###1abc\n###Mappingé\n");
        let names: Vec<&str> = split.iter().map(|s| s.0.as_str()).collect();
        assert_eq!(names, ["Pure", "Pure$x", "1abc", "Mapping"]);
        // The name ends where its characters do: the rest is content.
        assert_eq!(split[1].1, " Class");
        assert_eq!(split[3].1, "é\n");
    }

    #[test]
    fn the_default_section_may_be_empty() {
        let split = sections("###Mapping");
        assert_eq!(split[0].1, "");
        assert_eq!(split[0].3, None);
        assert_eq!(split[1].1, "");
        assert_eq!(split[1].2, (2, 7));
    }
}
