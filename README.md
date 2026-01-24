# Jadify

A configurable CLI/CI tool to enforce Javadoc coverage for public Java APIs.

## Architecture (layers)
- config: YAML/schema → Config (pure data)
- compile: Config → CompiledConfig (precompiled selectors/regex)
- scan: source → ScanContext (elements + doc comments)
- rules: ScanContext + CompiledConfig → Issues
- engine: pipeline orchestration
- cli: user interface & output formatting


### TODOS
- 1 **Configuration/Parsing**: 
  - [x] Implement JsonSchema generation
  - [x] Add schema validation
  - [x] Add default config (checking default javadoc annotations)
  - [ ] Implement default javadoc rules
  - [ ] Add better handling for rule configuration and rule parsing errors
- 2 **Test/Demo**:
  - [x] Implement starter
  - [ ] Add Tests
- 3 **Documentation**:
  - [ ] Generate configuration documentation
- 4 **CLI**:
  - [ ] Implement and document CLI usage
- 5 **Output**:
  - [ ] Implement different report output formats
 
---
### Ideas
- Check for e.g. xml in Javadoc
