import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Hashtable;

/**
 * Copyright @2017-07-03
 *
 */
public class fleet extends Thread {

    public static final String VERSION = "Fleet v0.1 @2017 Summer";
    public static final Hashtable<String,String> MIME_TYPES = new Hashtable<String,String>();
    
    static {
        String image = "image/";
        MIME_TYPES.put(".gif", image + "gif");
        MIME_TYPES.put(".jpg", image + "jpeg");
        MIME_TYPES.put(".jpeg", image + "jpeg");
        MIME_TYPES.put(".png", image + "png");
        // String text = "text/";
        MIME_TYPES.put(".html", "text/html");
        MIME_TYPES.put(".htm", "text/html");
        MIME_TYPES.put(".js", "text/js");
        MIME_TYPES.put(".css", "text/css");
        MIME_TYPES.put(".txt", "text/plain");
        MIME_TYPES.put(".md", "text/markdown");
        MIME_TYPES.put(".java", "text/java");
        MIME_TYPES.put(".c", "text/c");
        MIME_TYPES.put(".js", "text/js");

        //audio
        MIME_TYPES.put(".mp3", "audio/mp3");
        MIME_TYPES.put(".ogg", "audio/mp3");
        //video
        MIME_TYPES.put("mp4", "video/mp4");
    }
    
    public fleet(File rootDir, int port) throws IOException {
        _rootDir = rootDir.getCanonicalFile();
        if (!_rootDir.isDirectory()) {
            throw new IOException("Not a directory.");
        }
        _serverSocket = new ServerSocket(port);
        start();
    }
    
    public void run() {
        while (_running) {
            try {
                Socket socket = _serverSocket.accept();
                ServeThread serveThread = new ServeThread(socket, _rootDir);
                serveThread.start();
            }
            catch (IOException e) {
                System.exit(1);
            }
        }
    }
    
    public static String getExtension(java.io.File file) {
        String extension = "";
        String filename = file.getName();
        int dotPos = filename.lastIndexOf(".");
        if (dotPos >= 0) {
            extension = filename.substring(dotPos);
        }
        return extension.toLowerCase();
    }
    
    public static void main(String[] args) {
        try {
            System.out.println("Usage: java fleet [webroot] [port]\n");
        	String webroot = "./";
            if(args.length>0) webroot = args[0];
            int port= 80;
            if(args.length>1) port= Integer.parseInt(args[1]);
           /* fleet server = */new fleet(new File(webroot), port);
           
            System.out.println("http server running at:  http://localhost:"+port);
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }
    
    private File _rootDir;
    private ServerSocket _serverSocket;
    private boolean _running = true;

}


class ServeThread extends Thread {
    
    public ServeThread(Socket socket, File rootDir) {
        _socket = socket;
        _rootDir = rootDir;
    }
    
    private static void sendHeader(BufferedOutputStream out, int code, String contentType, long contentLength, long lastModified) throws IOException {
        out.write(("HTTP/1.0 " + code + " OK\r\n" + 
                   "Date: " + new Date().toString() + "\r\n" +
                   "Server: Fleet WebServer 0.1\r\n" +
                   "Content-Type: " + contentType + "\r\n" +
                   "Expires: \r\n" +
                   ((contentLength != -1) ? "Content-Length: " + contentLength + "\r\n" : "") +
                   "Last-modified: " + new Date(lastModified).toString() + "\r\n" +
                   "\r\n").getBytes());
    }
    
    private static void sendError(BufferedOutputStream out, int code, String message) throws IOException {
        message = message + "<hr>" + fleet.VERSION;
        sendHeader(out, code, "text/html", message.length(), System.currentTimeMillis());
        out.write(message.getBytes());
        out.flush();
        out.close();
    }
    
    public void run() {
        InputStream reader = null;
        try {
            _socket.setSoTimeout(30000);
            BufferedReader in = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
            BufferedOutputStream out = new BufferedOutputStream(_socket.getOutputStream());
            
            String request = in.readLine();
            if (request == null || !request.startsWith("GET ") || !(request.endsWith(" HTTP/1.0") || request.endsWith("HTTP/1.1"))) {
                
                sendError(out, 500, "Invalid Method.");
                return;
            }            
            String path = request.substring(4, request.length() - 9);            
            File file = new File(_rootDir, URLDecoder.decode(path, "UTF-8")).getCanonicalFile();
            
            if (file.isDirectory()) {
                // Check to see if there is an index file in the directory.
                File indexFile = new File(file, "index.html");
                if (indexFile.exists() && !indexFile.isDirectory()) {
                    file = indexFile;
                }
            }

            if (!file.toString().startsWith(_rootDir.toString())) {
                // it looks like some lamer is trying to take a peek
                // outside of our web root directory.
                sendError(out, 403, "Permission Denied.");
            }
            else if (!file.exists()) {
                sendError(out, 404, "File Not Found.");
            }
            else if (file.isDirectory()) {
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
                File[] files = file.listFiles();
                sendHeader(out, 200, "text/html", -1, System.currentTimeMillis());
                String title = "Index of " + path;
                out.write(("<html><head><title>" + title + "</title></head><body><h3>Index of " + path + "</h3><p>\n").getBytes());
                for (int i = 0; i < files.length; i++) {
                    file = files[i];
                    String filename = file.getName();
                    String description = "";
                    if (file.isDirectory()) {
                        description = "/";
                    }
                    out.write(("<a href=\"" + path + filename + "\">" + filename + "</a> " + description + "<br>\n").getBytes());
                }
                out.write(("</p><hr><p>" + fleet.VERSION + "</p></body><html>").getBytes());
            }
            else {
                reader = new BufferedInputStream(new FileInputStream(file));
            
                String contentType = (String)fleet.MIME_TYPES.get(fleet.getExtension(file));
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                
                sendHeader(out, 200, contentType, file.length(), file.lastModified());
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = reader.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                reader.close();
            }
            out.flush();
            out.close();
        }
        catch (IOException e) {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (Exception err) {
                    
                }
            }
        }
    }
    
    private File _rootDir;
    private Socket _socket;
    
}