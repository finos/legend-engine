# Overview
Pure functions can be thought of as the platform's api. The Pure Compatibility Testing (PCT) Framework is how
we specify expectations around the api. This guide enables you to contribute platform functions while leveraging PCT.

## Development Setup
Set up development environment for *legend-engine*:
- https://github.com/finos/legend-engine/blob/master/README.md#development-setup

## How To Add Pure Platform Functions
A Pure Platform Function has an implementation in Pure code. See [Pure Function How-To](purefunction-howto.md) for step-by-step instructions.

## How To Add Java Platform Functions
A "native" function has an implementation in Java (the native language of the platform) and no implementation in Pure. See [the Native How-To](native-howto.md) for step-by-step instructions.

## How to Wire your Function to run on Relational Databases
A key feature of Legend is that functions on the platform are cross-compiled, or "wired," to target runtimes. "Wiring" to target databases is required if we wish the function to be evaluated in the target database's runtime. See [the Wiring How-To](wiring-howto.md) for a step-by-step guide.

## Finishing Up / Running Database-specific PCT Tests
The final step involves running PCT Tests against database targets for the functions you've defined/modified. See [this page](expected-failures-howto.md) for information on how
to run the tests and log expected failures.

-------------
# References
## Conventions
For conventions and best practices, see [this page](conventions.md)

## Taxonomy Guide
An overview of Pure Function Taxonomy is [here](taxonomy.md)

## Key Concepts / Glossary, and FAQ
For concepts / glossary, and FAQ see [this page](concepts-glossary.md)
