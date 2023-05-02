## INSTRUCTIONS ON HOW TO RUN AND ADDING MONGO QUERY EXECUTION TESTS

### Dependencies
- Docker desktop should run locally.

### Running test suite
- Via Java (compiled-mode)
  - Running maven "test" task or right-clicking Test_Pure_MongoDB_ExecutionPlan.java class and running tests.
- Via Pure IDE (interpreted-mode)
  - By default we don't expose mongodb execution plan test module in the IDE so we must first make some changes before starting PureIDE:
1. Add to legend-engine-pure-ide-light module's pom.xml the following dependencies:

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

2. Add the following line to PureIDELight.java

<div align="center" style="width:100%">
<div style="width:90%" align="left">

    function go():Any[*]
        {
            let testConfig = meta::external::store::mongodb::executionTest::getCompiledTestConfig('4.1.1', 59555, '/core_mongodb_execution_test/test_setup/testSetupDSL.txt');
            let result =meta::external::store::mongodb::executionTest::testCase::graphfetch::filter::greaterThan::testGreaterThanNumber($testConfig);
    
            println('test result:');
            println($result);
        }
</div>
</div>


Replace the 59555 value with the port of your running docker container you noted down. Executing this will now only run the given test. Add/modify tests as needed.
