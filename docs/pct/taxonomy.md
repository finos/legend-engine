# Legend Platform Function Taxonomy
Below describes the categories of platform functions. Most of the functions you add will end up in Standard. 

The file (directory) structure of *.pure files containing Platform Functions has been harmonized to reflect the four categories of Pure Functions:
1. Grammar (legend-pure) - functions needed for the grammar of the platform. Should almost never be modified. If needed, consult with a Legend CODEOWNER.
2. Essential (legend-pure) - foundational functions required for running tests in the platform (e.g. assert, eq). Should almost never be modified. If needed, consult with a Legend CODEOWNER.
3. Standard (legend-engine) - majority of platform functions will fall into this category
4. Relation (legend-engine) - platform functions specific to operating on relations (e.g. join)

Note that *pure package* naming will eventually be harmonized to a hierarchy similar to the directory structure of *legend-pure* "essential" functions. However, for now, please consult with an SME to confirm the proposed package/naming for your PCT function.