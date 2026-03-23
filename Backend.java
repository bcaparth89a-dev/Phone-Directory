import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.*;

public class Backend {

    private static final String URL = "jdbc:mysql://localhost:3306/Phonedb";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private static final String[] ALLOWED_FIELDS = {
            "id", "name", "phone_number", "email", "address"
    };

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(9090), 0);

        server.createContext("/addContact", new AddContactHandler());
        server.createContext("/allContacts", new AllContactsHandler());
        server.createContext("/deleteContact", new DeleteContactHandler());
        server.createContext("/updateContact", new UpdateContactHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("✅ Server running at http://localhost:9090");
    }

    // ================= ADD =================
    static class AddContactHandler implements HttpHandler {
        public void handle(HttpExchange exchange) {

            try {
                if (handleOptions(exchange)) return;

                if ("POST".equals(exchange.getRequestMethod())) {

                    String body = new String(exchange.getRequestBody().readAllBytes());
                    JSONObject json = new JSONObject(body);
                    JSONObject res = new JSONObject();

                    try (Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS)) {

                        String sql = "INSERT INTO contacts VALUES (?, ?, ?, ?, ?)";
                        PreparedStatement ps = conn.prepareStatement(sql);

                        ps.setInt(1, json.getInt("id"));
                        ps.setString(2, json.getString("name"));
                        ps.setString(3, json.getString("phone"));
                        ps.setString(4, json.optString("email", ""));
                        ps.setString(5, json.optString("address", ""));

                        ps.executeUpdate();

                        res.put("message", "Added Successfully");

                    } catch (SQLException e) {
                        res.put("error", e.getMessage());
                    }

                    sendResponse(exchange, res.toString());

                } else {
                    sendMethodNotAllowed(exchange);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ================= GET =================
    static class AllContactsHandler implements HttpHandler {
        public void handle(HttpExchange exchange) {

            try {
                if (handleOptions(exchange)) return;

                if ("GET".equals(exchange.getRequestMethod())) {

                    JSONArray arr = new JSONArray();

                    try (Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS)) {

                        ResultSet rs = conn.createStatement()
                                .executeQuery("SELECT * FROM contacts");

                        while (rs.next()) {
                            JSONObject obj = new JSONObject();

                            obj.put("id", rs.getInt("id"));
                            obj.put("name", rs.getString("name"));
                            obj.put("phone", rs.getString("phone_number"));
                            obj.put("email", rs.getString("email"));
                            obj.put("address", rs.getString("address"));

                            arr.put(obj);
                        }
                    }

                    sendResponse(exchange, arr.toString());

                } else {
                    sendMethodNotAllowed(exchange);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ================= DELETE =================
    static class DeleteContactHandler implements HttpHandler {
        public void handle(HttpExchange exchange) {

            try {
                if (handleOptions(exchange)) return;

                if ("POST".equals(exchange.getRequestMethod())) {

                    String body = new String(exchange.getRequestBody().readAllBytes());
                    JSONObject json = new JSONObject(body);
                    JSONObject res = new JSONObject();

                    String field = json.getString("by");
                    String value = json.getString("value");

                    if (!isAllowedField(field)) {
                        res.put("error", "Invalid field");
                        sendResponse(exchange, res.toString());
                        return;
                    }

                    try (Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS)) {

                        String sql = "DELETE FROM contacts WHERE " + field + "=?";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, value);

                        int rows = ps.executeUpdate();
                        res.put("success", rows > 0);
                    }

                    sendResponse(exchange, res.toString());

                } else {
                    sendMethodNotAllowed(exchange);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ================= UPDATE =================
    static class UpdateContactHandler implements HttpHandler {
        public void handle(HttpExchange exchange) {

            try {
                if (handleOptions(exchange)) return;

                if ("POST".equals(exchange.getRequestMethod())) {

                    String body = new String(exchange.getRequestBody().readAllBytes());
                    JSONObject json = new JSONObject(body);
                    JSONObject res = new JSONObject();

                    String field = json.getString("by");
                    String value = json.get("value").toString();

                    if (!isAllowedField(field)) {
                        res.put("error", "Invalid field");
                        sendResponse(exchange, res.toString());
                        return;
                    }

                    try (Connection conn = DriverManager.getConnection(URL, DB_USER, DB_PASS)) {

                        String sql = "UPDATE contacts SET name=?, phone_number=?, email=?, address=? WHERE " + field + "=?";
                        PreparedStatement ps = conn.prepareStatement(sql);

                        ps.setString(1, json.optString("name", ""));
                        ps.setString(2, json.optString("phone", ""));
                        ps.setString(3, json.optString("email", ""));
                        ps.setString(4, json.optString("address", ""));
                        ps.setString(5, value);

                        int rows = ps.executeUpdate();
                        res.put("success", rows > 0);
                    }

                    sendResponse(exchange, res.toString());

                } else {
                    sendMethodNotAllowed(exchange);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ================= UTIL =================

    private static boolean isAllowedField(String field) {
        for (String f : ALLOWED_FIELDS) {
            if (f.equals(field)) return true;
        }
        return false;
    }

    private static boolean handleOptions(HttpExchange exchange) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static void sendResponse(HttpExchange exchange, String response) throws Exception {

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        exchange.sendResponseHeaders(200, response.getBytes().length);

        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static void sendMethodNotAllowed(HttpExchange exchange) throws Exception {
        exchange.sendResponseHeaders(405, -1);
    }
}