import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ProductsHandler implements HttpHandler {
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.connect();
    }

    private String getRequestData(HttpExchange exchange) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        StringBuilder requestData = new StringBuilder();
        int character;
        while ((character = requestBody.read()) != -1) {
            requestData.append((char) character);
        }
        return requestData.toString();
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream output = exchange.getResponseBody();
        output.write(response.getBytes());
        output.flush();
        output.close();
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String errorMessage) throws IOException {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("error", errorMessage);
        sendResponse(exchange, statusCode, errorResponse.toString());
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null) {
            String[] keyValuePairs = query.split("&");
            for (String pair : keyValuePairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }


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