// Copyright 2021 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection.test.bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.junit.Assert.assertEquals;

public class TestByteBuddy
{
    @Test
    public void testInterceptMethod() throws Exception
    {
        ByteBuddyAgent.install();

        Class1 class1Object1 = new Class1();
        class1Object1.foo();
        assertEquals(1, class1Object1.getIntField1());

        Class<?> type = new ByteBuddy()
                .redefine(Class1.class)
                .visit(Advice.to(Class1Advice.class).on(named("foo")))
                .make()
                .load(Class1.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent())
                .getLoaded();

        class1Object1.foo();
        assertEquals(102, class1Object1.getIntField1());
    }

    public static class Class1
    {
        int intField1 = 0;
        String stringField1 = "foo";
        List<String> listField1 = new ArrayList<>();

        public void foo()
        {
            this.intField1 += 1;
        }

        public int getIntField1()
        {
            return intField1;
        }

        public String getStringField1()
        {
            return stringField1;
        }

        public List<String> getListField1()
        {
            return listField1;
        }
    }

    static class Class1Advice
    {
        @Advice.OnMethodEnter
        private static void enter(@Advice.This Class1 thisObject)
        {
            System.out.println("+ Entering method :");
            thisObject.intField1 += 100;
            System.out.println(thisObject.intField1);
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static void exit()
        {
            System.out.println("+ Exiting method :");
        }
    }
}
