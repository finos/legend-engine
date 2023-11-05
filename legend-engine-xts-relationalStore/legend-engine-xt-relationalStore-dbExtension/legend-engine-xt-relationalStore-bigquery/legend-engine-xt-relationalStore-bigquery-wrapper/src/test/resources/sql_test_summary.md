Legend SQL Compatibility Report
===============================

Overview
--------
This is a summary report of SQL tests for databases supported by Legend.

The report summarizes the output of each database's Junit integration test results.

The 'Test Name' column refers to a Pure test function.



Cheat sheet
-----------
| Test Status             | Emoji                             | Description                                                                                                                                                                      |
| ----------------------- | --------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Success                 | :green_circle:                    | Test passed                                                                                                                                                                      |
| Error                   | :red_circle:                      | Test failed                                                                                                                                                                      |
| Unsupported             | :black_circle:                    | Legend feature has not been implemented for this database as yet. (The feature very well might be supported in the target database)                                              |
| Behavior Deviation      | :diamond_shape_with_a_dot_inside: | Deviation from standard behavior. (TODO : The semantics of this status are not clear and needs to be refined.)                                                                   |
| Missing                 | :purple_circle:                   | Test result not available. Most likely because of a systemic failure or omission (e.g Github workflow failed or we have not included the database in the testing framework etc.) |
| Report Generation Error | :confused:                        | An error/bug in the generation of this report                                                                                                                                    |

Test Statistics By Database
---------------------------
| Database            | Success | Error | Unsupported | Behavior Deviation | Missing | Report Generation Error | Total |
| ------------------- | ------- | ----- | ----------- | ------------------ | ------- | ----------------------- | ----- |
| BigQuery (Deloitte) | 95      | 61    | 38          | 0                  | 0       | 0                       | 194   |
| BigQuery (Simba)    | 95      | 61    | 38          | 0                  | 0       | 0                       | 194   |

Test Details By Database
------------------------
| Test Name                                                                    | BigQuery (Deloitte) | BigQuery (Simba) |
| ---------------------------------------------------------------------------- | ------------------- | ---------------- |
| selectSubClauses::filters::testLessThanEqualString                           | :red_circle:        | :red_circle:     |
| selectSubClauses::windowColumn::testWindowWithoutSortMultiple                | :red_circle:        | :red_circle:     |
| dynaFunctions::substring::testCustomStartIndex                               | :green_circle:      | :green_circle:   |
| dynaFunctions::endsWith::testNotEndsWith                                     | :green_circle:      | :green_circle:   |
| dynaFunctions::abs::testPositive                                             | :green_circle:      | :green_circle:   |
| dynaFunctions::firstDayOfWeek::testFirstDayOfWeek                            | :black_circle:      | :black_circle:   |
| dynaFunctions::substring::testEndIndexExceedingStringLength                  | :green_circle:      | :green_circle:   |
| namingTests::testTableNameInJoinAlias                                        | :red_circle:        | :red_circle:     |
| selectSubClauses::filters::testFilterOnAssociatedClassAggValueAfterGroupBy   | :red_circle:        | :red_circle:     |
| selectSubClauses::aggregationDynaFns::max::testMax                           | :red_circle:        | :red_circle:     |
| dynaFunctions::lessThan::testNumbers                                         | :green_circle:      | :green_circle:   |
| dynaFunctions::greaterThanEqual::testNumbers                                 | :green_circle:      | :green_circle:   |
| dynaFunctions::round::testDecimalUpper                                       | :green_circle:      | :green_circle:   |
| dynaFunctions::length::testWithString                                        | :black_circle:      | :black_circle:   |
| dynaFunctions::abs::testZero                                                 | :green_circle:      | :green_circle:   |
| dynaFunctions::ceiling::testDecimal                                          | :green_circle:      | :green_circle:   |
| dynaFunctions::dateDiff::testDatePartAsString                                | :black_circle:      | :black_circle:   |
| dynaFunctions::dateDiff::testYearsDiffNegative                               | :black_circle:      | :black_circle:   |
| dynaFunctions::coalesce::testCoalesce                                        | :green_circle:      | :green_circle:   |
| dynaFunctions::indexOf::testChar                                             | :black_circle:      | :black_circle:   |
| dynaFunctions::quarterNumber::testQuarterNumber                              | :green_circle:      | :green_circle:   |
| selectSubClauses::union::testConcatenateWithPostOperation                    | :red_circle:        | :red_circle:     |
| dynaFunctions::replace::testCharReplace                                      | :green_circle:      | :green_circle:   |
| selectSubClauses::view::testViewOnView                                       | :red_circle:        | :red_circle:     |
| dynaFunctions::isNotEmpty::testSqlNull                                       | :green_circle:      | :green_circle:   |
| dynaFunctions::sqlTrue::testSqlTrue                                          | :red_circle:        | :red_circle:     |
| selectSubClauses::join::testSelfJoinInner                                    | :red_circle:        | :red_circle:     |
| dynaFunctions::acos::testDecimal                                             | :green_circle:      | :green_circle:   |
| dynaFunctions::ceiling::testIntAsFloat                                       | :green_circle:      | :green_circle:   |
| dynaFunctions::toTimestamp::testToTimestamp                                  | :black_circle:      | :black_circle:   |
| dynaFunctions::dateDiff::testMinutesDiff                                     | :black_circle:      | :black_circle:   |
| dynaFunctions::hour::testHour                                                | :green_circle:      | :green_circle:   |
| dynaFunctions::trim::testNoSpace                                             | :green_circle:      | :green_circle:   |
| dynaFunctions::rtrim::testTrailingSpace                                      | :green_circle:      | :green_circle:   |
| dynaFunctions::contains::testNoSpace                                         | :green_circle:      | :green_circle:   |
| dynaFunctions::sqlFalse::testSqlFalse                                        | :red_circle:        | :red_circle:     |
| dynaFunctions::log::testDecimal                                              | :green_circle:      | :green_circle:   |
| dynaFunctions::atan2::testDecimal                                            | :green_circle:      | :green_circle:   |
| dynaFunctions::firstDayOfThisYear::testFirstDayOfThisYear                    | :black_circle:      | :black_circle:   |
| dynaFunctions::minute::testminute                                            | :green_circle:      | :green_circle:   |
| dynaFunctions::dayOfYear::testDayOfYear                                      | :black_circle:      | :black_circle:   |
| dynaFunctions::cos::testDecimal1                                             | :green_circle:      | :green_circle:   |
| dynaFunctions::ltrim::testStartingSpace                                      | :green_circle:      | :green_circle:   |
| namingTests::setUp                                                           | :black_circle:      | :black_circle:   |
| dynaFunctions::firstDayOfThisQuarter::testFirstDayOfThisQuarter              | :black_circle:      | :black_circle:   |
| dynaFunctions::firstDayOfQuarter::testFirstDayOfQuarter                      | :black_circle:      | :black_circle:   |
| dynaFunctions::firstDayOfQuarter::testFirstDayOfQuarterWithDateLiteral       | :black_circle:      | :black_circle:   |
| selectSubClauses::filters::testSelectEqual                                   | :red_circle:        | :red_circle:     |
| selectSubClauses::filters::testGreaterThanEqualString                        | :red_circle:        | :red_circle:     |
| dynaFunctions::firstDayOfThisMonth::testFirstDayOfThisMonth                  | :black_circle:      | :black_circle:   |
| dynaFunctions::acos::testDecimal1                                            | :green_circle:      | :green_circle:   |
| dynaFunctions::substring::testCustomEndIndex                                 | :green_circle:      | :green_circle:   |
| dynaFunctions::endsWith::testEndsWith                                        | :green_circle:      | :green_circle:   |
| dynaFunctions::notEqualAnsi::testNumberInequality                            | :green_circle:      | :green_circle:   |
| selectSubClauses::windowColumn::windowWithSortMultiple                       | :red_circle:        | :red_circle:     |
| dynaFunctions::trim::testSpace                                               | :green_circle:      | :green_circle:   |
| dynaFunctions::matches::testMatchesNonAlphaNumeric                           | :black_circle:      | :black_circle:   |
| dynaFunctions::round::testDecimalLower                                       | :green_circle:      | :green_circle:   |
| selectSubClauses::join::testSelfJoinOuter                                    | :red_circle:        | :red_circle:     |
| dynaFunctions::toLower::test1                                                | :green_circle:      | :green_circle:   |
| dynaFunctions::firstDayOfYear::testFirstDayOfYear                            | :black_circle:      | :black_circle:   |
| dynaFunctions::adjust::testAdjustWithStringUnit                              | :green_circle:      | :green_circle:   |
| dynaFunctions::sin::testDecimal                                              | :green_circle:      | :green_circle:   |
| dynaFunctions::isNull::testSqlNull                                           | :green_circle:      | :green_circle:   |
| dynaFunctions::floor::testInt                                                | :green_circle:      | :green_circle:   |
| selectSubClauses::windowColumn::testWindowWithSortSingle                     | :red_circle:        | :red_circle:     |
| dynaFunctions::adjust::testAdjustWithMicroseconds                            | :green_circle:      | :green_circle:   |
| namingTests::testColumnNames                                                 | :red_circle:        | :red_circle:     |
| selectSubClauses::aggregationDynaFns::min::testMin                           | :red_circle:        | :red_circle:     |
| dynaFunctions::ceiling::testInt                                              | :green_circle:      | :green_circle:   |
| selectSubClauses::aggregationDynaFns::count::testOnAllRows                   | :red_circle:        | :red_circle:     |
| dynaFunctions::floor::testDecimal                                            | :green_circle:      | :green_circle:   |
| dynaFunctions::log::testDecimal1                                             | :green_circle:      | :green_circle:   |
| selectSubClauses::orderBy::testDoubleSortMixedChain                          | :red_circle:        | :red_circle:     |
| dynaFunctions::concat::testSpaceBeforeSecondString                           | :green_circle:      | :green_circle:   |
| dynaFunctions::asin::testDecimal1                                            | :green_circle:      | :green_circle:   |
| dynaFunctions::adjust::testAdjust                                            | :green_circle:      | :green_circle:   |
| dynaFunctions::dayOfMonth::testDayOfMonth                                    | :black_circle:      | :black_circle:   |
| dynaFunctions::right::testSpace                                              | :green_circle:      | :green_circle:   |
| dynaFunctions::pow::testDecimal                                              | :green_circle:      | :green_circle:   |
| namingTests::testAsciiColumnNameAndAsciiAliasName                            | :red_circle:        | :red_circle:     |
| dynaFunctions::abs::testDecimal                                              | :green_circle:      | :green_circle:   |
| selectSubClauses::orderBy::testSimpleSortAsc                                 | :red_circle:        | :red_circle:     |
| dynaFunctions::joinStrings::testNoSpace                                      | :red_circle:        | :red_circle:     |
| dynaFunctions::left::testSpace                                               | :black_circle:      | :black_circle:   |
| dynaFunctions::sin::testDecimal1                                             | :green_circle:      | :green_circle:   |
| selectSubClauses::setUp                                                      | :black_circle:      | :black_circle:   |
| selectSubClauses::join::testJoinBySingleColumnName                           | :red_circle:        | :red_circle:     |
| dynaFunctions::mod::testIntNonDivisible                                      | :green_circle:      | :green_circle:   |
| dynaFunctions::quarter::testQuarterAsNumber                                  | :green_circle:      | :green_circle:   |
| dynaFunctions::atan::testDecimal                                             | :green_circle:      | :green_circle:   |
| dynaFunctions::tan::testDecimal                                              | :green_circle:      | :green_circle:   |
| dynaFunctions::contains::testSpaceFalse                                      | :green_circle:      | :green_circle:   |
| dynaFunctions::sqlNull::testIsNull                                           | :green_circle:      | :green_circle:   |
| selectSubClauses::filters::testSelectNotEqual                                | :red_circle:        | :red_circle:     |
| selectSubClauses::orderBy::testDoubleSortAsc                                 | :red_circle:        | :red_circle:     |
| selectSubClauses::orderBy::testDoubleSortMixed                               | :red_circle:        | :red_circle:     |
| selectSubClauses::drop::testSimpleDrop                                       | :red_circle:        | :red_circle:     |
| namingTests::testASCiiAliasName                                              | :red_circle:        | :red_circle:     |
| selectSubClauses::orderBy::testDoubleSortDesc                                | :red_circle:        | :red_circle:     |
| dynaFunctions::contains::testSpace                                           | :green_circle:      | :green_circle:   |
| selectSubClauses::filters::testFilterAfterFilter                             | :black_circle:      | :black_circle:   |
| dynaFunctions::indexOf::testString                                           | :black_circle:      | :black_circle:   |
| dynaFunctions::monthNumber::testMonthNumber                                  | :green_circle:      | :green_circle:   |
| selectSubClauses::aggregationDynaFns::sum::testSum                           | :red_circle:        | :red_circle:     |
| dynaFunctions::month::testMonthAsNumber                                      | :green_circle:      | :green_circle:   |
| dynaFunctions::substring::testCustomStartIndexOnly                           | :green_circle:      | :green_circle:   |
| selectSubClauses::aggregationDynaFns::average::testAverage                   | :red_circle:        | :red_circle:     |
| selectSubClauses::limit::testSimpleLimit                                     | :red_circle:        | :red_circle:     |
| selectSubClauses::mixtureOfCluases::testGroupByAfterJoinInner                | :red_circle:        | :red_circle:     |
| namingTests::testASCiiColumnName                                             | :red_circle:        | :red_circle:     |
| selectSubClauses::union::testConcatenateWithPreOperation                     | :red_circle:        | :red_circle:     |
| dynaFunctions::lessThanEqual::testNumbers                                    | :green_circle:      | :green_circle:   |
| dynaFunctions::startsWith::testNotStartsWith                                 | :green_circle:      | :green_circle:   |
| dynaFunctions::dateDiff::testYearsDiffPositive                               | :black_circle:      | :black_circle:   |
| literals::string::testApostrophe                                             | :green_circle:      | :green_circle:   |
| dynaFunctions::in::testIn                                                    | :green_circle:      | :green_circle:   |
| dynaFunctions::abs::testNegative                                             | :green_circle:      | :green_circle:   |
| selectSubClauses::view::testUnionOnViewsMapping                              | :red_circle:        | :red_circle:     |
| dynaFunctions::cos::testDecimal                                              | :green_circle:      | :green_circle:   |
| dynaFunctions::startsWith::testEndsWithSpace                                 | :green_circle:      | :green_circle:   |
| selectSubClauses::groupBy::simpleGroupByCount                                | :red_circle:        | :red_circle:     |
| selectSubClauses::union::testSimpleConcatenate                               | :red_circle:        | :red_circle:     |
| selectSubClauses::windowColumn::windowAvg                                    | :red_circle:        | :red_circle:     |
| dynaFunctions::floor::testIntAsFloat                                         | :green_circle:      | :green_circle:   |
| dynaFunctions::rtrim::testNoSpace                                            | :green_circle:      | :green_circle:   |
| selectSubClauses::mixtureOfCluases::testFilterAfterJoinOuter                 | :red_circle:        | :red_circle:     |
| dynaFunctions::concat::testNoSpace                                           | :green_circle:      | :green_circle:   |
| dynaFunctions::startsWith::testStartsWith                                    | :green_circle:      | :green_circle:   |
| dynaFunctions::left::testNoSpace                                             | :black_circle:      | :black_circle:   |
| selectSubClauses::orderBy::testSimpleSortDesc                                | :red_circle:        | :red_circle:     |
| dynaFunctions::previousDayOfWeek::testPreviousDayOfWeekWithDate              | :black_circle:      | :black_circle:   |
| selectSubClauses::view::testViewAllOneSimpleProperty                         | :red_circle:        | :red_circle:     |
| dynaFunctions::datePart::testDatePartWithStrictDate                          | :black_circle:      | :black_circle:   |
| dynaFunctions::isNotNull::testSqlNull                                        | :green_circle:      | :green_circle:   |
| selectSubClauses::aggregationDynaFns::size::testSize                         | :red_circle:        | :red_circle:     |
| dynaFunctions::second::testSecond                                            | :green_circle:      | :green_circle:   |
| dynaFunctions::castBoolean::testCastBoolean                                  | :black_circle:      | :black_circle:   |
| selectSubClauses::slice::testSliceWithRestrict                               | :red_circle:        | :red_circle:     |
| dynaFunctions::dateDiff::testYearsDiffZero                                   | :black_circle:      | :black_circle:   |
| selectSubClauses::drop::testDropLimitByVendor                                | :red_circle:        | :red_circle:     |
| selectSubClauses::join::testJoinByMultiColumnName                            | :red_circle:        | :red_circle:     |
| dynaFunctions::now::testNow                                                  | :green_circle:      | :green_circle:   |
| dynaFunctions::matches::testMatchesAlphaNumeric                              | :black_circle:      | :black_circle:   |
| selectSubClauses::join::testInnerJoinSimple                                  | :red_circle:        | :red_circle:     |
| literals::varPlaceHolder::testApostropheInParameterValue                     | :red_circle:        | :red_circle:     |
| dynaFunctions::round::testInt                                                | :green_circle:      | :green_circle:   |
| dynaFunctions::ltrim::testNoSpace                                            | :green_circle:      | :green_circle:   |
| dynaFunctions::dateDiff::testMilliSecondsDiff                                | :black_circle:      | :black_circle:   |
| dynaFunctions::year::testYear                                                | :black_circle:      | :black_circle:   |
| selectSubClauses::aggregationDynaFns::stdDevSample::testStdDevSample         | :red_circle:        | :red_circle:     |
| selectSubClauses::orderBy::testDoubleSortDescChain                           | :red_circle:        | :red_circle:     |
| dynaFunctions::replace::testStringReplace                                    | :green_circle:      | :green_circle:   |
| dynaFunctions::exp::testDecimal                                              | :green_circle:      | :green_circle:   |
| dynaFunctions::startsWith::testStartsWithSpace                               | :green_circle:      | :green_circle:   |
| dynaFunctions::pow::testDecimal1                                             | :green_circle:      | :green_circle:   |
| dynaFunctions::and::testAndwithConditions                                    | :green_circle:      | :green_circle:   |
| dynaFunctions::dayOfWeekNumber::testDayOfWeekNumber                          | :black_circle:      | :black_circle:   |
| dynaFunctions::equal::testNumberEquality                                     | :green_circle:      | :green_circle:   |
| selectSubClauses::join::testLeftOuterJoinSimple                              | :red_circle:        | :red_circle:     |
| selectSubClauses::take::testSimpleTake                                       | :red_circle:        | :red_circle:     |
| dynaFunctions::sqrt::testInt1                                                | :green_circle:      | :green_circle:   |
| dynaFunctions::sqrt::testInt2                                                | :green_circle:      | :green_circle:   |
| dynaFunctions::atan2::testDecimal1                                           | :green_circle:      | :green_circle:   |
| selectSubClauses::mixtureOfCluases::testFilterAfterJoinInner                 | :red_circle:        | :red_circle:     |
| dynaFunctions::atan::testDecimal1                                            | :green_circle:      | :green_circle:   |
| dynaFunctions::rem::testIntNonDivisible                                      | :green_circle:      | :green_circle:   |
| dynaFunctions::tan::testDecimal1                                             | :green_circle:      | :green_circle:   |
| dynaFunctions::firstDayOfMonth::testFirstDayOfMonth                          | :black_circle:      | :black_circle:   |
| dynaFunctions::toUpper::test1                                                | :green_circle:      | :green_circle:   |
| dynaFunctions::isEmpty::testSqlNull                                          | :green_circle:      | :green_circle:   |
| dynaFunctions::today::testToday                                              | :green_circle:      | :green_circle:   |
| selectSubClauses::mixtureOfCluases::groupByAfterConcatenate                  | :red_circle:        | :red_circle:     |
| dynaFunctions::notEqual::testNumberInequality                                | :green_circle:      | :green_circle:   |
| dynaFunctions::or::testOrWithConditions                                      | :green_circle:      | :green_circle:   |
| dynaFunctions::right::testNoSpace                                            | :green_circle:      | :green_circle:   |
| selectSubClauses::mixtureOfCluases::testGroupByAfterJoinOuter                | :red_circle:        | :red_circle:     |
| dynaFunctions::asin::testDecimal                                             | :green_circle:      | :green_circle:   |
| dynaFunctions::dateDiff::testSecondsDiff                                     | :black_circle:      | :black_circle:   |
| selectSubClauses::slice::testSlice                                           | :red_circle:        | :red_circle:     |
| dynaFunctions::weekOfYear::testWeekOfYear                                    | :red_circle:        | :red_circle:     |
| selectSubClauses::filters::testFilterMultipleExpressions                     | :red_circle:        | :red_circle:     |
| selectSubClauses::join::testRightOuterJoinSimple                             | :red_circle:        | :red_circle:     |
| dynaFunctions::dateDiff::testDaysDiff                                        | :black_circle:      | :black_circle:   |
| dynaFunctions::dateDiff::testHoursDiff                                       | :black_circle:      | :black_circle:   |
| dynaFunctions::exp::testDecimal1                                             | :green_circle:      | :green_circle:   |
| dynaFunctions::rem::testIntDivisible                                         | :green_circle:      | :green_circle:   |
| dynaFunctions::mod::testIntDivisible                                         | :green_circle:      | :green_circle:   |
| dynaFunctions::mostRecentDayOfWeek::testMostRecentDayOfWeekWithDate          | :black_circle:      | :black_circle:   |
| dynaFunctions::greaterThan::testNumbers                                      | :green_circle:      | :green_circle:   |
| selectSubClauses::aggregationDynaFns::stdDevPopulation::testStdDevPopulation | :red_circle:        | :red_circle:     |
| dynaFunctions::datePart::testDatePartWithDateTime                            | :black_circle:      | :black_circle:   |
| dynaFunctions::dateDiff::testMonthsDiff                                      | :black_circle:      | :black_circle:   |
| selectSubClauses::orderBy::testDoubleSortAscChain                            | :red_circle:        | :red_circle:     |
