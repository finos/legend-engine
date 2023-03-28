## INSTRUCTIONS ON HOW TO EXECUTE A MONGO QUERY LOCALLY

### Dependencies
- Docker desktop should run locally.

### Execution
- Run Server.java
- Run PureIDELight.java
- Run MongoTestServerInvoker.java
  - By running this, a dockerized mongo db spins up, and contains the data from this file: src/test/resources/mongoData/person.json
  - After running the MongoTestServerInvoker, in the console you will see the Running port. You will need to use this port when running the execute
- Use the welcome.pure pasted on the section below to execute the query. Change the port variable on welcome.pure with the port that the MongoTestServerInvoker generated on the previous step


### Welcome.pure

<div align="center" style="width:100%">
<div style="width:90%" align="left">

    import meta::external::shared::format::metamodel::*;
    import meta::legend::*;
    import meta::pure::functions::tests::collection::*;
    import meta::pure::graphFetch::execution::*;
    import meta::external::store::mongodb::executionPlan::platformBinding::legendJava::test::*;
    
    function go():Any[*]
    {
      let port = 50874; // update the port with what you get from the MongoTestServerInvoker
    
      let personMapping = meta::external::store::mongodb::executionPlan::platformBinding::legendJava::test::getTestMongoPersonMapping();
      let mongoRuntime = meta::external::store::mongodb::executionPlan::platformBinding::legendJava::test::getMongoRuntime($port);
      let executionContext = ^meta::external::store::mongodb::functions::pureToDatabaseCommand::MongoDBExecutionContext(queryTimeOutInSeconds=5, enableConstraints=false);
      let debugContext = ^meta::pure::tools::DebugContext(
        debug=false,
        space=' '
      );

      let personTree = #{
        Person {
          firstName,
          lastName,
          firm {
            legalName,
            address
          }
        }
      }#;
      
      let personQuery = {|Person.all()->filter(x|  $x.firm.legalName == 'Goldman Sachs')->graphFetch($personTree)->serialize($personTree)};
      
      let result = execute($personQuery, $personMapping, $mongoRuntime, meta::external::store::mongodb::extension::mongoDBExtensions()->concatenate(meta::external::format::json::extension::jsonSchemaFormatExtension())).values;
      println($result);
    
    }
</div>
</div>
