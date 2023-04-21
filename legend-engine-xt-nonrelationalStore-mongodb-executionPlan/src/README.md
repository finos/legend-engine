## INSTRUCTIONS ON HOW TO EXECUTE A MONGO QUERY LOCALLY

### Dependencies
- Docker desktop should run locally.

### Platform Binder issue

Currently there is an issue with the platform binding, so there is a manual setting that needs to be changed in order to run the tests or a query from the PURE IDE.
- Search for the `mongodb_test_utils.pure` file.
- On line 27, remove the `<<LocalPlatformBinding.TestPlanBinder>>` next to the `function`.
- Now you can run the query/tests from the PURE IDE. 
- Adding the `<<LocalPlatformBinding.TestPlanBinder>>` back, will allow you to run the tests from Intellij.

### Execution
- Run PureIDELight.java
  - Before running the ide, click the Run configurations between the Build Project button(hammer symbol) and the Run button on the upper right corner of Intellij:
    - Select "Edit Configurations..."
    - Under "Application", select "PureIDELight"
    - Select "Modify Options"
    - Select "Add VM Options"
    - In the "VM Options" input field that appeared after clicking "Add VM Options", add "-Dmongo_pwd=sa" (without the double quotes)
    - Click "Apply"
- Run MongoTestServerInvoker.java
  - By running this, a dockerized mongo db spins up, and contains the data from this file: src/test/resources/mongoData/person.json
  - After running the MongoTestServerInvoker, in the console you will see the Running port. You will need to use this port when running the execute function in the welcome.pure.
    - In the PURE IDE, go the File Explorer on the left hand side.
    - Find the `pureSetup.txt` file under `/core_nonrelational_mongodb_java_platform_binding/test`.
    - On the `###Connection` section, at the `serverURLs`, add the port number that the MongoTestServerInvoker generated.
- Use the welcome.pure pasted on the section below to execute the query using F9.

#### Welcome.pure

<div align="center" style="width:100%">
<div style="width:90%" align="left">

    import meta::external::store::mongodb::executionTest::testCase::utils::*;
    import meta::external::store::mongodb::extension::*;
    import meta::pure::mapping::*;
    import meta::external::shared::format::metamodel::*;
    import meta::legend::*;
    import meta::pure::functions::tests::collection::*;
    import meta::pure::graphFetch::execution::*;
    import meta::external::store::mongodb::executionPlan::platformBinding::legendJava::test::*;
    
    function go():Any[*]
    {
      let file = meta::pure::functions::io::readFile('core_nonrelational_mongodb_java_platform_binding/test/pureSetup.txt')->toOne();
      
      let compiled = meta::legend::compile($file);
      
      let compiledMapping = $compiled->at(3)->cast(@meta::pure::mapping::Mapping)->toOne();
      
      let compiledRuntime = $compiled->at(1)->cast(@meta::pure::runtime::PackageableRuntime).runtimeValue->cast(@meta::pure::runtime::Runtime);
      
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
          phoneNumber,
          otherPhoneNumbers,
          email,
          address {
            street,
            city,
            postalCode,
            country {
              countryCode,
              continent
            }
          },
          firm {
            legalName,
            dateFounded,
            industrySectors,
            address {
              street,
              postalCode,
              country {
                countryCode,
                continent
              }
            }
          }
        }
      }#;
    
    
      let personQuery = {|Person.all()->filter(x| $x.firm.legalName == 'Voolia')->from($compiledMapping, $compiledRuntime)->graphFetch($personTree)->serialize($personTree)};
    
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
