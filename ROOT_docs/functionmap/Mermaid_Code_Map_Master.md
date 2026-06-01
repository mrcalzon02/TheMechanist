# Mermaid Code Map Master Record

Status: active master code-position map scaffold.

Generate/evaluate with:

```powershell
py -3 scripts\BUILD_MERMAID_CODE_MAP.py --apply
```

## Top-Line Rule

Every code module, generated code error, compile error cluster, or subsystem remap must submit a Mermaid position before it is considered mapped. Unpositioned modules are architecture debt, not invisible implementation detail.

## Current Purpose

This record supersedes shard-only Mermaid snippets for current code ownership work. Shard-era Mermaid records remain historical context, but this master record is the codewide map location for modules, error clusters, and function remapping decisions.

## Generated Ledgers

The builder writes:

- `ROOT_docs/functionmap/generated/MERMAID_CODE_MAP.md`
- `ROOT_docs/functionmap/generated/CODE_MERMAID_POSITION_LEDGER.tsv`
- `ROOT_docs/functionmap/generated/CODE_MERMAID_EVALUATION.tsv`

## Review Rule

A module listed as unpositioned in `CODE_MERMAID_EVALUATION.tsv` must be assigned to an existing zone or given a new explicit zone before related code movement is considered complete.
