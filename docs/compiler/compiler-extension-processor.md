# Defining Compiler Extension Processor

``` JAVA
public static <T extends PackageableElement> Processor<T> newProcessor(
   Class<T> elementClass,
   Collection<? extends Class<? extends PackageableElement>> prerequisiteClasses,
   BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
   BiConsumer<? super T, CompileContext> secondPass,
   BiConsumer<? super T, CompileContext> thirdPass,
   BiFunction<? super T, CompileContext, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement>> prerequisiteElementsPass)
```

## Element Class
Provide subclass of `PackageableElement` which is being compiled (e.g. `Database.class`).

## Prerequisite Classes
Provide a set of prerequisite classes which your element class depends on (e.g. `[Mapping.class, PackageableConnection.class]`).
The compiler sorts the dependencies based on these prerequisite classes and guarantees all elements in those classes are compiled before the elements in your class.

## First Pass
Define a function that performs the following operations:
* Create `Pure (M3)` objects and register them in Pure graph
* Set primitive values in the `Pure (M3)` objects
* **MUST NOT** reference other elements in the Pure graph

## Second Pass
Define a function that performs the following operations:
* Resolve content of its own element and references to other elements
* **MUST NOT** introspect content of other elements or check validity/correctness

## Third Pass
Define a function that performs the following operations:
* Resolve cross-dependencies
* Introspect other elements

## Prerequisite Elements Pass
Define a function that returns the prerequisite elements under the same element class that your element depends on.
The compiler sorts the dependencies based on these prerequisite elements and guarantees that those elements are compiled before your element.
For instance, `MappingA` depends on `MappingB` and `MappingC`, and therefore, `MappingB` and `MappingC` must be compiled first.

The compiler throws an `EngineException` if it finds circular dependencies in these elements. The following Pure grammar demonstrates an example of circular dependencies:

``` pure
###Relational
Database store::CovidDataStoreA
(
   include store::CovidDataStoreB
)

Database store::CovidDataStoreB
(
   include store::CovidDataStoreC
)

Database store::CovidDataStoreC
(
  include store::CovidDataStoreA

  Table DEMOGRAPHICS
  (
    FIPS VARCHAR(200),
    STATE VARCHAR(200)
  )
  Table COVID_DATA
  (
    ID INTEGER PRIMARY KEY,
    FIPS VARCHAR(200),
    DATE DATE,
    CASE_TYPE VARCHAR(200),
    CASES INTEGER,
    LAST_REPORTED_FLAG BIT
  )

  Join CovidDataDemographicsJoin(DEMOGRAPHICS.FIPS = COVID_DATA.FIPS)
)
```