import org.finos.legend.engine.Session;
import org.finos.legend.engine.pg.postgres.jdbc.JDBCSessionFactory;

import java.sql.*;

public class PostgresTestClient  {


    public static void main(String[] args) throws Exception{
       //Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "vika");
        Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:9998/postgres", "postgres", "vika");
        PreparedStatement statement = connection.prepareStatement("select * from  public.demo");
        ResultSet resultSet = statement.executeQuery();
        ResultSetMetaData metaData = resultSet.getMetaData();
    /*    for(int i =1; i <= metaData.getColumnCount();i++){
            System.out.println(metaData.getColumnName(i)+"  :  "+metaData.getColumnType(i));
        }*/
        while (resultSet.next()){
            String name = resultSet.getString("name");
            int age =   resultSet.getInt("age");
            System.out.println(name+" : "+age);
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
