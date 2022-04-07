# Java Language Support

## Creating classes, enums, etc.

There are a large set of factory methods to create Pure representations of each of the main concepts in Java.

For example to create a class there are the following:

``` pure
function meta::external::language::java::factory::javaClass(fullClassName:String[1]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(modifiers:String[*], fullClassName:String[1]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(modifiers:String[*], proto:meta::external::language::java::metamodel::Class[1]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(modifiers:String[*], typeParams:meta::external::language::java::metamodel::TypeVariable[*], proto:meta::external::language::java::metamodel::Class[1]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(modifiers:String[*], typeParams:meta::external::language::java::metamodel::TypeVariable[*], fullClassName:String[1]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(pkg:meta::external::language::java::metamodel::Package[1], name:String[1]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(modifiers:String[*], pkg:meta::external::language::java::metamodel::Package[1], name:String[1]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(modifiers:String[*], typeParams:meta::external::language::java::metamodel::TypeVariable[*], pkg:meta::external::language::java::metamodel::Package[1], name:String[1]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(proto:meta::external::language::java::metamodel::Class[1], methods:Pair<meta::external::language::java::metamodel::Method,Code>[*]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(proto:meta::external::language::java::metamodel::Class[1], fields:meta::external::language::java::metamodel::Field[*], methods:Pair<meta::external::language::java::metamodel::Method,Code>[*]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(modifiers:String[*], proto:meta::external::language::java::metamodel::Class[1], methods:Pair<meta::external::language::java::metamodel::Method,Code>[*]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(modifiers:String[*], proto:meta::external::language::java::metamodel::Class[1], fields:meta::external::language::java::metamodel::Field[*], methods:Pair<meta::external::language::java::metamodel::Method,Code>[*]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(modifiers:String[*], fullClassName:String[1], methods:Pair<meta::external::language::java::metamodel::Method,Code>[*]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(modifiers:String[*], fullClassName:String[1], fields:meta::external::language::java::metamodel::Field[*], methods:Pair<meta::external::language::java::metamodel::Method,Code>[*]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(modifiers:String[*], typeParams:meta::external::language::java::metamodel::TypeVariable[*], fullClassName:String[1], methods:Pair<meta::external::language::java::metamodel::Method,Code>[*]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(modifiers:String[*], typeParams:meta::external::language::java::metamodel::TypeVariable[*], fullClassName:String[1], fields:meta::external::language::java::metamodel::Field[*], methods:Pair<meta::external::language::java::metamodel::Method,Code>[*]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(modifiers:String[*], typeParams:meta::external::language::java::metamodel::TypeVariable[*], pkg:meta::external::language::java::metamodel::Package[1], name:String[1], methods:Pair<meta::external::language::java::metamodel::Method,Code>[*]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(modifiers:String[*], typeParams:meta::external::language::java::metamodel::TypeVariable[*], pkg:meta::external::language::java::metamodel::Package[1], name:String[1], fields:meta::external::language::java::metamodel::Field[*], methods:Pair<meta::external::language::java::metamodel::Method,Code>[*]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(typeParams:meta::external::language::java::metamodel::TypeVariable[*], proto:meta::external::language::java::metamodel::Class[1], fields:meta::external::language::java::metamodel::Field[*], methods:Pair<meta::external::language::java::metamodel::Method,Code>[*]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(proto:meta::external::language::java::metamodel::Class[1], fields:meta::external::language::java::metamodel::Field[*], constructors:Pair<meta::external::language::java::metamodel::Constructor,Code>[*], methods:Pair<meta::external::language::java::metamodel::Method,Code>[*]):meta::external::language::java::metamodel::Class[1]
function meta::external::language::java::factory::javaClass(typeParams:meta::external::language::java::metamodel::TypeVariable[*], proto:meta::external::language::java::metamodel::Class[1], fields:meta::external::language::java::metamodel::Field[*], constructors:Pair<meta::external::language::java::metamodel::Constructor,Code>[*], methods:Pair<meta::external::language::java::metamodel::Method,Code>[*]):meta::external::language::java::metamodel::Class[1]
```

It is generally easier to use the simpler versions and then to use further factory calls to add details:

``` pure
let interface = javaClass(['public', 'interface'], 'org.finos.example.api.Greeter')
                  ->addMethod(javaMethod(javaVoid(), 'sayHello', []))
                  ->inferImports();

let greeting = javaField('private', javaString(), 'greeting');
let impl     = javaClass('public', 'org.finos.example.impl.MyGreeter')
                  ->implements($interface)
                  ->addField($greeting)
                  ->addConstructor({cls:meta::external::language::java::metamodel::Class[1]|
                    let param = j_parameter(javaString(), 'greeting');
                     
                    javaConstructor('public', $param, j_this($cls)->j_field($greeting)->j_assign($param)); 
                  })
                  ->addMethod({cls|
                    let message = j_variable(javaString(), 'message');

                    javaMethod('public', javaVoid(), 'sayHello', [], 
                      [
                        $message->j_declare(j_this($cls)->j_field($greeting)->j_plus(j_string(', World'))),
                        j_println($message)
                      ]
                    );
                  })
                  ->inferImports();
```

This code demonstrates some general principles:
* Modifiers are given as Strings.  They are verified by the factory methods.
* the `addXxx` methods exist in two forms.  One takes the object to be added directly.  The other uses a lambda
  which provides the class as so-far constructed.
* Imports can be inferred at the end of construction to provide the most readable code.
  It should be done as the last factory call for the class.
  The factory method `->imports(...)` is deprecated in favour of this approach.
* There are two sets of factories.  Some begin with `java` and are used to create classes, methods, fields, types, etc.
  Others begin with `j_` and are used to create code.
  Parameters are created using `j_parameter` so that they can be referred to in code as variables.

The above produces the following Java:

``` java
package org.finos.example.api;

public interface Greeter
{
    void sayHello();
}
```

``` java
package org.finos.example.impl;

import java.io.PrintStream;
import org.finos.example.api.Greeter;

public class MyGreeter implements Greeter
{
    private String greeting;

    public MyGreeter(String greeting)
    {
        this.greeting = greeting;
    }

    public void sayHello()
    {
        String message = this.greeting + ", World";
        System.out.println(message);
    }
}
```

### Don't use j_code

There is a factory `j_code` that allows textual code representation.  **This is deprecated.**  It supports some
legacy code generation and it should not be used for new code generation.  It should be removed once possible.

### Primitives and their boxed and literal forms

The following are defined:

| Java     | primitive factory | boxed factory     | literal factory |
|---------|-------------------|--------------------|-----------------|
| boolean | javaBoolean()     | javaBooleanBoxed() | j_boolean(true) |
| byte    | javaByte()        | javaByteBoxed()    | j_byte(10)      |
| char    | javaChar()        | javaCharBoxed()    | j_char('A')     |
| short   | javaShort()       | javaShortBoxed()   | j_short(0)      |
| int     | javaInt()         | javaIntBoxed()     | j_int(0)        |
| long    | javaLong()        | javaLongBoxed()    | j_long(0)       |
| float   | javaFloat()       | javaFloatBoxed()   | j_float(0.0)    |
| double  | javaDouble()      | javaDoubleBoxed()  | j_double(0.0)   |

Also available is `$type->toBoxed()` which will generate the boxed type if `$type` is a primitive.
Otherwise it retrurns `$type`.
`$type->toUnboxed()` does the reverse. 

### j_block

Many factory methods take a `Code[*]` parameter to allow multiple statements. 
For example `j_if($condition, [...], [...])`.  It's also possible to reduce a `Code[*]` to a `Code[1]` using `j_block([...])`.

### Stream operations

It's possible to take code and produce a Stream for it.  There are a set of `js_xxx` factories for working with streams.
It can then be resolved back to a desired type.

The following:
``` pure
let param  = j_parameter(javaList(javaString()), 'words');
let s      = j_parameter(javaString(), 's');
let lambda = j_lambda($s, $s->j_invoke('startsWith', j_string('Z')));
let code   = $param->j_streamOf()->js_filter($lambda)->js_resolve(javaList(javaString()));
```
produces for `$code` in Java:
``` java
words.stream().filter((String s) -> s.startsWith("Z")).collect(java.util.stream.Collectors.toList())
```

## Projects

Classes can be contained in projects and this is the preferred unit of transfer between functions implementing
significant portions of a generation algorithm.

Projects are similarly created using factory methods:

``` pure
newProject()->addClasses([$interface, $impl]);
```

Projects can be merged:
``` pure
mergeProjects([
  newProject()->addClass($interface),
  newProject()->addClass($impl)
]);
```

When merging projects classes will be brought together into a single project so the above examples are equivalent.
It is also possible that the same class exists in both projects.
In this circumstance the class is merged.  The result will contain the superset of fields and methods.
If the same field exists in both it must have the same type and initialization.
If the same method exists in both it must have the same implementation.
Together these features allow separate aspects of a class to be generated independently and then combined. 

_The merging algorithm can be made smarter.  For example merging a field with an initializer and one without could
result in the initialized field.  Similarly for methods one without an implementation and one with can result
in an implemented method.  For both annotations should be merged but are currently not._

## Serialization

Classes are serialized using:

``` pure
$interface->classToString()
```

It's also sometimes helpful to serialize parts of Java, particularly for debugging.  These will produce
verbose output as the set if imports is not known:

``` pure
javaString()->typeToString()->println();
javaString().package->packageToString()->println();
javaEnum('public', 'org.finos.Side')->addEntries([javaEnumEntry('Buy'), javaEnumEntry('Sell')])->enumToString()->println();
j_variable(javaInt(), 'i')-j_declare(j_int(0))->codeToString()->println();
```

## Dependencies

It's possible to associate dependencies with `$code->dependsOn($dependency)`.

A dependency is formed of a name and a function from a `CodeDependencyResolutionState` to a `CodeDependencyResolutionState`.  

``` pure
let name = 'an-identity';
let dep = newDependency(
  $name,
  {state |
    let project = generateSomeMoreCodeInAProject();
    $state->addDependencyProject($name, $project);
  }
);  
```

This mechanism is used when a piece of code requires other code (depended-on code) to be present.
The mechanism defers generating that depended-on code until later.  
It's possible that multiple pieces of code will require the same depended-on code.
The name should uniquely identify the depended-upon code.  
That is, if multiple pieces of code require the same depended-on code they should have dependencies with the same name.
Deferring generation of the depended-on code allows it to be generated only once, after all code expressing the dependencies have been generated.

Dependencies are resolved to projects at any time using `$codes->dependencies()->resolveAndGetProjects()`.
The resulting projects can be merged with the project containing the originating codes.

_It might be useful to add a function to resolve all dependencies in a project and return the merged result._

# Java generation from Pure

Java generation is heavily used in plan generation. 
This takes advantage of higher-level APIs to make generation easier and consistent across a code base.

## Conventions

`Conventions` is a configuration object used to control naming and type usage for generated Java code.
The conventions used in plan generation are obtained using `meta::pure::executionPlan::engine::java::engineConventions()`.
Conventions are used to provide interpretations of Pure metamodel instances such as:

``` pure
let fieldName        = $conventions->fieldName($property);
let getterName       = $conventions->getterName($property);
let javaTypeForTrade = $conventions->pureTypeToJavaType(Trade, PureOne);
let javaTypeIntegers = $conventions->integerType(ZeroMany);
```

## Expression Generation

Pure expressions can be generated into Java.  For example the following:

``` pure
let conventions = meta::pure::executionPlan::engine::java::engineConventions([]);
let func        = {|test::getTrades()->filter(t| $t.side == test::Side.BUY).quantity->max()};
let expression  = $func.expressionSequence->at(0)->evaluateAndDeactivate()->generateJava($conventions);
```
produces for `$expression` in Java:

``` java
_pure.functions.Functions__test.getTrades__Trade_MANY_()
                               .stream()
                               .filter((_pure.app.test.Trade t) -> t.getSide()
                                                                    .equals(_pure.app.test.Side.BUY))
                               .map((_pure.app.test.Trade v_automap) -> v_automap.getQuantity())
                               .reduce(Long::max)
                               .orElse(null)
```

The function `_pure.functions.Functions__test.getTrades__Trade_MANY_` is a dependency of `$expression` and
its implementation can be realised by resolving dependencies as above.

In order to perform this type of translation it's necessary to include libraries in the conventions that
perform the implementation of any native Pure functions that can be used.  It's also possible to provide
implementations of non-native functions if you want to generate a different solution than the translation
of the function's definition in Pure would provide.

The `engineConventions` include such a set of libraries that implement functions that work within execution
plans and which can leverage Java classes in engine (module `legend-engine-executionPlan-dependencies`).
For example in the above example `max()` is implemented as follows:

``` pure
         fc1(max_Integer_MANY__Integer_$0_1$_,                         {ctx,nums       | $nums->j_streamOf()->js_reduce(javaLongBoxed()->j_methodReference('max'))}),
```

This is a short-hand definition (longer definition are available for more complex functions).
`fc1` defines a function code to a one-parameter function.
The lambda parameters represent a `FuncCoderContext`, which is helpful in some cases, followed by the parameters
translated into Java (that is a `Code[1]` generated from the parameter).
Its result ios the implementation of the function.  In this case it takes the parameter (`$nums`), obtains a Stream and 
reduces the Stream using `Long::max` (since in engine Pure Integers are represented as Java longs).

If the type of the code resulting from the function coder is not the type expected from the Pure function it is 
cast/resolved.  In this case the Stream reduce results in a Java `Optional` so the `.orElse(null)` is automatically
added to give a `Long` which agrees with the `Integer[0..1]` result of `max`.
