import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class ProductsHandler implements HttpHandler {
    private static final String API_KEY;

    static {
        Dotenv dotenv = Dotenv.configure().directory(".env").load();
        API_KEY = dotenv.get("API_KEY");
    }

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

    private static boolean validateApiKey(HttpExchange exchange) {
        String apiKey = exchange.getRequestHeaders().getFirst("x-api-key");
        return apiKey != null && apiKey.equals(API_KEY);
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

    private void handleGetAllProducts(HttpExchange exchange) throws IOException {
        if (!validateApiKey(exchange)) {
            sendErrorResponse(exchange, 401, "Unauthorized");
            return;
        }
        try {
            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
            String filterField = queryParams.get("field");
            String filterOperator = queryParams.get("cond");
            String filterValue = queryParams.get("val");

            String query = "SELECT * FROM products";
            if (filterField != null && filterOperator != null && filterValue != null) {
                query += " WHERE " + filterField + " " + getFilterOperator(filterOperator) + " ?";
            }

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {

                if (filterField != null && filterOperator != null && filterValue != null) {
                    statement.setString(1, filterValue);
                }

                ResultSet resultSet = statement.executeQuery();

                JSONArray jsonArray = new JSONArray();
                while (resultSet.next()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("product_id", resultSet.getInt("product_id"));
                    jsonObject.put("seller", resultSet.getString("seller"));
                    jsonObject.put("title", resultSet.getString("title"));
                    jsonObject.put("description", resultSet.getString("description"));
                    jsonObject.put("price", resultSet.getString("price"));
                    jsonObject.put("stock", resultSet.getString("stock"));
                    jsonArray.put(jsonObject);
                }

                sendResponse(exchange, 200, jsonArray.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error");
        }
    }

    private String getFilterOperator(String condition) {
        String operator = "";
        switch (condition) {
            case "equal":
                operator = "=";
                break;
            case "notEqual":
                operator = "<>";
                break;
            case "lessThan":
                operator = "<";
                break;
            case "lessEqual":
                operator = "<=";
                break;
            case "greaterThan":
                operator = ">";
                break;
            case "greaterEqual":
                operator = ">=";
                break;
        }
        return operator;
    }

    private void handleGetProduct(HttpExchange exchange, int productId) throws IOException {
        if (!validateApiKey(exchange)) {
            sendErrorResponse(exchange, 401, "Unauthorized");
            return;
        }
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM products WHERE product_id = ?")) {

            statement.setInt(1, productId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("product_id", resultSet.getInt("product_id"));
                jsonObject.put("seller", resultSet.getString("seller"));
                jsonObject.put("title", resultSet.getString("title"));
                jsonObject.put("description", resultSet.getString("description"));
                jsonObject.put("price", resultSet.getString("price"));
                jsonObject.put("stock", resultSet.getString("stock"));

                sendResponse(exchange, 200, jsonObject.toString());
            } else {
                sendErrorResponse(exchange, 404, "Product not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error");
        }
    }

    private void handleCreateProduct(HttpExchange exchange) throws IOException {
        if (!validateApiKey(exchange)) {
            sendErrorResponse(exchange, 401, "Unauthorized");
            return;
        }
        String requestBody = getRequestData(exchange);
        try {
            JSONObject productObject = new JSONObject(requestBody);
            String seller = productObject.getString("seller");
            String title = productObject.getString("title");
            String description = productObject.getString("description");
            String price = productObject.getString("price");
            String stock = productObject.getString("stock");

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO products (seller, title, description, price, stock) VALUES (?, ?, ?, ?, ?)",
                         Statement.RETURN_GENERATED_KEYS)) {

                statement.setString(1, seller);
                statement.setString(2, title);
                statement.setString(3, description);
                statement.setString(4, price);
                statement.setString(5, stock);
                statement.executeUpdate();

                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int productId = generatedKeys.getInt(1);
                    JSONObject response = new JSONObject();
                    response.put("message", "Product created successfully");
                    response.put("product_id", productId);
                    sendResponse(exchange, 201, response.toString());
                } else {
                    sendErrorResponse(exchange, 500, "Failed to create product");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 400, "Invalid request body");
        }
    }

    private void handleUpdateProduct(HttpExchange exchange, int productId) throws IOException {
        if (!validateApiKey(exchange)) {
            sendErrorResponse(exchange, 401, "Unauthorized");
            return;
        }
        String requestBody = getRequestData(exchange);
        try {
            JSONObject productObject = new JSONObject(requestBody);
            String seller = productObject.getString("seller");
            String title = productObject.getString("title");
            String description = productObject.getString("description");
            String price = productObject.getString("price");
            String stock = productObject.getString("stock");

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE products SET seller =?, title = ?, description = ?, price = ?, stock = ? WHERE product_id = ?")) {

                statement.setString(1, seller);
                statement.setString(2, title);
                statement.setString(3, description);
                statement.setString(4, price);
                statement.setString(5, stock);
                statement.setInt(6, productId);
                int rowsAffected = statement.executeUpdate();

                if (rowsAffected > 0) {
                    JSONObject responseObj = new JSONObject();
                    responseObj.put("message", "Product updated successfully");
                    sendResponse(exchange, 200, responseObj.toString());
                } else {
                    sendErrorResponse(exchange, 404, "Product not found");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 400, "Invalid request body");
        }
    }

    private void handleDeleteProduct(HttpExchange exchange, int productId) throws IOException {
        if (!validateApiKey(exchange)) {
            sendErrorResponse(exchange, 401, "Unauthorized");
            return;
        }
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM products WHERE product_id = ?")) {

            statement.setInt(1, productId);
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                JSONObject responseObj = new JSONObject();
                responseObj.put("message", "Product deleted successfully");
                sendResponse(exchange, 200, responseObj.toString());
            } else {
                sendErrorResponse(exchange, 404, "Product not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error");
        }
    }

}