package org.finos.legend.engine.language.pure;

import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.junit.Test;

public class TestToPureGrammarRoundtripTests extends TestToPureGrammarRoundtrip
{
    protected org.finos.legend.pure.generated.Root_meta_pure_metamodel_serialization_grammar_Configuration getConfiguration(ExecutionSupport es)
    {
        return org.finos.legend.pure.generated.core_relational_relational_extensions_grammarSerializerExtension.Root_meta_relational_grammar_serialization_relationGrammarConfiguration__Configuration_1_(es);
    }

    public static final String FIRM_CLASS = "###Pure\n"+
            "Class models::Firm\n"+
            "{\n"+
            "  name: String[1];\n"+
            "}";

    public static final String PERSON_CLASS = "###Pure\n"+
            "Class models::Person\n"+
            "{\n"+
            "  name: String[1];\n"+
            "  firm: models::Firm[1];\n"+
            "}";

    @Test
    public void testClass() {
        test(FIRM_CLASS + "\n\n" + PERSON_CLASS);
    }

    @Test
    public void testEmptyMapping()
    {
        test("###Mapping\n" +
                "Mapping mappings::BaseMapping\n" +
                "(\n" +
                "\n)");
    }

    @Test
    public void testClassMappingWithDatabase()
    {
        String mapping = "###Mapping\n"+
                "Mapping mappings::FirmMapping\n"+
                "(\n"+
                "  *models::Firm[model_Firm]: Relational\n"+
                "  {\n"+
                "    ~primaryKey\n"+
                "    (\n"+
                "      [stores::SimpleDB]Firms.id\n"+
                "    )\n"+
                "    ~mainTable [stores::SimpleDB]Firms\n"+
                "    name: [stores::SimpleDB]Firms.name\n"+
                "  }\n"+
                ")\n"+
                "\n"+
                "###Mapping\n"+
                "Mapping mappings::PersonMapping\n"+
                "(\n"+
                "  *models::Person[model_Person]: Relational\n"+
                "  {\n"+
                "    ~primaryKey\n"+
                "    (\n"+
                "      [stores::SimpleDB]People.id\n"+
                "    )\n"+
                "    ~mainTable [stores::SimpleDB]People\n"+
                "    name: [stores::SimpleDB]People.name,\n"+
                "    firm[model_Firm]: [stores::SimpleDB]@FirmPerson\n"+
                "  }\n"+
                ")";
        String database = "###Relational\n"+
                "Database stores::SimpleDB\n"+
                "(\n"+
                "  Table People\n"+
                "  (\n"+
                "    id INTEGER PRIMARY KEY,\n"+
                "    name VARCHAR(200),\n"+
                "    firm_id INTEGER\n"+
                "  )\n"+
                "  Table Firms\n"+
                "  (\n"+
                "    id INTEGER PRIMARY KEY,\n"+
                "    name VARCHAR(200)\n"+
                "  )\n"+
                "  Join FirmPerson(People.firm_id = Firms.id)\n"+
                ")\n";

        test(FIRM_CLASS+"\n"+PERSON_CLASS+"\n"+mapping+"\n"+database,
                FIRM_CLASS+"\n\n"+database+"\n"+PERSON_CLASS+"\n\n"+mapping);
    }

    @Test
    public void testMappingIncludesMappingDatabaseIncludesDatabase()
    {
        test( FIRM_CLASS+"\n"+PERSON_CLASS+"\n\n"+
                        "###Mapping\n"+
                        "Mapping mappings::BaseMapping\n"+
                        "(\n"+
                        "  models::Firm[model_Firm]: Relational\n"+
                        "  {\n"+
                        "    name: [stores::ExtendedDB]FirmSchema.Firms.name\n"+
                        "  }\n"+
                        "  models::Person[model_Person]: Relational\n"+
                        "  {\n"+
                        "    name: [stores::ExtendedDB]PeopleSchema.People.name\n"+
                        "  }\n"+
                        ")\n"+
                        "Mapping mappings::ExtendedMapping\n"+
                        "(\n"+
                        "  include mapping mappings::BaseMapping\n"+
                        ")\n"+
                        "###Relational\n"+
                        "Database stores::BaseDB\n"+
                        "(\n"+
                        "  Schema PeopleSchema\n"+
                        "  (\n"+
                        "    Table People\n"+
                        "    (\n"+
                        "      id INTEGER PRIMARY KEY,\n"+
                        "      name VARCHAR(200),\n"+
                        "      firm_id INTEGER\n"+
                        "    )\n"+
                        "  )\n"+
                        ")\n"+
                        "Database stores::ExtendedDB\n"+
                        "(\n"+
                        "  include stores::BaseDB\n"+
                        "  Schema FirmSchema\n"+
                        "  (\n"+
                        "    Table Firms\n"+
                        "    (\n"+
                        "      id INTEGER PRIMARY KEY,\n"+
                        "      name VARCHAR(200)\n"+
                        "    )\n"+
                        "  )\n"+
                        ")",
                "###Pure\n"+
                        "Class models::Firm\n"+
                        "{\n"+
                        "  name: String[1];\n"+
                        "}\n"+
                        "\n"+
                        "###Relational\n"+
                        "Database stores::ExtendedDB\n"+
                        "(\n"+
                        "  include stores::BaseDB\n"+
                        "  Schema FirmSchema\n"+
                        "  (\n"+
                        "    Table Firms\n"+
                        "    (\n"+
                        "      id INTEGER PRIMARY KEY,\n"+
                        "      name VARCHAR(200)\n"+
                        "    )\n"+
                        "  )\n"+
                        ")\n"+
                        "\n"+
                        "###Mapping\n"+
                        "Mapping mappings::BaseMapping\n"+
                        "(\n"+
                        "  *models::Firm[model_Firm]: Relational\n"+
                        "  {\n"+
                        "    ~primaryKey\n"+
                        "    (\n"+
                        "      [stores::ExtendedDB]FirmSchema.Firms.id\n"+
                        "    )\n"+
                        "    ~mainTable [stores::ExtendedDB]FirmSchema.Firms\n"+
                        "    name: [stores::ExtendedDB]FirmSchema.Firms.name\n"+
                        "  }\n"+
                        "  *models::Person[model_Person]: Relational\n"+
                        "  {\n"+
                        "    ~primaryKey\n"+
                        "    (\n"+
                        "      [stores::ExtendedDB]PeopleSchema.People.id\n"+
                        "    )\n"+
                        "    ~mainTable [stores::ExtendedDB]PeopleSchema.People\n"+
                        "    name: [stores::ExtendedDB]PeopleSchema.People.name\n"+
                        "  }\n"+
                        ")\n"+
                        "\n"+
                        "###Pure\n"+
                        "Class models::Person\n"+
                        "{\n"+
                        "  name: String[1];\n"+
                        "  firm: models::Firm[1];\n"+
                        "}\n"+
                        "\n"+
                        "###Mapping\n"+
                        "Mapping mappings::ExtendedMapping\n"+
                        "(\n"+
                        "  include mapping mappings::BaseMapping\n"+
                        ")\n"+
                        "\n"+
                        "###Relational\n"+
                        "Database stores::BaseDB\n"+
                        "(\n"+
                        "  Schema PeopleSchema\n"+
                        "  (\n"+
                        "    Table People\n"+
                        "    (\n"+
                        "      id INTEGER PRIMARY KEY,\n"+
                        "      name VARCHAR(200),\n"+
                        "      firm_id INTEGER\n"+
                        "    )\n"+
                        "  )\n"+
                        ")"
        );
    }
}
