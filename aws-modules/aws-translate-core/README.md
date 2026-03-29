# AWS Translate Core

AWS Translate and Comprehend integration for text translation and language detection.

## What It Solves

- `AwsTranslate` implements JSK's `ITranslate` interface for text translation (AWS Translate) and language recognition (AWS Comprehend)
- Detects dominant language(s) in text, returns ranked list of `LangType` values
- Translates text between `LangType` pairs

## Key Details

- Implements a JSK-defined `ITranslate` interface — can be swapped with other backends
- Falls back to English if detection fails
- Translate and Comprehend can use separate AWS regions
