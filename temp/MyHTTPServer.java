import java.io.*;
import java.net.*;
import com.sun.net.httpserver.*;

public class MyHTTPServer {

   public static void main(String[] args) throws Exception {
      HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
      server.createContext("/hello", new HelloHandler());
      server.setExecutor(null);
      server.start();
      System.out.println("Server is listening on port 8080");
      System.out.println("http://localhost:8080/hello");

   }

   static class HelloHandler implements HttpHandler {
      public void handle(HttpExchange t) throws IOException {
         String response = "Hello from MyHTTPServer+++";
         t.sendResponseHeaders(200, response.length());
         OutputStream os = t.getResponseBody();
         os.write(response.getBytes());
         os.close();
      }
   }
}

/*
 * Output: $ java MyHTTPServer & 
 * Server is listening on port 8080
 * 
 * 
 * $ wget http://localhost:8080/hello -q -O - 
 * Hello from MyHTTPServer.....
 */