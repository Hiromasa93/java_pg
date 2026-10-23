import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を読み込みます。
import java.util.ArrayList; // Todoを順番に並べて保存するリストを作る機能を読み込みます。
import java.util.List; // Todoのリストを表す型を読み込みます。
import java.net.InetSocketAddress; // 待ち受ける住所とポートを指定する機能を読み込みます。
import java.net.URLDecoder; // URL内の記号列を元の文字に戻す機能を読み込みます。

public class App { // Appという名前のクラスを定義します。
    public static void main(String[] args) throws Exception { // Javaプログラムの開始位置を定義します。【1】
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートでサーバーを用意します。【1】
        server.createContext("/", exchange -> { // ルートURLへのリクエストを処理します。【1】
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します。
            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常の応答をUTF-8のテキストにします。
            if (path.equals("/hello")) { // パスが /hello かどうかを比べます。
                String query = exchange.getRequestURI().getRawQuery(); // URLのクエリ文字列を取り出します。
                System.out.println("query = " + query);
                String name;
                if (query == null) { // クエリがないかどうかを確認します。
                    name = "ゲスト"; // 名前がないときはゲストにします。
                } else {
                    name = query.substring(5); // name=の後ろを切り出します。
                    name = URLDecoder.decode(name, "UTF-8"); // URL内の記号列をUTF-8の文字に戻します。
                }
                message = "こんにちは、" + name + "さん！"; // 名前を含めた応答文を作ります。
            } else if (path.equals("/todos")) { // パスが /todos かどうかを比べます。
                List<String> todos = new ArrayList<>(); // Todoを入れるリストを用意します。
                todos.add("牛乳を買う"); // 1件目のTodoを追加します。
                todos.add("卵を買う"); // 2件目のTodoを追加します。
                todos.add("パンを買う"); // 3件目のTodoを追加します。
                todos.add("勉強をする");
                String html = "<ul>"; // HTMLのリストを開始します。
                for (String todo : todos) { // Todoを1件ずつ取り出します。
                    html += "<li>" + todo + "</li>"; // TodoをHTMLの項目として追加します。
                } // TodoをHTMLに追加する処理を終えます。
                html += "</ul>"; // HTMLのリストを閉じます。
                message = html; // 組み立てたHTMLを応答にします。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // /todosの応答をUTF-8のHTMLにします。【変換】
            } else if (path.equals("/bye")) { // パスが /bye かどうかを比べます。
                message = "さようなら！";
            } else if (path.equals("/menu")) {
                message = "今夜の定食はカレー";
            } else { // どちらのパスにも当てはまらない場合を処理します。
                message = "ページが見つかりません";
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
