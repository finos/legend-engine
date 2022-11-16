# Service Post Validations

## Overview
A `Service Post Validation` provides a means of executing pre-defined validation checks against the output of a `Service`.

A Post Validation is expressed as a collection of `Assertions`. An `Assertion` is expressed as a lambda function that accepts the output of the service as a single parameter and returns a Boolean.

An Assertion is written such that the violations of the validation check would be returned if any were found. The output of the Assertion is then asserted to be an empty set.  If the set of violations is empty, the validation assertion passed, else it failed.

Assertions are combined with the service query and executed using the defined `Mapping` and `Runtime`.

Post Validations are executed via a HTTP call. A single Assertion can be ran at a time. The assertion ID is provided with the HTTP request.

## Specification
A Post Validation is expressed as such, with the Type and Multiplicity inferred from the Service query:
```
Class meta::legend::service::metamodel::PostValidation<T|m>
{
  description: String[1];
  parameters: Function<Any>[*];
  assertions: meta::legend::service::metamodel::PostValidationAssertion<T|m>[1..*];
}

Class meta::legend::service::metamodel::PostValidationAssertion<T|m>
{
  id: String[1];
  assertion: Function<{T[m]->Boolean[1]}>[1];
}
```

Post Validations are defined within a Service definition:
```
###Service
Service meta::validation::test::DemoService
{
    ...
    execution: Single
    {
        query: |meta::validation::test::Person.all()->project([col(p|$p.firstName, 'firstName'), col(p|$p.lastName, 'lastName'), col(p|$p.age, 'age')]);
        ...
    }
    postValidations:
    [
        {
            description: 'A good description of the validation';
            params: [];
            assertions: [
                noFirstNamesWithLetterT: { tds: TabularDataSet[1]|$tds->filter(row|$row.getString('firstName')->startsWith('T'))->meta::legend::service::validation::assertTabularDataSetEmpty('Expected no first names to begin with the letter T') },
                rowCountGreaterThan10: { tds: TabularDataSet[1]|$tds->groupBy([], agg('count', r:TDSRow[1]|$r, y|$y->count()))->filter(row|$row.getInteger('count') <= 10)->meta::legend::service::validation::assertTabularDataSetEmpty('Expected row count to be greater than 10') };
            ];
        }
    ]
}
```

A Post Validation Assertion must accept a parameter of the same Type and Multiplicity as the service execution query. The expression must end with one of the following:
- **meta::legend::service::validation::assertTabularDataSetEmpty('...assertion message...')** for queries that return a TabularDataSet
- **meta::legend::service::validation::assertCollectionEmpty('...assertion message...')** for queries that return a collection of an entity

`Parameters` must be specified for the Post Validation if the Service uses them. Parameters are evaluated and mapped 1-1 based on the index in which they appear in the service path and the `params` set. A parameter can be expressed as:
- A lambda function that returns a static value
- A lambda function that returns a relational result
  - The first column from the first row of the returned result will be used as the parameter

In the case of a multi-execution service, the execution key parameter will be evaluated in the same way as other parameters. The relevant execution will be used based on the evaluated key.

## Execution
To execute a Post Validation, a POST request can be made to **/service/v1/doValidation** providing appropriate PureModelContext, an assertion ID, and a serialization format for violations.

The response can take the form of one of the following:

A passing response:
```json
{
  "id": "noFirstNamesWithLetterT",
  "message": "Expected no first names to begin with the letter T",
  "result": "PASSED"
}
```

A failing response (in this case with PURE_TDSOBJECT serialization format):
```json
{
  "id": "noFirstNamesWithLetterT",
  "message": "Expected no first names to begin with the letter T",
  "result": "FAILED",
  "violations": [
    {
      "firstName": "Tom",
      "lastName": "Wilson",
      "age": 24
    }
  ]
}
```

## Example Services With Post Validations

### TabularDataSet Service
[Unit test](/legend-engine-service-post-validation-runner/src/test/java/org/finos/legend/engine/service/post/validation/runner/TestLegendServicePostValidationRunner.java#L89)
```
###Service
Service meta::validation::test::DemoService
{
    pattern: '/validation/demoService';
    owners: ['xyz', 'abc'];
    documentation: 'Some helpful docs';
    autoActivateUpdates: true;
    execution: Single
    {
        query: |meta::validation::test::Person.all()->project([col(p|$p.firstName, 'firstName'), col(p|$p.lastName, 'lastName'), col(p|$p.age, 'age')]);
        mapping: meta::validation::test::PersonMapping;
        runtime: meta::validation::test::TestRuntime;
    }
    postValidations:
    [
        {
            description: 'A good description of the validation';
            params: [];
            assertions: [
                noFirstNamesWithLetterT: { tds: TabularDataSet[1]|$tds->filter(row|$row.getString('firstName')->startsWith('T'))->meta::legend::service::validation::assertTabularDataSetEmpty('Expected no first names to begin with the letter T') },
                rowCountGreaterThan10: { tds: TabularDataSet[1]|$tds->groupBy([], agg('count', r:TDSRow[1]|$r, y|$y->count()))->filter(row|$row.getInteger('count') <= 10)->meta::legend::service::validation::assertTabularDataSetEmpty('Expected row count to be greater than 10') }
            ];
        }
    ]
}
```

### TabularDataSet Service With Lambda Static Parameter
[Unit test](/legend-engine-service-post-validation-runner/src/test/java/org/finos/legend/engine/service/post/validation/runner/TestLegendServicePostValidationRunner.java#L121)
```
###Service
Service meta::validation::test::DemoServiceWithParams
{
    pattern: '/validation/demoServiceWithParams/{minAge}';
    owners: ['xyz', 'abc'];
    documentation: 'Some helpful docs';
    autoActivateUpdates: true;
    execution: Single
    {
        query: minAge: Integer[1]|meta::validation::test::Person.all()->project([col(p|$p.firstName, 'firstName'), col(p|$p.lastName, 'lastName'), col(p|$p.age, 'age')])->filter(row|$row.getInteger('age') >= $minAge);
        mapping: meta::validation::test::PersonMapping;
        runtime: meta::validation::test::TestRuntime;
    }
    postValidations:
    [
        {
            description: 'A good description of the validation';
            params: [{|18}];
            assertions: [
                noFirstNamesWithLetterT: { tds: TabularDataSet[1]|$tds->filter(row|$row.getString('firstName')->startsWith('T'))->meta::legend::service::validation::assertTabularDataSetEmpty('Expected no first names to begin with the letter T') },
                rowCountGreaterThan10: { tds: TabularDataSet[1]|$tds->groupBy([], agg('count', r:TDSRow[1]|$r, y|$y->count()))->filter(row|$row.getInteger('count') <= 10)->meta::legend::service::validation::assertTabularDataSetEmpty('Expected row count to be greater than 10') }
            ];
        }
    ]
}
```

### TabularDataSet Service With Lambda Query Parameter
[Unit test](/legend-engine-service-post-validation-runner/src/test/java/org/finos/legend/engine/service/post/validation/runner/TestLegendServicePostValidationRunner.java#L129)
```
###Service
Service meta::validation::test::DemoServiceWithParams
{
    pattern: '/validation/demoServiceWithParams/{minAge}';
    owners: ['xyz', 'abc'];
    documentation: 'Some helpful docs';
    autoActivateUpdates: true;
    execution: Single
    {
        query: minAge: Integer[1]|meta::validation::test::Person.all()->project([col(p|$p.firstName, 'firstName'), col(p|$p.lastName, 'lastName'), col(p|$p.age, 'age')])->filter(row|$row.getInteger('age') >= $minAge);
        mapping: meta::validation::test::PersonMapping;
        runtime: meta::validation::test::TestRuntime;
    }
    postValidations:
    [
        {
            description: 'A good description of the validation';
            params: [{|meta::validation::test::Person.all()->project([col(p|$p.age, 'age')])->groupBy([], agg('min', r:TDSRow[1]|$r.getInteger('age'), y|$y->min()))}];
            assertions: [
                noFirstNamesWithLetterT: { tds: TabularDataSet[1]|$tds->filter(row|$row.getString('firstName')->startsWith('T'))->meta::legend::service::validation::assertTabularDataSetEmpty('Expected no first names to begin with the letter T') },
                rowCountGreaterThan10: { tds: TabularDataSet[1]|$tds->groupBy([], agg('count', r:TDSRow[1]|$r, y|$y->count()))->filter(row|$row.getInteger('count') <= 10)->meta::legend::service::validation::assertTabularDataSetEmpty('Expected row count to be greater than 10') }
            ];
        }
    ]
}
```

### TabularDataSet Multi Execution Service
[Unit test](/legend-engine-service-post-validation-runner/src/test/java/org/finos/legend/engine/service/post/validation/runner/TestLegendServicePostValidationRunner.java#L137)
```
###Service
Service meta::validation::test::DemoServiceWithParamsMultiExecution
{
    pattern: '/validation/demoServiceWithParamsMultiExecution/{minAge}/{executionKey}';
    owners: ['xyz', 'abc'];
    documentation: 'Some helpful docs';
    autoActivateUpdates: true;
    execution: Multi
    {
        query: minAge: Integer[1]|meta::validation::test::Person.all()->project([col(p|$p.firstName, 'firstName'), col(p|$p.lastName, 'lastName'), col(p|$p.age, 'age')])->filter(row|$row.getInteger('age') >= $minAge);
        key: 'executionKey';
        executions['first-key']: {
            mapping: meta::validation::test::PersonMapping;
            runtime: meta::validation::test::TestRuntime;
        }
        executions['second-key']: {
            mapping: meta::validation::test::PersonMapping;
            runtime: meta::validation::test::AnotherTestRuntime;
        }
    }
    postValidations:
    [
        {
            description: 'A good description of the validation';
            params: [18, 'first-key'];
            assertions: [
                noFirstNamesWithLetterT: { tds: TabularDataSet[1]|$tds->filter(row|$row.getString('firstName')->startsWith('T'))->meta::legend::service::validation::assertTabularDataSetEmpty('Expected no first names to begin with the letter T') },
                rowCountGreaterThan10: { tds: TabularDataSet[1]|$tds->groupBy([], agg('count', r:TDSRow[1]|$r, y|$y->count()))->filter(row|$row.getInteger('count') <= 10)->meta::legend::service::validation::assertTabularDataSetEmpty('Expected row count to be greater than 10') }
            ];
        }
    ]
}
```

### Object Service
[Unit test](/legend-engine-service-post-validation-runner/src/test/java/org/finos/legend/engine/service/post/validation/runner/TestLegendServicePostValidationRunner.java#L105)
```
Service meta::validation::test::DemoService
{
    pattern: '/validation/demoService';
    owners: ['xyz', 'abc'];
    documentation: 'Some helpful docs';
    autoActivateUpdates: true;
    execution: Single
    {
        query: |meta::validation::test::Person.all();
        mapping: meta::validation::test::PersonMapping;
        runtime: meta::validation::test::TestRuntime;
    }
    postValidations:
    [
        {
            description: 'A good description of the validation';
            params: [];
            assertions: [
                noFirstNamesWithLetterT: { people: meta::validation::test::Person[*]|$people->filter(p|$p.firstName->startsWith('T'))->meta::legend::service::validation::assertCollectionEmpty('Expected no first names to begin with the letter T') }
            ];
        }
    ]
}
```

## Frequently Asked Questions
**Are there any helper functions for common assertions?**  
Yes, further details to come

**How should I unit test my Post Validations?**  
Further details to come

**Can Post Validations be executed on embedded services?**  
Yes, further details to come