# legend-pure-parser-lexer

Hand-written tokenizer for the Pure grammar. Produces `Vec<Token>` with spans from UTF-8 source text.

## Key Types

- `Token` — Token with kind and byte-offset span
- `TokenKind` — Keywords, operators, literals, delimiters, island markers, section headers
- `Lexer` — Stateful cursor over source text
