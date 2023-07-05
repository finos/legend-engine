// Copyright 2021 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.compiler.test;

public class TestServiceStoreCompilationUtils
{
    static final String TEST_BINDING =
            "###Pure\n" +
                    "Class test::Person\n" +
                    "{\n" +
                    "  fullName: String[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class test::Firm\n" +
                    "{\n" +
                    "  firmName: String[1];\n" +
                    "}\n" +
                    "\n" +
                    "Enum test::Enum\n" +
                    "{\n" +
                    "  A, B, C\n" +
                    "}\n" +
                    "\n" +
                    "###ExternalFormat\n" +
                    "SchemaSet test::SchemaSet\n" +
                    "{\n" +
                    "  format: Example;\n" +
                    "  schemas: [ { content: 'example'; } ];\n" +
                    "}\n" +
                    "\n" +
                    "Binding test::TestBinding\n" +
                    "{\n" +
                    "  schemaSet: test::SchemaSet;\n" +
                    "  contentType: 'text/example';\n" +
                    "  modelIncludes: [\n" +
                    "    test::Person\n" +
                    "  ];\n" +
                    "}\n" +
                    "\n";

    static final String FLATDATA_BINDING =
            "###Pure\n" +
                    "Class test::model::A\n" +
                    "{\n" +
                    "  alpha   : String[1];\n" +
                    "  beta    : Boolean[0..1];\n" +
                    "  gamma   : Integer[1];\n" +
                    "  delta   : Float[1];\n" +
                    "  epsilon : Decimal[1];\n" +
                    "  zeta    : Float[1];\n" +
                    "  eta     : Decimal[1];\n" +
                    "  theta   : StrictDate[1];\n" +
                    "  iota    : DateTime[1];\n" +
                    "}\n" +
                    "###ExternalFormat\n" +
                    "SchemaSet test::SchemaSet\n" +
                    "{\n" +
                    "  format: FlatData;\n" +
                    "  schemas: [ { content: 'section A: DelimitedWithHeadings\\n{\\n  scope.untilEof;\\n  delimiter: \\',\\';\\n\\n  Record\\n  {\\n    alpha   : STRING;\\n    beta    : BOOLEAN(optional);\\n    gamma   : INTEGER;\\n    delta   : INTEGER;\\n    epsilon : INTEGER;\\n    zeta    : DECIMAL;\\n    eta     : DECIMAL;\\n    theta   : DATE;\\n    iota    : DATETIME;\\n  }\\n}'; } ];\n" +
                    "}\n" +
                    "\n" +
                    "Binding test::Binding\n" +
                    "{\n" +
                    "  schemaSet: test::SchemaSet;\n" +
                    "  contentType: 'application/x.flatdata';\n" +
                    "  modelIncludes: [ test::model::A ];\n" +
                    "}\n" +
                    "\n" +
                    "###Pure\n" +
                    "Class test::model::B\n" +
                    "{\n" +
                    "  alpha   : String[1];\n" +
                    "  beta    : Boolean[0..1];\n" +
                    "  gamma   : Integer[1];\n" +
                    "}\n" +
                    "###ExternalFormat\n" +
                    "SchemaSet test::SchemaSet2\n" +
                    "{\n" +
                    "  format: FlatData;\n" +
                    "  schemas: [ { content: 'section A: DelimitedWithHeadings\\n{\\n  scope.untilEof;\\n  delimiter: \\',\\';\\n\\n  Record\\n  {\\n    alpha   : STRING;\\n    beta    : BOOLEAN(optional);\\n    gamma   : INTEGER;\\n  }\\n}'; } ];\n" +
                    "}\n" +
                    "\n" +
                    "Binding test::Binding2\n" +
                    "{\n" +
                    "  schemaSet: test::SchemaSet2;\n" +
                    "  contentType: 'application/x.flatdata';\n" +
                    "  modelIncludes: [ test::model::B ];\n" +
                    "}\n";

    static final String JSON_BINDING =
            "###Pure\n" +
                    "import meta::external::store::service::showcase::domain::*;\n" +
                    "\n" +
                    "Class meta::external::store::service::showcase::domain::ApiResponse\n" +
                    "{\n" +
                    "    metadata  : Metadata[1];\n" +
                    "    employees : Person[*];\n" +
                    "    firms     : Firm[*];\n" +
                    "}\n" +
                    "\n" +
                    "Class meta::external::store::service::showcase::domain::Metadata\n" +
                    "{\n" +
                    "    noOfRecords : Integer[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class meta::external::store::service::showcase::domain::Person\n" +
                    "{\n" +
                    "    firstName  : String[1];\n" +
                    "    lastName   : String[1];\n" +
                    "    middleName : String[0..1];\n" +
                    "}\n" +
                    "\n" +
                    "Class meta::external::store::service::showcase::domain::Firm\n" +
                    "{\n" +
                    "    firmName : String[1];\n" +
                    "    firmId   : Integer[1];\n" +
                    "    address  : Address[*];\n" +
                    "}\n" +
                    "\n" +
                    "Class meta::external::store::service::showcase::domain::Address\n" +
                    "{\n" +
                    "    street : String[1];\n" +
                    "}\n" +
                    "\n" +
                    "Association meta::external::store::service::showcase::domain::Employment\n" +
                    "{\n" +
                    "    employees : Person[*];\n" +
                    "    firm      : Firm[0..1];\n" +
                    "}\n\n" +
                    "###ServiceStore\n" +
                    "ServiceStore meta::external::store::service::showcase::store::EmployeesServiceStore\n" +
                    "(\n" +
                    "   description : 'Showcase Service Store Fragment support';\n" +
                    "\n" +
                    "   Service EmployeesService\n" +
                    "   (\n" +
                    "      path     : '/employees';\n" +
                    "      method   : GET;\n" +
                    "      security : [];\n" +
                    "      response : [meta::external::store::service::showcase::domain::ApiResponse <- meta::external::store::service::showcase::store::ApiResponseSchemaBinding];\n" +
                    "   )\n" +
                    "\n" +
                    "   Service EmployeesServiceByFirmId\n" +
                    "   (\n" +
                    "      path     : '/employeesByFirmId/{firmId}';\n" +
                    "      method   : GET;\n" +
                    "      parameters :\n" +
                    "      (\n" +
                    "         firmId : Integer (location = path)\n" +
                    "      );\n" +
                    "      security : [];\n" +
                    "      response : [meta::external::store::service::showcase::domain::ApiResponse <- meta::external::store::service::showcase::store::ApiResponseSchemaBinding];\n" +
                    "   )\n" +
                    ")\n" +
                    "\n" +
                    "###ExternalFormat\n" +
                    "Binding meta::external::store::service::showcase::store::ApiResponseSchemaBinding\n" +
                    "{\n" +
                    "  contentType   : 'application/json';\n" +
                    "  modelIncludes : [\n" +
                    "                    meta::external::store::service::showcase::domain::Metadata,\n" +
                    "                    meta::external::store::service::showcase::domain::Person,\n" +
                    "                    meta::external::store::service::showcase::domain::Firm,\n" +
                    "                    meta::external::store::service::showcase::domain::Address,\n" +
                    "                    meta::external::store::service::showcase::domain::ApiResponse\n" +
                    "                  ];\n" +
                    "}\n\n";
}
