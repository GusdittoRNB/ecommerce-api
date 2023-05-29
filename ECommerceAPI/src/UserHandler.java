import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.sql.*;

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
        } else if (method.equals("POST")) {
            if (path.matches("/users")) {
                handleCreateUser(exchange);
                return;
            } else if (path.matches("/users/addresses")) {
                handleCreateAddress(exchange);
                return;
            }
        } else if (method.equals("PUT")) {
            if (path.matches("/users/\\d+")) {
                handleUpdateUser(exchange);
                return;
            } else if (path.matches("/users/addresses/\\d+")) {
                handleUpdateAddress(exchange);
                return;
            }
        } else if (method.equals("DELETE")) {
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

    private void handleGetUserById(HttpExchange exchange) throws IOException {
//            if (!validateApiKey(exchange)) {
//                sendErrorResponse(exchange, 401, "Unauthorized");
//                return;
//            }

        String path = exchange.getRequestURI().getPath();
        int userId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT users.user_id, users.first_name, users.last_name, users.email, " +
                             "users.phone_number, users.type, addresses.address_id, addresses.type AS address_type, addresses.line1, " +
                             "addresses.line2, addresses.city, addresses.province, addresses.postcode " +
                             "FROM users LEFT JOIN addresses ON users.user_id = addresses.user_id " +
                             "WHERE users.user_id = ?")) {

            statement.setInt(1, userId);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                JSONObject userObject = new JSONObject();
                userObject.put("user_id", resultSet.getInt("user_id"));
                userObject.put("first_name", resultSet.getString("first_name"));
                userObject.put("last_name", resultSet.getString("last_name"));
                userObject.put("email", resultSet.getString("email"));
                userObject.put("phone_number", resultSet.getString("phone_number"));
                userObject.put("type", resultSet.getString("type"));

                JSONArray addressesArray = new JSONArray();
                while (resultSet.getString("address_type") != null) {
                    JSONObject addressObject = new JSONObject();
                    addressObject.put("address_id", resultSet.getString("address_id"));
                    addressObject.put("type", resultSet.getString("address_type"));
                    addressObject.put("line1", resultSet.getString("line1"));
                    addressObject.put("line2", resultSet.getString("line2"));
                    addressObject.put("city", resultSet.getString("city"));
                    addressObject.put("province", resultSet.getString("province"));
                    addressObject.put("postcode", resultSet.getString("postcode"));
                    addressesArray.put(addressObject);
                    if (!resultSet.next()) {
                        break;
                    }
                }

                if (addressesArray.length() > 0) {
                    userObject.put("addresses", addressesArray);
                }

                sendResponse(exchange, 200, userObject.toString());
                return;
            }
        } catch (SQLException | JSONException e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error");
        }
        sendErrorResponse(exchange, 404, "User not found");
    }

    private void handleCreateUser(HttpExchange exchange) throws IOException {
//            if (!validateApiKey(exchange)) {
//                sendErrorResponse(exchange, 401, "Unauthorized");
//                return;
//            }

        String requestBody = getRequestData(exchange);
        try {
            JSONObject userObject = new JSONObject(requestBody);
            String firstName = userObject.getString("first_name");
            String lastName = userObject.getString("last_name");
            String email = userObject.getString("email");
            String phoneNumber = userObject.getString("phone_number");
            String type = userObject.getString("type");

            int userId;
            try (Connection connection = DatabaseConnection.connect();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO users (first_name, last_name, email, phone_number, type) " +
                                 "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {

                statement.setString(1, firstName);
                statement.setString(2, lastName);
                statement.setString(3, email);
                statement.setString(4, phoneNumber);
                statement.setString(5, type);
                statement.executeUpdate();

                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                } else {
                    sendErrorResponse(exchange, 500, "Failed to create user");
                    return;
                }
            }

            // Check if the user has address data
            if (userObject.has("addresses")) {
                JSONArray addressesArray = userObject.getJSONArray("addresses");
                for (int i = 0; i < addressesArray.length(); i++) {
                    JSONObject addressObject = addressesArray.getJSONObject(i);
                    String addressType = addressObject.getString("type");
                    String line1 = addressObject.getString("line1");
                    String line2 = addressObject.getString("line2");
                    String city = addressObject.getString("city");
                    String province = addressObject.getString("province");
                    String postcode = addressObject.getString("postcode");

                    // Insert address data into the addresses table
                    try (Connection connection = DatabaseConnection.connect();
                         PreparedStatement statement = connection.prepareStatement(
                                 "INSERT INTO addresses (user_id, type, line1, line2, city, province, postcode) " +
                                         "VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                        statement.setInt(1, userId);
                        statement.setString(2, addressType);
                        statement.setString(3, line1);
                        statement.setString(4, line2);
                        statement.setString(5, city);
                        statement.setString(6, province);
                        statement.setString(7, postcode);
                        statement.executeUpdate();
                    }

                }
            }
            // Create the response object
            JSONObject response = new JSONObject();
            response.put("message", "User created successfully");
            response.put("user_id", userId);

            sendResponse(exchange, 201, response.toString());
        } catch (JSONException e) {
            sendErrorResponse(exchange, 400, "Invalid request body");
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error");
        }
    }

    private void handleCreateAddress(HttpExchange exchange) throws IOException {
//            if (!validateApiKey(exchange)) {
//                sendErrorResponse(exchange, 401, "Unauthorized");
//                return;
//            }

        String requestBody = getRequestData(exchange);
        try {
            JSONObject addressObject = new JSONObject(requestBody);
            int userId = addressObject.getInt("user_id");
            String addressType = addressObject.getString("type");
            String line1 = addressObject.getString("line1");
            String line2 = addressObject.getString("line2");
            String city = addressObject.getString("city");
            String province = addressObject.getString("province");
            String postcode = addressObject.getString("postcode");

            try (Connection connection = DatabaseConnection.connect();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO addresses (user_id, type, line1, line2, city, province, postcode) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                statement.setInt(1, userId);
                statement.setString(2, addressType);
                statement.setString(3, line1);
                statement.setString(4, line2);
                statement.setString(5, city);
                statement.setString(6, province);
                statement.setString(7, postcode);
                statement.executeUpdate();
            }

            JSONObject response = new JSONObject();
            response.put("message", "Address added successfully");

            sendResponse(exchange, 201, response.toString());
        } catch (JSONException e) {
            sendErrorResponse(exchange, 400, "Invalid request body");
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error");
        }
    }

    private void handleUpdateUser(HttpExchange exchange) throws IOException {
//            if (!validateApiKey(exchange)) {
//                sendErrorResponse(exchange, 401, "Unauthorized");
//                return;
//            }

        String path = exchange.getRequestURI().getPath();
        int userId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));

        String requestBody = getRequestData(exchange);
        try {
            JSONObject userObject = new JSONObject(requestBody);
            String firstName = userObject.getString("first_name");
            String lastName = userObject.getString("last_name");
            String email = userObject.getString("email");
            String phoneNumber = userObject.getString("phone_number");
            String type = userObject.getString("type");

            try (Connection connection = DatabaseConnection.connect();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE users SET first_name = ?, last_name = ?, email = ?, phone_number = ?, type = ? WHERE user_id = ?")) {

                statement.setString(1, firstName);
                statement.setString(2, lastName);
                statement.setString(3, email);
                statement.setString(4, phoneNumber);
                statement.setString(5, type);
                statement.setInt(6, userId);

                int affectedRows = statement.executeUpdate();
                if (affectedRows > 0) {
                    JSONObject responseObj = new JSONObject();
                    responseObj.put("message", "User updated successfully");
                    sendResponse(exchange, 200, responseObj.toString());
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendErrorResponse(exchange, 400, "Bad Request");
    }

    private void handleUpdateAddress(HttpExchange exchange) throws IOException {
//            if (!validateApiKey(exchange)) {
//                sendErrorResponse(exchange, 401, "Unauthorized");
//                return;
//            }

        String path = exchange.getRequestURI().getPath();
        int addressId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));

        String requestBody = getRequestData(exchange);
        try {
            JSONObject addressObject = new JSONObject(requestBody);
            String type = addressObject.getString("type");
            String line1 = addressObject.getString("line1");
            String line2 = addressObject.getString("line2");
            String city = addressObject.getString("city");
            String province = addressObject.getString("province");
            String postcode = addressObject.getString("postcode");

            try (Connection connection = DatabaseConnection.connect();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE addresses SET type = ?, line1 = ?, line2 = ?, city = ?, province = ?, postcode = ? WHERE address_id = ?")) {

                statement.setString(1, type);
                statement.setString(2, line1);
                statement.setString(3, line2);
                statement.setString(4, city);
                statement.setString(5, province);
                statement.setString(6, postcode);
                statement.setInt(7, addressId);

                int affectedRows = statement.executeUpdate();
                if (affectedRows > 0) {
                    JSONObject responseObj = new JSONObject();
                    responseObj.put("message", "Address updated successfully");
                    sendResponse(exchange, 200, responseObj.toString());
                    return;
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendErrorResponse(exchange, 400, "Bad Request");
    }

}