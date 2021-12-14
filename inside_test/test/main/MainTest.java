package main;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import ru.p5277.http.HTTPClient;
import ru.p5277.http.headers.EHeaderName;
import ru.p5277.http.headers.Header;
import ru.p5277.http.headers.HeaderContentLength;
import ru.p5277.http.headers.HeaderContentType;
import ru.p5277.http.headers.HeaderRequest;
import ru.p5277.http.headers.HeaderResponse;
import ru.w5277c.libs.JSON.JObject;
import ru.w5277c.libs.JSON.JString;

public class MainTest {
   
   public MainTest() {
   }
   
   @BeforeClass
   public static void setUpClass() {
   }
   
   @AfterClass
   public static void tearDownClass() {
   }
   
   @Before
   public void setUp() {
   }
   
   @After
   public void tearDown() {
   }

   @Test
   public void testMain() throws Exception {
      Main.main(null);

      assertTrue(check("kostas", "123321"));
   }
   
   public boolean check(String name, String password) throws Exception {
      boolean result = false;
      
      HTTPClient client = new HTTPClient("127.0.0.1", 8080, 15000);
      client.set_panic(false);
      client.connect();
      String token = auth(client, name, password);
      if(null != token) {
         boolean _result = true;
         for(int i=0; i<10; i++) {
            _result &= add_message(client, token, name, "message_" + Integer.toString(i+1));
         }
         if(_result) {
            LinkedList<String> messages = show_messages(client, token, name);
            if(null != messages) {
               System.out.println(messages);
               result = true;
            }
         }
      }
      
      return result;
   }
   
   public String auth(HTTPClient client, String name, String password) throws Exception {
      String result = null;
      
      LinkedList<Header> headers = new LinkedList<>();
      JObject jobj = new JObject();
      jobj.add(new JString("name", name));
      jobj.add(new JString("password", password));
      byte[] body_bytes = jobj.toString().getBytes(StandardCharsets.UTF_8);
      headers.add(new HeaderContentLength(body_bytes.length));
      headers.add(new HeaderContentType(HeaderContentType.EType.APPLICATION_JSON));
      client.send_http(new HeaderRequest(HeaderRequest.EMethod.POST, "/"), headers);
      client.get_socket().getOutputStream().write(body_bytes);
      client.get_socket().getOutputStream().flush();
      HashMap<EHeaderName, Header> response = client.recv_http();
      if(null != response) {
         HeaderResponse hr = (HeaderResponse)response.get(EHeaderName.HEADER_RESP);
         HeaderContentLength hcl = (HeaderContentLength)response.get(EHeaderName.CONTENT_LENGTH);
         if(null != hr && HeaderResponse.ECode.OK == hr.get_code() && null != hcl && 0 != hcl.get_length()) {
            jobj = new JObject(client.get_socket().getInputStream());
            result = jobj.get_string("token");
         }
      }
      return result;
   }
   
   public boolean add_message(HTTPClient client, String token, String name, String message) throws Exception {
      LinkedList<Header> headers = new LinkedList<>();
      headers.add(new Header(EHeaderName.AUTHORIZATION, "Bearer " + token));
      JObject jobj = new JObject();
      jobj.add(new JString("name", name));
      jobj.add(new JString("message", message));
      byte[] body_bytes = jobj.toString().getBytes(StandardCharsets.UTF_8);
      headers.add(new HeaderContentLength(body_bytes.length));
      headers.add(new HeaderContentType(HeaderContentType.EType.APPLICATION_JSON));
      client.send_http(new HeaderRequest(HeaderRequest.EMethod.POST, "/"), headers);
      client.get_socket().getOutputStream().write(body_bytes);
      client.get_socket().getOutputStream().flush();

      HashMap<EHeaderName, Header> response = client.recv_http();
      if(null != response) {
         HeaderResponse hr = (HeaderResponse)response.get(EHeaderName.HEADER_RESP);
         HeaderContentLength hcl = (HeaderContentLength)response.get(EHeaderName.CONTENT_LENGTH);
         if(null != hr && HeaderResponse.ECode.OK == hr.get_code()) {
            if(null != hcl && 0 != hcl.get_length()) {
               byte[] cotent = new byte[hcl.get_length().intValue()];
               client.get_socket().getInputStream().read(cotent);
               return true;
            }
         }
      }
      return false;
   }
   
   public LinkedList<String> show_messages(HTTPClient client, String token, String name) throws Exception {
      LinkedList<String> result = null;
      
      LinkedList<Header> headers = new LinkedList<>();
      headers.add(new Header(EHeaderName.AUTHORIZATION, "Bearer " + token));
      JObject jobj = new JObject();
      jobj.add(new JString("name", name));
      jobj.add(new JString("message", "history 10"));
      byte[] body_bytes = jobj.toString().getBytes(StandardCharsets.UTF_8);
      headers.add(new HeaderContentLength(body_bytes.length));
      headers.add(new HeaderContentType(HeaderContentType.EType.APPLICATION_JSON));
      client.send_http(new HeaderRequest(HeaderRequest.EMethod.POST, "/"), headers);
      client.get_socket().getOutputStream().write(body_bytes);
      client.get_socket().getOutputStream().flush();

      HashMap<EHeaderName, Header> response = client.recv_http();
      if(null != response) {
         HeaderResponse hr = (HeaderResponse)response.get(EHeaderName.HEADER_RESP);
         HeaderContentLength hcl = (HeaderContentLength)response.get(EHeaderName.CONTENT_LENGTH);
         if(null != hr && HeaderResponse.ECode.OK == hr.get_code() && null != hcl && 0 != hcl.get_length()) {
            jobj = new JObject(client.get_socket().getInputStream());
            result = new LinkedList<>();
            for(JObject _jobj : jobj.get_object("messages").get_items()) {
               result.add(_jobj.get_value());
            }
         }
      }
      return result;
   }
}
