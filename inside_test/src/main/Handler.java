/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
12.12.2021  w5277c@gmail.com        Вынесен из main
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package main;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import ru.p5277.http.*;
import ru.p5277.http.headers.*;
import ru.w5277c.libs.JSON.*;
import ru.w5277c.libs.mysql.*;

public class Handler implements HTTPSHandler {
   private  final static   String               HISTORY10   = "history 10";
   private  final static   String               BEARER      = "Bearer";
   private  final          MySQL                db;
   
   //Обработчик запроса от HTTP сервера
   public Handler(MySQL db) {
      this.db = db;
   }

   //Реализация основного алгоритма
   @Override
   public void receive_http(HeaderRequest hReqv, HashMap<EHeaderName, Header> headers, HTTPSReceiver receiver) {
      HeaderResponse.ECode rc = HeaderResponse.ECode.UNKNOWN_ERROR;
      JObject result = null;
      try {
         //Поддерживаем только POST запрос
         if(HeaderRequest.EMethod.POST == hReqv.get_method()) {
            //Выделяем bearer, при наличии
            String token = null;
            Header authHeader = headers.get(EHeaderName.AUTHORIZATION);
            if(null != authHeader && authHeader.get_value().startsWith(BEARER)) {
               token = authHeader.get_value().substring(BEARER.length()).trim();
            }
            //Читаем длину контента 
            HeaderContentLength hcl = (HeaderContentLength)headers.get(EHeaderName.CONTENT_LENGTH);
            if(null != hcl && 0 != hcl.get_length()) {
               //Читаем тело, в нем должны быть name и (password или message) в формате JSON
               JObject jobj = new JObject(receiver.get_socket().getInputStream());
               String name = jobj.get_string("name");
               String password = (jobj.contains("password") ? jobj.get_string("password") : null);
               String message = (jobj.contains("message") ? jobj.get_string("message").trim() : null);
               if(null != name && !name.trim().isEmpty()) {
                  if(null != password) {
                     //Есть пароль - авторизируем и отдаем JWT
                     Select sel = db.select("SELECT `id` FROM `users` WHERE `name`=? AND hash=md5(?)");
                     if(sel.exec(name, password) && sel.next()) {
                        JObject _result = new JObject();
                        _result.add(new JString("token", new JWT(name).getBearer()));
                        result = _result;
                     }
                     else {
                        rc = HeaderResponse.ECode.UNAUTHORIZED;
                     }
                  }
                  else if(null != token && null != message) {
                     //Пароля нет, значит должен быть токен в хидере Authorization(bearer) и message в теле.
                     if(JWT.validate(name, token)) {
                        //JWT валидный
                        if(HISTORY10.equals(message)) {
                           //Получено сообщение 'history 10', возвращаем последние 10 сообщений
                           JObject jmsgs = new JArray("messages");
                           Select sel = db.select( "SELECT `message` FROM `messages` " +
                                                   "JOIN `users` ON `messages`.`user_id` = `users`.`id` " +
                                                   "WHERE `users`.`name`=? " +
                                                   "ORDER BY `messages`.`fd` DESC LIMIT 10");
                           if(sel.exec(name)) {
                              while(sel.next()) {
                                 jmsgs.add(new JString(null, sel.get_string(1)));
                              }
                           }
                           sel.close();
                           result = new JObject();
                           result.add(jmsgs);
                        }
                        else {
                           //Получено другое сообщение, записываем его в БД
                           Insert ins = db.insert( "INSERT INTO `messages` (`user_id`,`message`) VALUES(" +
                                                   "(SELECT `id` FROM `users` WHERE `name`=? LIMIT 1)" +
                                                   ",?)");
                           ins.exec(name, message);
                           ins.close();
                           result = new JObject();
                        }
                     }
                     else {
                        rc = HeaderResponse.ECode.UNAUTHORIZED;
                     }
                  }
                  else {
                     rc = HeaderResponse.ECode.BAD_REQUEST;
                  }
               }
               else {
                  rc = HeaderResponse.ECode.BAD_REQUEST;
               }
            }
            else {
               rc = HeaderResponse.ECode.BAD_REQUEST;
            }
         }
         else {
            rc = HeaderResponse.ECode.IAM_A_TEAPOT;
         }
      }
      catch(Exception ex) {
         rc = HeaderResponse.ECode.INTERNAL_SERVER_ERROR;
         ex.printStackTrace();
      }

      //Здесь отвечаем либо OK с JSON ответом в теле, либо ошибкой
      try {
         LinkedList<Header> respHeaders = new LinkedList<>();
         if(null != result) {
            byte[] content_bytes = result.toString().getBytes(StandardCharsets.UTF_8);
            respHeaders.add(new HeaderContentLength(content_bytes.length));
            respHeaders.add(new HeaderContentType(HeaderContentType.EType.APPLICATION_JSON));
            receiver.send_http(new HeaderResponse(HeaderResponse.ECode.OK), respHeaders);
            receiver.get_socket().getOutputStream().write(content_bytes);
            receiver.get_socket().getOutputStream().flush();
         }
         else {
            receiver.send_http(new HeaderResponse(rc), null);
         }
      }
      catch(Exception ex) {
         ex.printStackTrace();
      }
   }

   @Override
   public void receiver_started(int num, int quantity, String socketInfo) {
   }
   @Override
   public void receiver_finished(int num) {
   }
   @Override
   public void error(String log) {
   }
}
