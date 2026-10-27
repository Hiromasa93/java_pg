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
                server.createContext("/api/todos",exchange->
        
            { // Todo一覧APIの入口を追加します
                if (!exchange.getRequestURI().getPath().equals("/api/todos")) { // パスが完全一致しない場合は404を返します
                    sendJson(exchange, 404, "{\"error\":\"not found\"}"); // エラー内容もJSONで返します
                    return; // 以降の処理を終了します
                } // パスの確認を終えます
                if (!exchange.getRequestMethod().equals("GET")) { // GET以外のリクエストを確認します
                    exchange.getResponseHeaders().set("Allow", "GET"); // 許可するメソッドを通知します
                    exchange.sendResponseHeaders(405, -1); // 405を返して本文を省略します
                    exchange.close(); // 接続を閉じます
                    return; // 以降の処理を終了します
                } // メソッドの確認を終えます
                try { // Todo一覧の取得とJSONへの変換を行います
                    StringBuilder json = new StringBuilder("["); // JSON配列を作り始めます
                    List<Todo> todos = loadTodos(); // データベースから全Todoを読み込みます
                    for (int i = 0; i < todos.size(); i++) { // Todoを順番にJSONへ追加します
                        Todo todo = todos.get(i); // 現在のTodoを取り出します
                        if (i > 0)
                            json.append(","); // 2件目以降の前に区切り文字を追加します
                        json.append("{\"title\":").append(jsonString(todo.title == null ? "" : todo.title)) // タイトルをJSON文字列として追加します
                                .append(",\"done\":").append(todo.done).append("}"); // 完了状態をJSONの真偽値として追加します
                    } // Todoの追加を終えます
                    json.append("]"); // JSON配列を閉じます
                    sendJson(exchange, 200, json.toString()); // JSONを200で返します
                } catch (SQLException e) { // データベースエラーを処理します
                    sendJson(exchange, 500, "{\"error\":\"database error\"}"); // エラーをJSONで返します
                } // Todo一覧の処理を終えます
            }); // Todo一覧APIの入口を登録します
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

    private static String jsonString(String value) { // 文字列をJSON形式にエスケープします
        StringBuilder json = new StringBuilder("\""); // JSON文字列の開始引用符を追加します
        for (int i = 0; i < value.length(); i++) { // 文字列を1文字ずつ処理します
            char c = value.charAt(i); // 現在の文字を取り出します
            switch (c) { // 特殊文字ごとにJSON用の表記へ変換します
                case '"': json.append("\\\""); break; // 引用符をエスケープします
                case '\\': json.append("\\\\"); break; // バックスラッシュをエスケープします
                case '\b': json.append("\\b"); break; // バックスペースをエスケープします
                case '\f': json.append("\\f"); break; // フォームフィードをエスケープします
                case '\n': json.append("\\n"); break; // 改行をエスケープします
                case '\r': json.append("\\r"); break; // 復帰をエスケープします
                case '\t': json.append("\\t"); break; // タブをエスケープします
                default: // その他の文字を処理します
                    if (c < 0x20) json.append(String.format("\\u%04x", (int) c)); // 制御文字をUnicode表記にします
                    else json.append(c); // 通常の文字はそのまま追加します
            } // 文字ごとの変換を終えます
        } // 文字列の変換を終えます
        return json.append("\"").toString(); // 終了引用符を追加して返します
    } // JSON文字列のエスケープ処理を終えます
    
    private static void sendJson(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException { // JSONレスポンスを送信します
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8); // 本文をUTF-8のバイト列にします
        exchange.getResponseHeaders().set("Content-Type", "application/json"); // charsetを付けずにContent-Typeを設定します
        exchange.sendResponseHeaders(status, bytes.length); // ステータスと本文サイズを送信します
        exchange.getResponseBody().write(bytes); // JSON本文を書き込みます
        exchange.close(); // レスポンスを閉じます
    } // JSONレスポンスの送信を終えます
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
