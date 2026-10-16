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

//! Source provider abstraction for the parser.
//!
//! The [`SourceProvider`] trait abstracts over where source text comes from,
//! enabling the parser to operate uniformly on in-memory strings, filesystem
//! files, ZIP entries, or any other source.
//!
//! # Built-in Providers
//!
//! [`SourceInput`] is the built-in enum implementing [`SourceProvider`]:
//!
//! - [`SourceInput::InMemory`] — source text already loaded in memory
//! - [`SourceInput::FileSystem`] — source text loaded lazily from disk
//!
//! # Extending with Custom Providers
//!
//! Implement [`SourceProvider`] for your own type to support additional
//! source backends (e.g., ZIP archives, remote URLs, database BLOBs):
//!
//! ```rust,ignore
//! struct ZipEntry { archive: PathBuf, entry: String }
//!
//! impl SourceProvider for ZipEntry {
//!     fn name(&self) -> &str { &self.entry }
//!     fn source_text(&self) -> std::io::Result<std::borrow::Cow<'_, str>> {
//!         let bytes = read_zip_entry(&self.archive, &self.entry)?;
//!         Ok(Cow::Owned(String::from_utf8(bytes).map_err(|e|
//!             std::io::Error::new(std::io::ErrorKind::InvalidData, e)
//!         )?))
//!     }
//! }
//! ```

use std::borrow::Cow;
use std::path::{Path, PathBuf};

/// Trait for providing source text to the parser.
///
/// Implementations must be [`Send`] + [`Sync`] because [`parse_many`](crate::parse_many)
/// loads and parses sources in parallel across threads.
///
/// # Contract
///
/// - [`name()`](SourceProvider::name) must return a stable identifier for the source,
///   used in error messages, source maps, and IDE links.
/// - [`source_text()`](SourceProvider::source_text) loads the source content.
///   It may be called multiple times (e.g., for error reporting after parsing).
///   Implementations should be idempotent.
pub trait SourceProvider: Send + Sync {
    /// The source name, used in error messages, source maps, and IDE links.
    ///
    /// For filesystem sources, this is the **full path** (e.g., `/path/to/model.pure`)
    /// to enable clickable `path:line:col` links in terminal output.
    /// For in-memory sources, this is whatever was provided at construction time.
    fn name(&self) -> &str;

    /// Load and return the source text.
    ///
    /// Returns [`Cow::Borrowed`] when the source is already in memory, avoiding
    /// allocation. Returns [`Cow::Owned`] when the source must be read from an
    /// external location (filesystem, archive, etc.).
    ///
    /// # Errors
    ///
    /// Returns [`std::io::Error`] if the source cannot be loaded (file not found,
    /// permission denied, archive corruption, etc.).
    fn source_text(&self) -> std::io::Result<Cow<'_, str>>;
}

/// Built-in source input types.
///
/// This enum provides the two most common source backends:
///
/// - [`InMemory`](SourceInput::InMemory) — source text already loaded in memory.
///   Zero-cost: `source_text()` returns a borrowed reference.
/// - [`FileSystem`](SourceInput::FileSystem) — source text loaded from disk on demand.
///   `source_text()` calls [`std::fs::read_to_string`] on each invocation.
///
/// For other backends (ZIP, remote, etc.), implement [`SourceProvider`] directly.
///
/// # Example
///
/// ```
/// use legend_pure_parser_parser::source::SourceInput;
///
/// // In-memory
/// let mem = SourceInput::in_memory("test.pure", "Class A {}");
///
/// // From filesystem
/// # // (not actually reading a file in doctest)
/// # let _fs = SourceInput::file_system("/path/to/model.pure");
/// ```
#[derive(Debug, Clone)]
pub enum SourceInput {
    /// Source text already loaded in memory.
    InMemory {
        /// Logical file name.
        name: String,
        /// Full source text.
        content: String,
    },
    /// Source text to be loaded from the filesystem.
    FileSystem {
        /// Path to the `.pure` file on disk.
        path: PathBuf,
    },
}

impl SourceInput {
    /// Create an in-memory source from a name and content.
    #[must_use]
    pub fn in_memory(name: impl Into<String>, content: impl Into<String>) -> Self {
        Self::InMemory {
            name: name.into(),
            content: content.into(),
        }
    }

    /// Create a filesystem source from a path.
    ///
    /// The [`name()`](SourceProvider::name) is the **full path string**, enabling
    /// clickable `path:line:col` links in terminal error output.
    #[must_use]
    pub fn file_system(path: impl Into<PathBuf>) -> Self {
        let path = path.into();
        Self::FileSystem { path }
    }

    /// Returns the filesystem path if this is a `FileSystem` variant.
    #[must_use]
    pub fn path(&self) -> Option<&Path> {
        match self {
            Self::FileSystem { path, .. } => Some(path),
            Self::InMemory { .. } => None,
        }
    }
}

impl SourceProvider for SourceInput {
    fn name(&self) -> &str {
        match self {
            Self::InMemory { name, .. } => name,
            Self::FileSystem { path, .. } => path.to_str().unwrap_or("unknown.pure"),
        }
    }

    fn source_text(&self) -> std::io::Result<Cow<'_, str>> {
        match self {
            Self::InMemory { content, .. } => Ok(Cow::Borrowed(content)),
            Self::FileSystem { path, .. } => {
                let text = std::fs::read_to_string(path)?;
                Ok(Cow::Owned(text))
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn in_memory_returns_borrowed() {
        let src = SourceInput::in_memory("test.pure", "Class A {}");
        assert_eq!(src.name(), "test.pure");
        let text = src.source_text().unwrap();
        assert!(matches!(text, Cow::Borrowed(_)));
        assert_eq!(&*text, "Class A {}");
    }

    #[test]
    fn file_system_name_is_full_path() {
        let src = SourceInput::file_system("/some/path/to/model.pure");
        assert_eq!(src.name(), "/some/path/to/model.pure");
    }

    #[test]
    fn file_system_path_accessor() {
        let src = SourceInput::file_system("/some/path/to/model.pure");
        assert_eq!(src.path().unwrap(), Path::new("/some/path/to/model.pure"));
    }

    #[test]
    fn in_memory_path_is_none() {
        let src = SourceInput::in_memory("test.pure", "Class A {}");
        assert!(src.path().is_none());
    }

    #[test]
    fn file_system_io_error_on_missing() {
        let src = SourceInput::file_system("/nonexistent/path/missing.pure");
        let result = src.source_text();
        assert!(result.is_err());
    }
}
