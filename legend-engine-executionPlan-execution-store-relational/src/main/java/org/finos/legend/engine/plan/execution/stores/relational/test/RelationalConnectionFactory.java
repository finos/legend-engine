package org.finos.legend.engine.plan.execution.stores.relational.test;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.ElementsTestDataSource;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.StringTestDataSource;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.mappingTest.RelationalInputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.mappingTest.RelationalInputType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.text.Text;


import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RelationalConnectionFactory implements ConnectionFactoryExtension
{
    @Override
    public Optional<Connection> tryBuildFromInputData(InputData inputData, PureModelContextData pureModelContextData)
    {
        if (inputData instanceof RelationalInputData)
        {
            RelationalInputData relationalInputData = (RelationalInputData) inputData;
            RelationalDatabaseConnection connection = new RelationalDatabaseConnection();
            connection.databaseType = DatabaseType.H2;
            connection.type = DatabaseType.H2;
            connection.element = relationalInputData.database;
            connection.authenticationStrategy = new TestDatabaseAuthenticationStrategy();
            LocalH2DatasourceSpecification localH2DatasourceSpecification = new LocalH2DatasourceSpecification();
            if (relationalInputData.testDataSource instanceof ElementsTestDataSource)
            {
                List<String> textElements = ((ElementsTestDataSource) relationalInputData.testDataSource).textElements;
                Map<String, Text> textElementMap = pureModelContextData.getElementsOfType(Text.class).stream().collect(Collectors.toMap(Text::getPath, Function.identity()));
                List<Text> textElementList = textElements.stream().map(path -> textElementMap.get(path)).collect(Collectors.toList());
                if(textElementList.stream().map(ele -> ele.type).collect(Collectors.toSet()).size() == 1)
                {
                    switch(textElementList.get(0).type.toUpperCase())
                    {
                        case "SQL" :  localH2DatasourceSpecification.testDataSetupSqls = textElementList.stream().map(textElement -> textElement.content.split("(?<!\\\\);")).flatMap(data -> Arrays.stream(data)).map(r -> r.replace("\\;", ";") + ";").collect(Collectors.toList());
                            break;
                        case "CSV" :  localH2DatasourceSpecification.testDataSetupCsv = textElementList.stream().map(textElement-> textElement.content.replaceAll("[\\r]+", "")).collect(Collectors.joining("\n"));
                            break;
                        default:
                            throw new RuntimeException(textElementList.get(0).type + " is not supported");
                    }
                }
                else
                {
                    throw new RuntimeException("The text elements provided are of different input types");
                }
            }
            else
             {
                 String data = ((StringTestDataSource) relationalInputData.testDataSource).data;
                if (relationalInputData.inputType == RelationalInputType.SQL)
                {
                    localH2DatasourceSpecification.testDataSetupSqls = Lists.mutable.of(data.split("(?<!\\\\);")).collect(r -> r.replace("\\;", ";") + ";");
                }
                else if (relationalInputData.inputType == RelationalInputType.CSV)
                {
                    localH2DatasourceSpecification.testDataSetupCsv = data;
                }
                else
                {
                    throw new RuntimeException(relationalInputData.inputType + " is not supported");
                }
            }
            connection.datasourceSpecification = localH2DatasourceSpecification;
            return Optional.of(connection);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Connection> tryBuildFromConnection(Connection connection, String testData, String element)
    {
        return ConnectionFactoryExtension.super.tryBuildFromConnection(connection, testData, element);
    }
}
