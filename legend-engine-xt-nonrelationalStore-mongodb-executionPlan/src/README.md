## INSTRUCTIONS ON HOW TO EXECUTE A MONGO QUERY LOCALLY

### Dependencies
- Docker desktop should run locally.
- The PureIDELight (legend-engine-pure-ide-light) needs the following dependency

#### legend-engine-pure-ide-light dependency

    <dependency>
      <groupId>org.finos.legend.engine</groupId>
      <artifactId>legend-engine-xt-nonrelationalStore-mongodb-executionPlan</artifactId>
      <scope>runtime</scope>
    </dependency>

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
  - After running the MongoTestServerInvoker, in the console you will see the Running port. You will need to use this port when running the execute function in the welcome.pure
- Use the welcome.pure pasted on the section below to execute the query. Change the port variable on welcome.pure with the port that the MongoTestServerInvoker generated on the previous step

#### Welcome.pure

<div align="center" style="width:100%">
<div style="width:90%" align="left">

    import meta::pure::mapping::*;
    import meta::external::shared::format::metamodel::*;
    import meta::legend::*;
    import meta::pure::functions::tests::collection::*;
    import meta::pure::graphFetch::execution::*;
    import meta::external::store::mongodb::executionPlan::platformBinding::legendJava::test::*;

    function go():Any[*]
    {
      let port = 50016; // update the port with what you get from the MongoTestServerInvoker
      
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
      
      
      let personQuery = {|Person.all()->from($personMapping, $mongoRuntime)->filter(x|  $x.firm.legalName == 'Goldman Sachs')->graphFetch($personTree)->serialize($personTree)};
       
      let result = meta::legend::execute(
        $personQuery,
        [],
        $executionContext,
        meta::external::store::mongodb::executionPlan::platformBinding::legendJava::mongoDBLegendJavaPlatformBindingExtensions()
      );
      println($result->meta::json::parseJSON()->meta::json::toPrettyJSONString());

    }
</div>
</div>
