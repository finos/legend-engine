<img align="right" width="40%" src="https://www.finos.org/hubfs/FINOS/finos-logo/FINOS_Icon_Wordmark_Name_RGB_horizontal.png">

# project-blueprint

Project blueprint is a GitHub repository template for all [Fintech Open Source Foundation (FINOS)](https://www.finos.org/) hosted GitHub repositories, contributed and maintained by FINOS as part of the [Open Developer Platform (ODP)](https://odp.finos.org) initiative.

## How to use this blueprint

1. Read the docs on [ODP website](https://odp.finos.org/docs/project-collaboration#finos-project-blueprint)
2. Clone this repository locally (`git clone https://github.com/finos/project-blueprint.git`)
3. Copy the `LICENSE`, `LICENSE.spdx`, and `NOTICE` files, as well as the entire `.github` directory, to your own repository (do _not_ copy this `README.md` file).
4. Copy the `README.template.md` file to your repository, and rename it to `README.md`.
5. Search and replace the following tokens in the newly copied files:

  | Token                        | Replace with                                                      |
  | ---------------------------- | ----------------------------------------------------------------- |
  | `{project name}`             | The name of the GitHub repository the project resides in.         |
  | `{yyyy}`                     | The year you started working on the code.                         |
  | `{current_year}`             | The current year.                                                 |
  | `{name of copyright owner}`  | The copyright owner of the code (typically you or your employer). |
  | `{email of copyright owner}` | The email address of the copyright owner of the code (if known).  |

5. Open the `NOTICE` file in a text editor and either remove the `{Other notices, as necessary}` token, or [add attributions if required by your code's dependencies](https://finosfoundation.atlassian.net/wiki/spaces/FINOS/pages/75530255/License+Categories).
6. Open the `README.md` file in a text editor and complete the content as appropriate for your project.
7. Add the [Apache license header to all of your source files](https://www.apache.org/licenses/LICENSE-2.0.html#apply).
8. Commit all of your changes.

## License

Copyright 2019 Fintech Open Source Foundation

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
