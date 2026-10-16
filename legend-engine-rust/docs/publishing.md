# Publishing the crates

The workspace's crates are published to crates.io, apart from the engine's Maven release. The JNI crate
(`crates/jni`) is not: it is the native library of the engine's
`legend-engine-language-pure-grammar-rust`, whose build makes it with cargo and puts it in the jar, under
`META-INF/native/<platform>/`.

## Versions

The crates share one version, their own, independent of the engine's.
`[workspace.package]` in `Cargo.toml` sets it. The workspace's own entries in `[workspace.dependencies]` repeat it:
that is the requirement crates.io records for the crates that depend on them.

## Releasing

1. In a pull request, set the new version in `[workspace.package]` and in every `legend-pure-*` entry of
   `[workspace.dependencies]`.
2. Run the **Publish Rust crates** workflow (`.github/workflows/rust-publish.yml`) from the merged commit, with
   *dry run* on: it packages every crate and builds it from its package, as crates.io will.
3. Run it again with *dry run* off. `cargo publish --workspace` uploads the crates in dependency order.

A version on crates.io can't be replaced: a crate published by mistake is yanked, and the fix goes out under a new
version.

The workflow needs the repository secret `CARGO_REGISTRY_TOKEN`: a crates.io API token with the
`publish-new` and `publish-update` scopes for the `legend-pure-*` crates.

## Checking a change

`cargo publish --workspace --dry-run --allow-dirty` runs the same checks locally. Rust CI runs them on every change
to the workspace, so a crate that stops packaging (a file it reads outside its directory, a dependency without a
version) fails there, not at release time.
