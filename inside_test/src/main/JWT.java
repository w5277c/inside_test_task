/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
11.12.2021  w5277c@gmail.com        Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package main;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import ru.w5277c.libs.JSON.JNumber;
import ru.w5277c.libs.JSON.JObject;
import ru.w5277c.libs.JSON.JString;

public class JWT {
   private  final static   String         ALGORITHM   = "HmacSHA256";
   private  final static   long           TTL         = 1 * 60 * 60 * 1000l;
   private  final static   SecureRandom   rnd         = new SecureRandom();
   private        static   SecretKeySpec  key         = null;
   private                 String         name;
   
   //Простейшая реализация JWT(использует HMAC+SHA256)
   public JWT(String name) {
      this.name = name;
   }

   public static boolean validate(String name, String token) throws Exception {
      boolean result = false;
      try {
         String pairs[] = token.split("\\.");
         JObject jheader = new JObject(JObject.str2stream(new String(Base64.getUrlDecoder().decode(pairs[0]), StandardCharsets.UTF_8)));
         JObject jpayload = new JObject(JObject.str2stream(new String(Base64.getUrlDecoder().decode(pairs[1]), StandardCharsets.UTF_8)));
         byte[] _hash = Base64.getUrlDecoder().decode(pairs[2]);
         String _alg = (jheader.contains("alg") ? jheader.get_string("alg") : null);
         String _typ = (jheader.contains("typ") ? jheader.get_string("typ") : null);
         String _name = (jpayload.contains("name") ? jpayload.get_string("name") : null);
         Long _exp = (jpayload.contains("exp") ? jpayload.get_long("exp") : null);
         
         if(null != _alg && _alg.equals("HS256") && null != _typ && _typ.equals("JWT") &&
            null != _name && _name.equals(name) && (System.currentTimeMillis()/1000) < _exp) {

            byte[] hash = get_hash(pairs[0], pairs[1]);
            result = Arrays.equals(_hash, hash);
         }
      }
      catch(Exception ex) {
         ex.printStackTrace();
      }
      return result;
   }
   
   private static synchronized byte[] get_hash(String header, String payload) throws NoSuchAlgorithmException, InvalidKeyException {
      if(null == key) {
         byte[] rnd_bytes = new byte[0x20];
         rnd.nextBytes(rnd_bytes);
         key = new SecretKeySpec(rnd_bytes, ALGORITHM);
      }

      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(key);
      mac.update(header.getBytes(StandardCharsets.UTF_8));
      mac.update(".".getBytes(StandardCharsets.UTF_8));
      mac.update(payload.getBytes(StandardCharsets.UTF_8));
      return mac.doFinal();
   }
   
   //Формируем три блока Base64, разделенные точкой. 
   public String getBearer() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
      JObject jheader = new JObject();
      jheader.add(new JString("alg", "HS256"));
      jheader.add(new JString("typ", "JWT"));
      byte[] header_bytes = jheader.toString().getBytes(StandardCharsets.UTF_8);
      
      JObject jpayload = new JObject();
      jpayload.add(new JString("name", name));
      jpayload.add(new JNumber("exp", (System.currentTimeMillis()+TTL)/1000));
      byte[] payload_bytes = jpayload.toString().getBytes(StandardCharsets.UTF_8);
      
      byte[] hash = get_hash(Base64.getUrlEncoder().withoutPadding().encodeToString(header_bytes),
                             Base64.getUrlEncoder().withoutPadding().encodeToString(payload_bytes));
      
      return   Base64.getUrlEncoder().withoutPadding().encodeToString(header_bytes) + "." +
               Base64.getUrlEncoder().withoutPadding().encodeToString(payload_bytes) + "." +
               Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
   }
   
   public String getName() {
      return name;
   }
   
}
