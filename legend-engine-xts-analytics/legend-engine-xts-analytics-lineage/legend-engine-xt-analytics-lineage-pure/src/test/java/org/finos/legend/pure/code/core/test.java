/*
 * //  Copyright 2023 Goldman Sachs
 * //
 * //  Licensed under the Apache License, Version 2.0 (the "License");
 * //  you may not use this file except in compliance with the License.
 * //  You may obtain a copy of the License at
 * //
 * //       http://www.apache.org/licenses/LICENSE-2.0
 * //
 * //  Unless required by applicable law or agreed to in writing, software
 * //  distributed under the License is distributed on an "AS IS" BASIS,
 * //  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * //  See the License for the specific language governing permissions and
 * //  limitations under the License.
 */

package org.finos.legend.pure.code.core;

public class test
{

  public static void main(String[] args)
    {// Repeat the test multiple times for better accuracy
        int numIterations = 2;

        // Test using split method
        long startTime = System.nanoTime();
        for (int i = 0; i < numIterations; i++)
        {
            System.out.println( splitMethodTest("Hello Worldrb"));
        }
        long endTime = System.nanoTime();
        long splitMethodTime = endTime - startTime;

        // Print the results
        System.out.println("Split method time: " + splitMethodTime / 1000000.0 + " milliseconds");
    }

    private static org.eclipse.collections.api.RichIterable<String> splitMethodTest(String input) {
        // Split the string by dot
        //String[] parts = input.split("\\."); // 143.796394 milliseconds
        return org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport.split(input, "or"); // 143.796394 milliseconds


}
    private static String[] splitMethodTest2(String input) {
        // Split the string by dot
        //String[] parts = input.split("\\."); // 143.796394 milliseconds
        return input.split( "or"); // 143.796394 milliseconds


    }

}

