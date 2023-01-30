// Copyright 2020 Goldman Sachs
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


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class PostgresTestClient
{


  public static void main(String[] args) throws Exception
  {
    //Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "vika");
    Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:9998/postgres",
        "postgres", "vika");
    PreparedStatement statement = connection.prepareStatement("select * from  public.demo");
    ResultSet resultSet = statement.executeQuery();
    ResultSetMetaData metaData = resultSet.getMetaData();
    /*    for(int i =1; i <= metaData.getColumnCount();i++){
            System.out.println(metaData.getColumnName(i)+"  :  "+metaData.getColumnType(i));
        }*/
    while (resultSet.next())
    {
      String name = resultSet.getString("name");
      int age = resultSet.getInt("age");
      System.out.println(name + " : " + age);
      //resultSet.getMetaData().getColumnCount()
    }
  }

/*    @Tes
    public void  test() throws Exception{
        //Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "vika");
        Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:9998/postgres", "postgres", "vika");
        PreparedStatement statement = connection.prepareStatement("select name from public.demo");
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()){
            String name = resultSet.getString("name");
            int age = 0;//  resultSet.getInt("age");
            System.out.println(name+": "+age);
        }
    }
    */
}
