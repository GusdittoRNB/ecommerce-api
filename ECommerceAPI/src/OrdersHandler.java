import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.sql.*;
import java.util.Map;
import java.util.HashMap;

public class OrdersHandler implements HttpHandler {
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
                if (path.equals("/orders")) {
                    handleGetAllOrders(exchange);
                } else if (path.startsWith("/orders/")) {
                    handleGetOrder(exchange);
                }
                break;
            case "POST":
                if (path.equals("/orders")) {
                    handleCreateOrder(exchange);
                }
                break;
            case "DELETE":
                if (path.startsWith("/orders/")) {
                    int orderId = Integer.parseInt(path.substring("/orders/".length()));
                    handleDeleteOrder(exchange, orderId);
                }
                break;
        }
    }

    private void handleGetAllOrders(HttpExchange exchange) throws IOException {
        //            if (!validateApiKey(exchange)) {
//                sendErrorResponse(exchange, 401, "Unauthorized");
//                return;
//            }

        // Mendapatkan nilai query params "type" dari URL
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> queryParams = parseQueryParams(query);
        String isPaid = queryParams.get("is_paid");

        if (isPaid != null) {
            // Menampilkan pengguna berdasarkan tipe (type)
            try (Connection connection = DatabaseConnection.connect();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM orders WHERE is_paid = ?")) {

                statement.setString(1, isPaid);
                ResultSet resultSet = statement.executeQuery();

                JSONArray ordersArray = new JSONArray();
                while (resultSet.next()) {
                    JSONObject orderObject = new JSONObject();
                    orderObject.put("order_id", resultSet.getInt("order_id"));
                    orderObject.put("buyer", resultSet.getString("buyer"));
                    orderObject.put("note", resultSet.getString("note"));
                    orderObject.put("total", resultSet.getString("total"));
                    orderObject.put("discount", resultSet.getString("discount"));
                    orderObject.put("is_paid", resultSet.getString("is_paid"));
                    ordersArray.put(orderObject);
                }

                JSONObject response = new JSONObject();
                response.put("orders", ordersArray);

                sendResponse(exchange, 200, response.toString());
            } catch (SQLException e) {
                e.printStackTrace();
                sendErrorResponse(exchange, 500, "Internal Server Error");
            }
        } else {
            // Menampilkan semua pengguna
            try (Connection connection = DatabaseConnection.connect();
                 Statement statement = connection.createStatement()) {

                ResultSet resultSet = statement.executeQuery("SELECT * FROM orders");

                JSONArray ordersArray = new JSONArray();
                while (resultSet.next()) {
                    JSONObject orderObject = new JSONObject();
                    orderObject.put("order_id", resultSet.getInt("order_id"));
                    orderObject.put("buyer", resultSet.getString("buyer"));
                    orderObject.put("note", resultSet.getString("note"));
                    orderObject.put("total", resultSet.getString("total"));
                    orderObject.put("discount", resultSet.getString("discount"));
                    orderObject.put("is_paid", resultSet.getString("is_paid"));
                    ordersArray.put(orderObject);
                }

                JSONObject response = new JSONObject();
                response.put("orders", ordersArray);

                sendResponse(exchange, 200, response.toString());
            } catch (SQLException e) {
                e.printStackTrace();
                sendErrorResponse(exchange, 500, "Internal Server Error");
            }
        }
    }

    private void handleGetOrder(HttpExchange exchange) throws IOException {
        String orderIdString = extractOrderId(exchange.getRequestURI().getPath());
        if (orderIdString != null) {
            try {
                int orderId = Integer.parseInt(orderIdString);
                try (Connection connection = DatabaseConnection.connect()) {
                    JSONObject order = getOrderById(connection, orderId);
                    if (order != null) {
                        sendResponse(exchange, 200, order.toString());
                    } else {
                        sendErrorResponse(exchange, 404, "Order not found");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    sendErrorResponse(exchange, 500, "Internal Server Error");
                }
            } catch (NumberFormatException e) {
                sendErrorResponse(exchange, 400, "Invalid order ID");
            }
        } else {
            sendErrorResponse(exchange, 400, "Invalid request URL");
        }
    }

    private JSONObject getOrderById(Connection connection, int orderId) throws SQLException {
        String query = "SELECT o.order_id, o.buyer, o.discount, o.is_paid, o.total, od.product_id, od.quantity, od.price " +
                "FROM orders o " +
                "JOIN order_details od ON o.order_id = od.order_id " +
                "WHERE o.order_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                JSONObject order = null;
                JSONArray orderDetails = new JSONArray();
                while (resultSet.next()) {
                    if (order == null) {
                        order = new JSONObject();
                        order.put("order_id", resultSet.getInt("order_id"));
                        order.put("buyer", resultSet.getInt("buyer"));
                        order.put("discount", resultSet.getDouble("discount"));
                        order.put("is_paid", resultSet.getString("is_paid"));
                        order.put("total", resultSet.getInt("total"));
                    }
                    JSONObject orderDetail = new JSONObject();
                    orderDetail.put("product_id", resultSet.getInt("product_id"));
                    orderDetail.put("quantity", resultSet.getInt("quantity"));
                    orderDetail.put("price", resultSet.getInt("price"));
                    orderDetails.put(orderDetail);
                }
                if (order != null) {
                    order.put("order_details", orderDetails);
                }
                return order;
            }
        }
    }

    private String extractOrderId(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 2) {
            return parts[parts.length - 1];
        } else {
            return null;
        }
    }

    private void handleCreateOrder(HttpExchange exchange) throws IOException {
        String requestBody = getRequestData(exchange);
        try {
            JSONObject orderObject = new JSONObject(requestBody);

            // Get database connection
            try (Connection connection = DatabaseConnection.connect()) {
                // Call the method to handle order creation
                handleCreateOrder(connection, orderObject);

                // Create the response object
                JSONObject response = new JSONObject();
                response.put("message", "Order created successfully");

                sendResponse(exchange, 201, response.toString());
            } catch (SQLException e) {
                e.printStackTrace();
                sendErrorResponse(exchange, 500, "Internal Server Error");
            }
        } catch (JSONException e) {
            sendErrorResponse(exchange, 400, "Invalid request body");
        }
    }

    private void handleCreateOrder(Connection connection, JSONObject requestBody) throws SQLException {
        int customerId = requestBody.getInt("buyer");
        String note = requestBody.getString("note");
        int discount = requestBody.getInt("discount");
        String isPaid = requestBody.getString("is_paid");
        JSONArray orderItems = requestBody.getJSONArray("order_items");

        // Insert order data into orders table
        String insertOrderQuery = "INSERT INTO orders (buyer, note, total, discount, is_paid) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement orderStatement = connection.prepareStatement(insertOrderQuery, Statement.RETURN_GENERATED_KEYS)) {
            orderStatement.setInt(1, customerId);
            orderStatement.setString(2, note);
            orderStatement.setInt(3, 0); // Placeholder for total, will be updated later
            orderStatement.setInt(4, discount);
            orderStatement.setString(5, isPaid);
            orderStatement.executeUpdate();

            ResultSet generatedKeys = orderStatement.getGeneratedKeys();
            if (!generatedKeys.next()) {
                throw new SQLException("Failed to create order");
            }

            int orderId = generatedKeys.getInt(1);

            // Insert order details data into order_details table
            String insertOrderDetailsQuery = "INSERT INTO order_details (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
            String updateOrderTotalQuery = "UPDATE orders SET total = ? WHERE order_id = ?";
            try (PreparedStatement orderDetailsStatement = connection.prepareStatement(insertOrderDetailsQuery);
                 PreparedStatement updateOrderTotalStatement = connection.prepareStatement(updateOrderTotalQuery)) {
                int total = 0;


                // Process each order item
                for (int i = 0; i < orderItems.length(); i++) {
                    JSONObject orderItem = orderItems.getJSONObject(i);
                    int productId = orderItem.getInt("product_id");
                    int quantity = orderItem.getInt("quantity");

                    // Fetch price from products table
                    String selectProductQuery = "SELECT price FROM products WHERE product_id = ?";
                    try (PreparedStatement selectProductStatement = connection.prepareStatement(selectProductQuery)) {
                        selectProductStatement.setInt(1, productId);
                        ResultSet productResult = selectProductStatement.executeQuery();

                        if (productResult.next()) {
                            int price = productResult.getInt("price");
                            int subtotal = (quantity * price) - discount;

                            // Update order details table
                            orderDetailsStatement.setInt(1, orderId);
                            orderDetailsStatement.setInt(2, productId);
                            orderDetailsStatement.setInt(3, quantity);
                            orderDetailsStatement.setInt(4, price);
                            orderDetailsStatement.executeUpdate();

                            // Calculate total
                            total += subtotal;
                        }
                    }
                }

                // Update total in orders table
                updateOrderTotalStatement.setInt(1, total);
                updateOrderTotalStatement.setInt(2, orderId);
                updateOrderTotalStatement.executeUpdate();
            }
        }
    }

    private void handleDeleteOrder(HttpExchange exchange, int productId) throws IOException {
        try (Connection connection = getConnection()){
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM orders WHERE order_id = ?")) {
                statement.setInt(1, productId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM order_details WHERE order_id = ?")) {
                statement.setInt(1, productId);
                int affectedRows = statement.executeUpdate();
                if (affectedRows > 0) {
                    JSONObject responseObj = new JSONObject();
                    responseObj.put("message", "Order deleted successfully");
                    sendResponse(exchange, 200, responseObj.toString());
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error");
        }

        sendErrorResponse(exchange, 404, "Order not found");
    }

}
