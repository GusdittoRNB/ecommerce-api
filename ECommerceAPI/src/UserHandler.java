import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UsersHandler implements HttpHandler {
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length());
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response.getBytes());
        outputStream.close();
    }

    private static void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JSONObject errorObject = new JSONObject();
        errorObject.put("error", message);
        sendResponse(exchange, statusCode, errorObject.toString());
    }

    private static String getRequestData(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder requestData = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestData.append(line);
        }

        return requestData.toString();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (method.equals("GET")) {
            if (path.equals("/users")) {
                handleGetAllUsers(exchange);
                return;
            } else if (path.matches("/users/\\d+")) {
                handleGetUserById(exchange);
                return;
            }
        } else if (method.equals("POST") && path.equals("/users")) {
            handleCreateUser(exchange);
            return;
        } else if (method.equals("PUT")) {
            if (path.matches("/users/\\d+")) {
                handleUpdateUser(exchange);
                return;
            } else if (path.matches("/users/addresses/\\d+")) {
                handleUpdateAddress(exchange);
                return;
            }
        } else if (method.equals("DELETE") && path.matches("/users/\\d+")) {
            if (path.matches("/users/\\d+")) {
                handleDeleteUser(exchange);
                return;
            } else if (path.matches("/users/addresses/\\d+")) {
                handleDeleteAddress(exchange);
                return;
            }
        }

        sendErrorResponse(exchange, 404, "Not Found");
    }

    private void handleGetAllUsers(HttpExchange exchange) throws IOException {
//            if (!validateApiKey(exchange)) {
//                sendErrorResponse(exchange, 401, "Unauthorized");
//                return;
//            }

        try (Connection connection = DatabaseConnection.connect();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM users")) {

            JSONArray usersArray = new JSONArray();

            while (resultSet.next()) {
                JSONObject userObject = new JSONObject();
                userObject.put("user_id", resultSet.getInt("user_id"));
                userObject.put("first_name", resultSet.getString("first_name"));
                userObject.put("last_name", resultSet.getString("last_name"));
                userObject.put("email", resultSet.getString("email"));
                userObject.put("phone_number", resultSet.getString("phone_number"));
                userObject.put("type", resultSet.getString("type"));

                usersArray.put(userObject);
            }

            sendResponse(exchange, 200, usersArray.toString());
        } catch (SQLException | JSONException e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error");
        }
    }

}