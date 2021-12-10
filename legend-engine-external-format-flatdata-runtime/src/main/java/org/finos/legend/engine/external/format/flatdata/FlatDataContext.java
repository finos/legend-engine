package org.finos.legend.engine.external.format.flatdata;

import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.ProcessingVariables;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.VariablesProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.Connection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.Cursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataDriverDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataReadDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataWriteDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ObjectToParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class FlatDataContext<T>
{
    private final List<FlatDataDriverDescription> descriptions = FlatDataDriverDescription.loadAll();

    public final FlatData schema;
    private final String definingPath;
    private final Map<String, Function<FlatDataRecordType, ParsedFlatDataToObject<?>>> toObjectFactories = Maps.mutable.empty();
    private final Map<String, Function<FlatDataRecordType, ObjectToParsedFlatData<?>>> fromObjectFactories = Maps.mutable.empty();

    public FlatDataContext(FlatData schema, String definingPath)
    {
        this.schema = schema;
        this.definingPath = definingPath;
    }

    public FlatDataContext<T> withSectionToObjectFactory(String sectionName, Function<FlatDataRecordType, ParsedFlatDataToObject<?>> factoryFactory)
    {
        toObjectFactories.put(sectionName, factoryFactory);
        return this;
    }

    public FlatDataContext<T> withSectionFromObjectFactory(String sectionName, Function<FlatDataRecordType, ObjectToParsedFlatData<?>> factoryFactory)
    {
        fromObjectFactories.put(sectionName, factoryFactory);
        return this;
    }

    public List<FlatDataReadDriver<T>> getReadDrivers(Connection connection)
    {
        ProcessingVariables variables = new ProcessingVariables(schema);
        FlatDataReadDriver<T> driver = null;
        List<FlatDataReadDriver<T>> drivers = new LinkedList<>();
        for (int i = schema.getSections().size() - 1; i >= 0; i--)
        {
            FlatDataSection section = schema.getSections().get(i);
            FlatDataDriverDescription description = descriptions.stream().filter(d -> d.getId().equals(section.getDriverId())).findFirst().orElseThrow(() -> new RuntimeException("No driver for: '" + section.getDriverId() + "'"));
            driver = description.newReadDriver(section, new SectionProcessingContext(connection, description, section, toObjectFactories.get(section.getName()), fromObjectFactories.get(section.getName()), variables, driver));
            drivers.add(0, driver);
        }
        return drivers;
    }

    public List<FlatDataWriteDriver<T>> getWriteDrivers(Connection connection)
    {
        ProcessingVariables variables = new ProcessingVariables(schema);
        FlatDataWriteDriver<T> driver = null;
        List<FlatDataWriteDriver<T>> drivers = new LinkedList<>();
        for (int i = schema.getSections().size() - 1; i >= 0; i--)
        {
            FlatDataSection section = schema.getSections().get(i);
            FlatDataDriverDescription description = descriptions.stream().filter(d -> d.getId().equals(section.getDriverId())).findFirst().orElseThrow(() -> new RuntimeException("No driver for: '" + section.getDriverId() + "'"));
            driver = description.newWriteDriver(section, new SectionProcessingContext(connection, description, section, toObjectFactories.get(section.getName()), fromObjectFactories.get(section.getName()), variables, driver));
            drivers.add(0, driver);
        }
        return drivers;
    }

    private class SectionProcessingContext extends VariablesProcessingContext
    {
        private final Connection connection;
        private final FlatDataSection section;
        private final FlatDataDriver nextDriver;
        private final Function<FlatDataRecordType, ParsedFlatDataToObject<?>> toObjectFactoryFactory;
        private final Function<FlatDataRecordType, ObjectToParsedFlatData<?>> fromObjectFactoryFactory;

        SectionProcessingContext(Connection connection, FlatDataDriverDescription description, FlatDataSection section, Function<FlatDataRecordType, ParsedFlatDataToObject<?>> toObjectFactoryFactory, Function<FlatDataRecordType, ObjectToParsedFlatData<?>> fromObjectFactoryFactory, ProcessingVariables variables, FlatDataDriver nextDriver)
        {
            super(variables, description.getDeclares());
            this.connection = connection;
            this.section = section;
            this.toObjectFactoryFactory = toObjectFactoryFactory;
            this.fromObjectFactoryFactory = fromObjectFactoryFactory;
            this.nextDriver = nextDriver;
        }

        @Override
        public String getDefiningPath()
        {
            return definingPath;
        }

        @Override
        public Connection getConnection()
        {
            return connection;
        }

        @Override
        public boolean isNextSectionReadyToStartAt(Cursor cursor)
        {
            return (nextDriver == null) ? cursor.isEndOfData() : nextDriver.canStartAt(cursor);
        }

        @Override
        public <T> ParsedFlatDataToObject<? extends T> createToObjectFactory(FlatDataRecordType recordType)
        {
            return (ParsedFlatDataToObject<? extends T>) Objects.requireNonNull(toObjectFactoryFactory, "No factory for section '" + section.getName() + "'").apply(recordType);
        }

        @Override
        public <T> ObjectToParsedFlatData<? extends T> createFromObjectFactory(FlatDataRecordType recordType)
        {
            return (ObjectToParsedFlatData<? extends T>) Objects.requireNonNull(fromObjectFactoryFactory, "No factory for section '" + section.getName() + "'").apply(recordType);
        }
    }
}
