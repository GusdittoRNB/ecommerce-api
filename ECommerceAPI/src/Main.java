import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

public class Main {
    private static final int PORT = 003;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/users", new UsersHandler());
        server.createContext("/products", new ProductsHandler());
        server.createContext("/orders", new OrdersHandler());
        server.createContext("/reviews", new ReviewsHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port 003");
    }
}
