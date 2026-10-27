import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection; // ★ SQLite接続に使います。
import java.sql.DriverManager; // ★ SQLite JDBC接続を作ります。
import java.sql.PreparedStatement; // ★ SQLへ値を安全に渡します。
import java.sql.ResultSet; // ★ SELECT結果を読み取ります。
import java.sql.SQLException; // ★ SQLエラーを扱います。
import java.util.ArrayList; // ★ SELECT結果をTodo一覧にします。
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class App {
    private static Connection connection; // ★ DB接続を保持します。

    public static void main(String[] args) throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite:todos.db"); // ★ 指定DBへ接続します。
        try (PreparedStatement statement = connection.prepareStatement( // ★
                "CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY, title TEXT, done INTEGER)")) { // ★
            statement.executeUpdate(); // ★ テーブルがなければ作成します。
        } // ★

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            try { // ★ DB処理のSQLExceptionをHTTPハンドラー内で処理します。
                if (path.equals("/add") && method.equals("POST")) {
                    Map<String, String> form = parseForm(
                            new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    String title = form.getOrDefault("todo", "").trim();
                    if (!title.isEmpty())
                        insertTodo(title); // ★ INSERTで追加します。
                    redirect(exchange);
                    return;
                } else if ((path.equals("/done") || path.equals("/delete")) && method.equals("GET")) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("id=")) {
                        try {
                            int id = Integer.parseInt(query.substring(3));
                            if (path.equals("/done"))
                                updateTodo(id); // ★ UPDATEで完了にします。
                            else
                                deleteTodo(id); // ★ DELETEで削除します。
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    redirect(exchange);
                    return;
                } else if (!path.equals("/")) {
                    send(exchange, "ページが見つかりません", "text/plain; charset=UTF-8");
                    return;
                }

                List<Todo> todos = loadTodos(); // ★ SELECTで一覧を取得します。
                StringBuilder html = new StringBuilder("<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                        + "<meta name='viewport' content='width=device-width, initial-scale=1'><title>わたしのTodo</title>"
                        + "<style>body{max-width:720px;margin:24px auto;padding:0 16px;font:16px sans-serif}"
                        + "form{display:flex;gap:12px;padding:16px;border:1px solid #ddd;border-radius:8px}"
                        + "li{margin:12px 0}input,button{font:inherit;padding:7px}</style>"
                        + "</head><body><h1>わたしのTodo</h1><form method='post' action='/add'>"
                        + "<label>タスク名 <input name='todo' required></label><button>追加</button></form>");
                if (todos.isEmpty())
                    html.append("<p>やることは、いまゼロです</p>");
                else {
                    html.append("<ul>");
                    for (Todo todo : todos) {
                        html.append("<li>").append(escape(todo.title))
                                .append(todo.done ? " <span style='color:#8a5a8a'>【済】</span>" : "")
                                .append(" <a href='/done?id=").append(todo.id).append("'>完了</a>")
                                .append(" <a href='/delete?id=").append(todo.id).append("'>削除</a></li>");
                    }
                    html.append("</ul>");
                }
                html.append("</body></html>");
                send(exchange, html.toString(), "text/html; charset=UTF-8");
            } catch (SQLException e) { // ★ DBエラーを応答に表示します。
                send(exchange, "データベースエラー: " + escape(e.getMessage()), "text/plain; charset=UTF-8"); // ★
            }
        });
        server.start();
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）");
    }

    private static void insertTodo(String title) throws SQLException { // ★ INSERT
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO todos (title, done) VALUES (?, 0)")) { // ★
            statement.setString(1, title); // ★
            statement.executeUpdate(); // ★
        }
    }

    private static void updateTodo(int id) throws SQLException { // ★ UPDATE
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE todos SET done = 1 WHERE id = ?")) { // ★
            statement.setInt(1, id); // ★
            statement.executeUpdate(); // ★
        }
    }

    private static void deleteTodo(int id) throws SQLException { // ★ DELETE
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM todos WHERE id = ?")) { // ★
            statement.setInt(1, id); // ★
            statement.executeUpdate(); // ★
        }
    }

    private static List<Todo> loadTodos() throws SQLException { // ★ SELECT
        List<Todo> todos = new ArrayList<>(); // ★
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, title, done FROM todos ORDER BY id"); // ★
             ResultSet rows = statement.executeQuery()) { // ★
            while (rows.next()) { // ★
                todos.add(new Todo(rows.getInt("id"), rows.getString("title"), rows.getInt("done") != 0)); // ★
            }
        }
        return todos; // ★
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static void redirect(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Location", "/");
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private static void send(com.sun.net.httpserver.HttpExchange exchange, String message, String type)
            throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }
}

class Todo {
    final int id; // ★ SQLiteから読み込んだIDです。
    final String title; // ★ SQLiteから読み込んだタスク名です。
    final boolean done; // ★ SQLiteから読み込んだ完了状態です。

    Todo(int id, String title, boolean done) { // ★
        this.id = id;
        this.title = title;
        this.done = done;
    }
}
