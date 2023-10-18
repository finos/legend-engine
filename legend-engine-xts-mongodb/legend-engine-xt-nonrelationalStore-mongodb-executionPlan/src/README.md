## INSTRUCTIONS ON HOW TO EXECUTE A MONGO QUERY LOCALLY

### Dependencies
- Docker desktop should run locally.
- Add to legend-engine-pure-ide-light module's pom.xml the following dependencies: (you might need to reload the maven dependencies after updating the pom file).

<div align="center" style="width:100%">
<div style="width:90%" align="left">

      <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-xt-nonrelationalStore-mongodb-executionPlan</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-xt-nonrelationalStore-mongodb-executionPlan-test</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-xt-nonrelationalStore-mongodb-protocol</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-xt-nonrelationalStore-mongodb-grammar-integration</artifactId>
            <version>${project.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-xt-nonrelationalStore-mongodb-grammar</artifactId>
            <scope>runtime</scope>
        </dependency>
</div>
</div>

### Execution
- Run PureIDELight.java
  - Before running the ide, click the Run configurations between the Build Project button(hammer symbol) and the Run button on the upper right corner of Intellij:
    - Select "Edit Configurations..."
    - Under "Application", select "PureIDELight"
    - Select "Modify Options"
    - Select "Add VM Options"
    - In the "VM Options" input field that appeared after clicking "Add VM Options", add "-Dmongo_pwd=sa" (without the double quotes)
    - Click "Apply"
- In module executionPlan-test, run MongoTestContainerInvoker.java
  - By running this, a dockerized mongo db spins up, and contains the data from this file: /legend-engine-xt-nonrelationalStore-mongodb-executionPlan-test/src/main/resources/core_mongodb_execution_test/test_setup/person.json
  - After running the MongoTestContainerInvoker, in the console you will see the Running port. You will need to use this port when running the execute function in the welcome.pure.

#### Welcome.pure for running a test
<div align="center" style="width:100%">
<div style="width:90%" align="left">

    function go():Any[*]
        {
            let testConfig = meta::external::store::mongodb::executionTest::getCompiledTestConfig('4.1.1', 'localhost', port_number, '/core_mongodb_execution_test/test_setup/testSetupDSL.txt');
            let result =meta::external::store::mongodb::executionTest::testCase::graphfetch::filter::greaterThan::testGreaterThanNumber($testConfig);
    
            println('test result:');
            println($result);
        }
</div>
</div>
    - Use the welcome.pure pasted on the section above to run a test using F9, the testGreaterThanNumber test in this case. Replace the <b>port_number</b> with the one you noted down on the previous section.

#### Welcome.pure for query execution

<div align="center" style="width:100%">
<div style="width:90%" align="left">

    import meta::external::store::mongodb::executionTest::testCase::utils::*;
    import meta::external::store::mongodb::extension::*;
    import meta::pure::mapping::*;
    import meta::external::format::shared::metamodel::*;
    import meta::legend::*;
    import meta::pure::functions::tests::collection::*;
    import meta::pure::graphFetch::execution::*;
    import meta::external::store::mongodb::executionTest::domain::*;
    import meta::pure::functions::boolean::*;

    function go():Any[*]
    {
      let file = meta::pure::functions::io::readFile('/core_mongodb_execution_test/test_setup/testSetupDSL.txt')->toOne();
      
      let mongoDBHost = 'localhost';
      
      let mongoDBPort = 'port_number';
      
      let detokenizedFile = $file->replace('serverURLs: [__HOST_TOKEN__:__PORT_TOKEN__];', 'serverURLs: [' + $mongoDBHost + ':' + $mongoDBPort->toString() + '];');
      
      let compiled = meta::legend::compile($detokenizedFile);
      
      let compiledMapping = $compiled->at(3)->cast(@meta::pure::mapping::Mapping)->toOne();
      
      let compiledRuntime = $compiled->at(1)->cast(@meta::pure::runtime::PackageableRuntime).runtimeValue->cast(@meta::core::runtime::Runtime);
      
      let executionContext = ^meta::external::store::mongodb::functions::pureToDatabaseCommand::MongoDBExecutionContext(queryTimeOutInSeconds=5, enableConstraints=false);
      let debugContext = ^meta::pure::tools::DebugContext(
        debug=true,
        space=' '
      );
      
      let personTree = #{
        Person {
          firstName,
          lastName,
          age,
          birthDate,
          legallyCompetent,
          otherPhoneNumbers,
          address {
            city,
            buildingNumber,
            isActive,
            lastUpdated
          },
          department {
            name,
            numberOfEmployees,
            dateCreated,
            isActive
          },
          firm {
            legalName,
            public,
            dateFounded,
            numberOfEmployees,
            address {
              city,
              buildingNumber,
              isActive,
              lastUpdated
            },
            department {
              name,
              numberOfEmployees,
              dateCreated,
              isActive
            }
          }
        }
      }#;
      
      let personQuery = {|Person.all()->filter(x| $x.firm.legalName == 'Yodoo' )->from($compiledMapping, $compiledRuntime)->graphFetch($personTree)->serialize($personTree)};
      
      let result = meta::legend::execute(
        $personQuery,
        [],
        $executionContext,
        meta::external::store::mongodb::executionPlan::platformBinding::legendJava::mongoDBLegendJavaPlatformBindingExtensions()
      )->meta::json::parseJSON()->meta::json::toPrettyJSONString();
      println($result);
    }

</div>
</div>

- Use the welcome.pure pasted on the section above to execute the query using F9. Replace the <b>port_number</b> with the one you noted down on the previous section.

