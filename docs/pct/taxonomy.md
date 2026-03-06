# Legend Platform Function Taxonomy
Below describes the categories of platform functions. Most of the functions you add will end up in Standard or Relation.

The file (directory) structure of *.pure files containing Platform Functions has been harmonized to reflect the categories of Pure Functions:

| Category | Repository | Description | Examples |
|----------|------------|-------------|----------|
| **Grammar** | legend-pure | Functions needed for the grammar of the platform. Should almost never be modified. | `if`, `let`, `match` |
| **Essential** | legend-pure | Foundational functions required for running tests (e.g., assert, eq). Should almost never be modified. | `assert`, `assertEquals`, `eq` |
| **Standard** | legend-engine | Majority of platform functions fall into this category. | `cosh`, `sin`, `abs`, `between` |
| **Relation** | legend-engine | Functions specific to operating on relations (tabular data). | `zScore`, `extend`, `groupBy`, `join` |
| **Unclassified** | legend-engine | Functions that haven't been categorized yet or don't fit elsewhere. | Various utility functions |

### Which category should I use?
- **Adding a math/string/date function?** → Standard
- **Adding a function that operates on `Relation<T>`?** → Relation
- **Unsure?** → Consult with a Legend CODEOWNER

Note that *pure package* naming will eventually be harmonized to a hierarchy similar to the directory structure of *legend-pure* "essential" functions. However, for now, please consult with an SME to confirm the proposed package/naming for your PCT function.