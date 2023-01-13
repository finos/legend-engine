
## Development setup

### Setting up ANTLR4 to test grammar directly

1. Download ANTLR 4.8  (antlr4-4.8-complete.jar) from here https://repo1.maven.org/maven2/org/antlr/antlr4/4.8/


2. On mac edit your  ~/.bash_profile to point to ANTLR following "Executing the instructions on Linux/Mac OS" at https://tomassetti.me/antlr-mega-tutorial/


3. Verify you have antlr correctly set up by running antlr4 in a terminal, you should be able to the version installed as output


4. From "legend-engine-xt-nonrelationalStore-mongodb-grammar" folder, invoke: mvn clean install && cd ./target/classes && grun org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDbQuery databaseCommand -tokens '../../src/test/resources/input1.js' -gui
