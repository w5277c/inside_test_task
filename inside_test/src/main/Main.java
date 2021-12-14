/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
11.12.2021  w5277c@gmail.com        Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package main;

import java.sql.SQLException;
import ru.p5277.http.HTTPServer;
import ru.w5277c.libs.mysql.ConnectionString;
import ru.w5277c.libs.mysql.LogHandler;
import ru.w5277c.libs.mysql.MySQL;

public class Main {
   public static void main(String[] args) throws SQLException, Exception {
      //Подключаемся к БД
      MySQL db = new MySQL(new ConnectionString("5277.ru", 3306, "db", "db", "123321"), new LogHandler() {
         @Override
         public boolean is_panic() {
            return false;
         }
         @Override
         public boolean is_enabled() {
            return true;
         }
         @Override
         public void push(String logData) {
         }
         @Override
         public void error(Object obj) {
         }
      });
      //Задаем параметры подключения к БД
      db.set_property("characterEncoding","utf8");
      db.set_property("characterSetResults","utf8");
      db.set_property("connectionTimeout", "5");
      db.set_property("autoReconnect","true");
     
      //ЗАпускаем HTTP сервер
      HTTPServer server = new HTTPServer(8080, new Handler(db));
      server.start();
   }
}
