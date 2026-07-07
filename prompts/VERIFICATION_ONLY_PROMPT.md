# Verification-only prompt for ZIB POC

This is a verification-only run.

Do not create a branch.  
Do not edit files.  
Do not commit.  
Do not open a PR.  
Do not “fix” anything during this run.

Read:

- `AGENTS.md`
- `TECHNICAL_SPEC.md`
- `ARCHITECTURE.md`
- `TESTING_STRATEGY.md`
- `README.md`

Then verify the current repository state against the POC requirements.

Run these commands if available:

```bash
mvn test
mvn package
```

If `espeak-ng` is installed and audio environment is available, also try:

```bash
java -jar target/zib-0.1.0.jar examples/demo.zib
```

Report exactly:

```markdown
## Verification summary
- ...

## Commands run
- command: result

## Requirements satisfied
- ...

## Requirements missing or uncertain
- ...

## Tests skipped or not run
- ...

## Evidence gaps
- ...

## Recommended next work order
- ...
```

A skipped test is not a passing test. A command that was not run is not evidence.

