## Legend Platform Conventions
Conventions ensure that we can achieve **Goal - a clean (minimal) and transparent platform api**.

It is critical that utmost care is taken when deciding on:
* Function Signature
* Code Location
* Naming/Style

### Style
- One file per PCT Function
    - All Function Signatures for the PCT Function belong in the same file
    - All PCT tests for the PCT function belong in the same file
- package names are all lower-case
- function names are camelCase, with the first letter lower-case
- function signatures **must have docstring (doc.doc)**

### Practices
#### PCT Tests
Checklist for writing good PCT Tests:
- [ ] Are all your PCT Tests defined in the same *.pure filea s your function signature?
- [ ] Test organization - tests should be granular such that a failure can be easily isolated. E.g. testing Float inputs should be declared in a separate
test from one that tests for Integer inputs. That way, the test failure clearly isolates what may be wrong.
- [ ] Test package naming - naming related tests to belong in the same Pure Package will enable expectedFailures or other configs to be done per package.
###### E.g.
```Java
// Example Good package name: a test package named like so
meta::pure::functions::date::tests::timeBucket

// can be registered in expectedFailuers with one line for unsupported targets
pack("meta::pure::functions::date::tests::timeBucket", "\"meta::pure::functions::date::timeBucket_DateTime_1__Integer_1__DurationUnit_1__DateTime_1_ is not supported yet!\"")

// Example Bad package name: a test package named like so can only be registered in expectedFailures one by one
meta::pure::functions::date::tests
```

- [ ] Did you account for edge cases? e.g. if testing Number inputs, did you try very large/small numbers?

#### Error Messages
PCT measure the level of cross-target support for a given Platform Function. When contributing to PCT on Legend, keep this preference order in mind:

``` PCT Passed(Green) > Failed PCT with Good Error Message > Failed PCT ```

*One key priority is to improve error messages on the platform - Good Error Messages are important.*

#### Changes to existing Ref/Spec Implementations or foundational classes
It is highly unlikely you will need to make changes to existing reference specs/implementations or foundational classes.
If you feel the need, consult with a CODEOWNER on your proposed change and rationale.




