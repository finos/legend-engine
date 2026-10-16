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

//! Derive macros for the Legend Pure parser AST.
//!
//! Provides three derive macros that form a trait hierarchy:
//!
//! - **`#[derive(Spanned)]`** — implements `Spanned` (requires `source_info: SourceInfo` field)
//! - **`#[derive(Annotated)]`** — implements `Spanned` + `Annotated` (requires `stereotypes`, `tagged_values`, `source_info`)
//! - **`#[derive(PackageableElement)]`** — implements `Spanned` + `Annotated` + `PackageableElement` (requires `package`, `name`, `source_info`)
//!
//! The hierarchy mirrors the trait supertraits: `PackageableElement: Spanned + Annotated`.
//! Each higher-level derive automatically generates the lower-level impls, so you only
//! need one derive annotation per struct.
//!
//! # Usage
//!
//! ```ignore
//! // Only need one derive — PackageableElement brings Spanned + Annotated
//! #[derive(Debug, Clone, PartialEq, crate::PackageableElement)]
//! pub struct ClassDef {
//!     pub package: Option<Package>,
//!     pub name: Identifier,
//!     pub stereotypes: Vec<StereotypePtr>,
//!     pub tagged_values: Vec<TaggedValue>,
//!     pub source_info: SourceInfo,
//!     // ...
//! }
//! ```

use proc_macro::TokenStream;
use quote::quote;
use syn::{Data, DeriveInput, Fields, parse_macro_input};

/// Derives the `Spanned` trait for a struct with a `source_info: SourceInfo` field.
///
/// The struct **must** have a field named `source_info`. If the field is missing,
/// a compile-time error is produced.
///
/// # Panics
///
/// Panics at compile time if:
/// - The type is not a struct (enums and unions are not supported)
/// - The struct does not have a field named `source_info`
#[proc_macro_derive(Spanned)]
pub fn derive_spanned(input: TokenStream) -> TokenStream {
    let input = parse_macro_input!(input as DeriveInput);
    let name = &input.ident;
    let generics = &input.generics;
    let (impl_generics, ty_generics, where_clause) = generics.split_for_impl();

    // Verify the struct has a `source_info` field
    match &input.data {
        Data::Struct(data) => {
            if let Fields::Named(fields) = &data.fields {
                let has_source_info = fields
                    .named
                    .iter()
                    .any(|f| f.ident.as_ref().is_some_and(|id| id == "source_info"));

                if !has_source_info {
                    return syn::Error::new_spanned(
                        &input.ident,
                        "Spanned derive requires a `source_info: SourceInfo` field",
                    )
                    .to_compile_error()
                    .into();
                }
            } else {
                return syn::Error::new_spanned(
                    &input.ident,
                    "Spanned derive only supports structs with named fields",
                )
                .to_compile_error()
                .into();
            }
        }
        _ => {
            return syn::Error::new_spanned(
                &input.ident,
                "Spanned derive only supports structs, not enums or unions",
            )
            .to_compile_error()
            .into();
        }
    }

    let expanded = quote! {
        impl #impl_generics crate::source_info::Spanned for #name #ty_generics #where_clause {
            fn source_info(&self) -> &crate::source_info::SourceInfo {
                &self.source_info
            }
        }
    };

    expanded.into()
}

fn has_annotated_fields(data: &syn::DataStruct) -> bool {
    let mut has_stereo = false;
    let mut has_tags = false;
    if let syn::Fields::Named(fields) = &data.fields {
        for f in &fields.named {
            let ident = f.ident.as_ref().map(std::string::ToString::to_string);
            if ident.as_deref() == Some("stereotypes") {
                let ty_str = quote::quote!(#f).to_string();
                if ty_str.contains("StereotypePtr") {
                    has_stereo = true;
                }
            }
            if ident.as_deref() == Some("tagged_values") {
                has_tags = true;
            }
        }
    }
    has_stereo && has_tags
}

/// Derives the `PackageableElement` trait (and `Annotated` trait, since it's a supertrait)
#[proc_macro_derive(PackageableElement)]
pub fn derive_packageable_element(input: TokenStream) -> TokenStream {
    let input = parse_macro_input!(input as DeriveInput);
    let name = &input.ident;
    let generics = &input.generics;
    let (impl_generics, ty_generics, where_clause) = generics.split_for_impl();

    let Data::Struct(data) = &input.data else {
        return syn::Error::new_spanned(&input.ident, "PackageableElement only supports structs")
            .to_compile_error()
            .into();
    };

    let has_annotated = has_annotated_fields(data);
    let stereotypes_body = if has_annotated {
        quote! { &self.stereotypes }
    } else {
        quote! { &[] }
    };
    let tagged_values_body = if has_annotated {
        quote! { &self.tagged_values }
    } else {
        quote! { &[] }
    };

    let expanded = quote! {
        impl #impl_generics crate::source_info::Spanned for #name #ty_generics #where_clause {
            fn source_info(&self) -> &crate::source_info::SourceInfo {
                &self.source_info
            }
        }

        impl #impl_generics crate::element::PackageableElement for #name #ty_generics #where_clause {
            fn package(&self) -> Option<&crate::type_ref::Package> {
                self.package.as_ref()
            }
            fn name(&self) -> &crate::type_ref::Identifier {
                &self.name.value
            }
        }

        impl #impl_generics crate::element::Annotated for #name #ty_generics #where_clause {
            fn stereotypes(&self) -> &[crate::annotation::StereotypePtr] {
                #stereotypes_body
            }
            fn tagged_values(&self) -> &[crate::annotation::TaggedValue] {
                #tagged_values_body
            }
        }
    };

    expanded.into()
}

/// Derives the `Annotated` trait and the `Spanned` trait.
///
/// The struct must have fields `stereotypes`, `tagged_values`, and `source_info`.
#[proc_macro_derive(Annotated)]
pub fn derive_annotated(input: TokenStream) -> TokenStream {
    let input = parse_macro_input!(input as DeriveInput);
    let name = &input.ident;
    let generics = &input.generics;
    let (impl_generics, ty_generics, where_clause) = generics.split_for_impl();

    let expanded = quote! {
        impl #impl_generics crate::source_info::Spanned for #name #ty_generics #where_clause {
            fn source_info(&self) -> &crate::source_info::SourceInfo {
                &self.source_info
            }
        }

        impl #impl_generics crate::element::Annotated for #name #ty_generics #where_clause {
            fn stereotypes(&self) -> &[crate::annotation::StereotypePtr] {
                &self.stereotypes
            }
            fn tagged_values(&self) -> &[crate::annotation::TaggedValue] {
                &self.tagged_values
            }
        }
    };

    expanded.into()
}
