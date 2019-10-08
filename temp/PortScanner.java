import java.net.*;

class PortScanner {
   public static void main(String[] args) {
      String host = "pan.baidu.com";
      if (args.length > 0)
         host = args[0];
      for (int port = 1; port <= 113; port++) {
         try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 1000);
            socket.close();
            System.out.println("Port " + port + " is open");
         } catch (Exception ex) {
            System.out.print(port + " ");
         }
      }
   }
}