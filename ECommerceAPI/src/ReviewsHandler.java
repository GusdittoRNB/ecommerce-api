import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class ReviewsHandler implements HttpHandler {
    private String getRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response.getBytes());
        outputStream.close();
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
                if (path.equals("/reviews")) {
                    handleGetAllReviews(exchange);
                } else if (path.startsWith("/reviews/order")) {
                    handleGetReviewsByOrderId(exchange, path);
                }
                break;
            case "POST":
                if (path.startsWith("/reviews/order")) {
                    handleCreateReview(exchange, path);
                }
                break;
            case "DELETE":
                if (path.startsWith("/reviews/order")) {
                    handleDeleteReview(exchange, path);
                }
                break;
        }
    }

    private void handleGetAllReviews(HttpExchange exchange) throws IOException {
        //            if (!validateApiKey(exchange)) {
//                sendErrorResponse(exchange, 401, "Unauthorized");
//                return;
//            }

        // Mendapatkan nilai query params "type" dari URL
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> queryParams = parseQueryParams(query);
        String star = queryParams.get("star");

        if (star != null) {
            // Menampilkan pengguna berdasarkan tipe (type)
            try (Connection connection = DatabaseConnection.connect();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM reviews WHERE star = ?")) {

                statement.setString(1, star);
                ResultSet resultSet = statement.executeQuery();

                JSONArray reviewsArray = new JSONArray();
                while (resultSet.next()) {
                    JSONObject reviewObject = new JSONObject();
                    reviewObject.put("order_id", resultSet.getInt("order_id"));
                    reviewObject.put("star", resultSet.getInt("star"));
                    reviewObject.put("description", resultSet.getString("description"));
                    reviewsArray.put(reviewObject);
                }

                JSONObject response = new JSONObject();
                response.put("reviews", reviewsArray);

                sendResponse(exchange, 200, response.toString());
            } catch (SQLException e) {
                e.printStackTrace();
                sendErrorResponse(exchange, 500, "Internal Server Error");
            }
        } else {
            // Menampilkan semua pengguna
            try (Connection connection = DatabaseConnection.connect();
                 Statement statement = connection.createStatement()) {

                ResultSet resultSet = statement.executeQuery("SELECT * FROM reviews");

                JSONArray reviewsArray = new JSONArray();
                while (resultSet.next()) {
                    JSONObject reviewObject = new JSONObject();
                    reviewObject.put("order_id", resultSet.getInt("order_id"));
                    reviewObject.put("star", resultSet.getInt("star"));
                    reviewObject.put("description", resultSet.getString("description"));
                    reviewsArray.put(reviewObject);
                }

                JSONObject response = new JSONObject();
                response.put("orders", reviewsArray);

                sendResponse(exchange, 200, response.toString());
            } catch (SQLException e) {
                e.printStackTrace();
                sendErrorResponse(exchange, 500, "Internal Server Error");
            }
        }
    }

    private void handleGetReviewsByOrderId(HttpExchange exchange, String path) throws IOException {
        String orderId = path.substring(path.lastIndexOf('/') + 1);

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM reviews WHERE order_id = ?")) {

            statement.setString(1, orderId);
            ResultSet resultSet = statement.executeQuery();

            JSONArray reviewsArray = new JSONArray();
            while (resultSet.next()) {
                JSONObject reviewObject = new JSONObject();
                reviewObject.put("order_id", resultSet.getInt("order_id"));
                reviewObject.put("star", resultSet.getInt("star"));
                reviewObject.put("description", resultSet.getString("description"));
                reviewsArray.put(reviewObject);
            }

            JSONObject response = new JSONObject();
            response.put("reviews", reviewsArray);

            sendResponse(exchange, 200, response.toString());
        } catch (SQLException | JSONException e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error");
        }
    }

    private void handleCreateReview(HttpExchange exchange, String path) throws IOException {
        String orderId = path.substring(path.lastIndexOf('/') + 1);

        String requestBody = getRequestBody(exchange);
        try {
            JSONObject reviewObject = new JSONObject(requestBody);
            int star = reviewObject.getInt("star");
            String description = reviewObject.getString("description");

            try (Connection connection = DatabaseConnection.connect();
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO reviews (order_id, star, description) VALUES (?, ?, ?)")) {

                statement.setString(1, orderId);
                statement.setInt(2, star);
                statement.setString(3, description);
                statement.executeUpdate();

                JSONObject responseObj = new JSONObject();
                responseObj.put("message", "Review created successfully");
                sendResponse(exchange, 201, responseObj.toString());
            } catch (SQLException e) {
                e.printStackTrace();
                sendErrorResponse(exchange, 500, "Internal Server Error");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 400, "Invalid request body");
        }
    }

    private void handleDeleteReview(HttpExchange exchange, String path) throws IOException {
        String orderId = path.substring(path.lastIndexOf('/') + 1);

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM reviews WHERE order_id = ?")) {

            statement.setString(1, orderId);
            int rowsDeleted = statement.executeUpdate();

            if (rowsDeleted > 0) {
                JSONObject responseObj = new JSONObject();
                responseObj.put("message", "Review deleted successfully");
                sendResponse(exchange, 200, responseObj.toString());
            } else {
                sendErrorResponse(exchange, 404, "Review not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error");
        }
    }


}
