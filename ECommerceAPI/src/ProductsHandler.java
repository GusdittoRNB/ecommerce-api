import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class ProductsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        switch (method) {
            case "GET":
                if (path.equals("/products")) {
                    handleGetAllProducts(exchange);
                } else if (path.startsWith("/products/")) {
                    int productId = Integer.parseInt(path.substring("/products/".length()));
                    handleGetProduct(exchange, productId);
                }
                break;
            case "POST":
                if (path.equals("/products")) {
                    handleCreateProduct(exchange);
                }
                break;
            case "PUT":
                if (path.startsWith("/products/")) {
                    int productId = Integer.parseInt(path.substring("/products/".length()));
                    handleUpdateProduct(exchange, productId);
                }
                break;
            case "DELETE":
                if (path.startsWith("/products/")) {
                    int productId = Integer.parseInt(path.substring("/products/".length()));
                    handleDeleteProduct(exchange, productId);
                }
                break;
        }
    }

}