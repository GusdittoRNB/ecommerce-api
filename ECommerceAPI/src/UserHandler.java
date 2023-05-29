import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.*;

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
                handleGetUsers(exchange);
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

}