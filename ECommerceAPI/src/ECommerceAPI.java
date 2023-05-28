import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

public class ECommerceAPI {
    private static final int PORT = 003;

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/users", new UserHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("Server started on port 00" + PORT);
        } catch (Exception e) {
            System.out.println("Error starting server: " + e.getMessage());
        }
    }
}
