import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を読み込みます。
import java.util.ArrayList; // Todoを順番に並べて保存するリストを作る機能を読み込みます。
import java.util.List; // Todoのリストを表す型を読み込みます。
import java.net.InetSocketAddress; // 待ち受ける住所とポートを指定する機能を読み込みます。
import java.net.URLDecoder; // URL内の記号列を元の文字に戻す機能を読み込みます。
import java.nio.charset.StandardCharsets; // UTF-8の文字コードを指定する機能を読み込みます。

public class App { // Appという名前のクラスを定義します。
    static List<Todo> todos = new ArrayList<>(); // ★変更 Todoを保存するリストをクラス直下に置きます。
    static int nextId = 1; // ★変更 次に振る番号をクラス直下に置きます。

    public static void main(String[] args) throws Exception { // Javaプログラムの開始位置を定義します。【1】
        todos.add(new Todo(nextId++, "牛乳を買う")); // ★変更 起動時のサンプルTodoを追加します。
        Todo egg = new Todo(nextId++, "卵を買う"); // ★変更 起動時のサンプルTodoを作ります。
        egg.setDone(true); // ★変更 卵を買うTodoを完了にします。
        todos.add(egg); // ★変更 卵を買うTodoを追加します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートでサーバーを用意します。【1】
        server.createContext("/", exchange -> { // ルートURLへのリクエストを処理します。【1】
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します。
            String method = exchange.getRequestMethod(); // GETやPOSTなどの方法を取り出します。
            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常の応答をUTF-8のテキストにします。
            if (path.equals("/add") && method.equals("POST")) { // POSTで /add に届いたTodoを処理します。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // 送られてきた内容をUTF-8で読み取ります。
                String value = body.substring(5); // todo=の5文字を除いた値を取り出します。
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8); // ★変更 URL形式の文字をUTF-8の日本語に戻します。
                if (!title.isEmpty()) { // ★変更 入力が空でないかを確認します。
                    todos.add(new Todo(nextId, title)); // ★変更 Todoをリストに追加します。
                    nextId++; // ★変更 次に振る番号を進めます。
                }
                exchange.getResponseHeaders().set("Location", "/"); // 戻り先を / に設定します。
                exchange.sendResponseHeaders(303, -1); // / への移動をブラウザーに伝えます。
                exchange.close(); // 応答を閉じます。
                return; // この分岐の処理を終えます。
            } else if (path.equals("/done") && method.equals("GET")) { // ★追加 GET /done でTodoを完了にします。
                String query = exchange.getRequestURI().getQuery(); // ★追加 URLの ? 以降を受け取ります。
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加 idがあるか確認します。
                    try { // ★追加 idを数字に変換します。
                        int id = Integer.parseInt(query.substring(3)); // ★追加 idを数に変えます。
                        for (Todo todo : todos) { // ★追加 Todoを1件ずつ確認します。
                            if (todo.getId() == id) { // ★追加 idが一致するTodoを探します。
                                todo.setDone(true); // ★追加 Todoを完了にします。
                                break; // ★追加 1件見つかったので繰り返しを終えます。
                            }
                        }
                    } catch (NumberFormatException e) { // ★追加 数字でないidは何もせず受け止めます。
                    }
                }
                exchange.getResponseHeaders().set("Location", "/"); // ★追加 戻り先を / にします。
                exchange.sendResponseHeaders(303, -1); // ★追加 303で / に戻します。
                exchange.close(); // ★追加 応答を閉じます。
                return; // ★追加 ここで処理を終えます。
            } else if (path.equals("/delete") && method.equals("GET")) { // ★追加 GET /delete でTodoを削除します。
                String query = exchange.getRequestURI().getQuery(); // ★追加 URLの ? 以降を受け取ります。
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加 idがあるか確認します。
                    try { // ★追加 idを数字に変換します。
                        int id = Integer.parseInt(query.substring(3)); // ★追加 idを数に変えます。
                        todos.removeIf(todo -> todo.getId() == id); // ★追加 idが一致するTodoを削除します。
                    } catch (NumberFormatException e) { // ★追加 数字でないidは何もせず受け止めます。
                    }
                }
                exchange.getResponseHeaders().set("Location", "/"); // ★追加 戻り先を / にします。
                exchange.sendResponseHeaders(303, -1); // ★追加 303で / に戻します。
                exchange.close(); // ★追加 応答を閉じます。
                return; // ★追加 ここで処理を終えます。
            } else if (path.equals("/")) { // パスが / のときフォームとTodo一覧を表示します。
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>"; // Todoを送るフォームと一覧を開始します。
                for (Todo todo : todos) { // ★変更 Todoを1件ずつ取り出します。
                    String mark = ""; // ★変更 完了印を空にします。
                    if (todo.isDone()) { // ★変更 Todoが完了しているか確認します。
                        mark = " <span style='color: #C8A2C8;'>【済】</span>"; // ★変更 完了表示を薄紫色にします。
                    }
                    html += "<li>" + todo.getTitle() + mark + " <a href='/done?id=" + todo.getId()
                            + "'>完了</a> <a href='/delete?id=" + todo.getId() + "'>削除</a></li>"; // ★追加
                                                                                                // id付きの完了・削除リンクを項目に追加します。
                } // TodoをHTMLに追加する処理を終えます。
                html += "</ul>"; // HTMLのリストを閉じます。
                message = html; // 組み立てたHTMLを応答にします。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // / の応答をUTF-8のHTMLにします。
            } else { // どちらのパスにも当てはまらない場合を処理します。
                message = "ページが見つかりません"; // ★変更 見つからないページの応答です。
            }
            byte[] body = message.getBytes("UTF-8"); // 返す文字をUTF-8のバイト列にします。【毎】
            exchange.sendResponseHeaders(200, body.length); // 成功の応答と本文の長さを送ります。【毎】
            exchange.getResponseBody().write(body); // 本文をブラウザへ送ります。【毎】
            exchange.getResponseBody().close(); // 応答の送信を終了します。【毎】
        }); // ルートURLの処理を登録します。【1】
        server.start(); // サーバーの待ち受けを開始します。【1】
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 指定された起動メッセージを表示します。【1】
    } // mainメソッドを終了します。【1】
} // Appクラスを終了します。

class Todo { // ★変更 Todoの情報をまとめるクラスです。
    private final int id; // ★変更 Todoの番号です。
    private final String title; // ★変更 やることです。
    private boolean done; // ★変更 終わったかどうかです。

    Todo(int id, String title) { // ★変更 Todoを作ります。
        this.id = id; // ★変更 番号を保存します。
        this.title = title; // ★変更 やることを保存します。
        this.done = false; // ★変更 最初は未完了にします。
    } // ★変更 コンストラクタを閉じます。

    int getId() { // ★変更 Todoの番号を読み出します。
        return id; // ★変更 番号を返します。
    } // ★変更 メソッドを閉じます。

    String getTitle() { // ★変更 やることを読み出します。
        return title; // ★変更 やることを返します。
    } // ★変更 メソッドを閉じます。

    boolean isDone() { // ★変更 終わったかどうかを読み出します。
        return done; // ★変更 完了状態を返します。
    } // ★変更 メソッドを閉じます。

    void setDone(boolean done) { // ★変更 終わったかどうかを書き換えます。
        this.done = done; // ★変更 完了状態を保存します。
    } // ★変更 メソッドを閉じます。
} // ★変更 Todoクラスを閉じます。
