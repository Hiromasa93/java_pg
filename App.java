import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を読み込みます。
import java.net.InetSocketAddress; // 待ち受ける住所とポートを指定する機能を読み込みます。

public class App { // Appという名前のクラスを定義します。
    public static void main(String[] args) throws Exception { // Javaプログラムの開始位置を定義します。【1】
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートでサーバーを用意します。【1】
        server.createContext("/", exchange -> { // ルートURLへのリクエストを処理します。【1】
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します。
            String message;
            if (path.equals("/hello")) { // パスが /hello かどうかを比べます。
                message = "こんにちは！";
            } else if (path.equals("/bye")) { // パスが /bye かどうかを比べます。
                message = "さようなら！";
            } else if (path.equals("/menu")) {
                message = "今夜の定食はカレー";
            } else { // どちらのパスにも当てはまらない場合を処理します。
                message = "ページが見つかりません";
            }
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 返す文字がUTF-8であることを指定します。【毎】
            byte[] body = message.getBytes("UTF-8"); // 返す文字をUTF-8のバイト列にします。【毎】
            exchange.sendResponseHeaders(200, body.length); // 成功の応答と本文の長さを送ります。【毎】
            exchange.getResponseBody().write(body); // 本文をブラウザへ送ります。【毎】
            exchange.getResponseBody().close(); // 応答の送信を終了します。【毎】
        }); // ルートURLの処理を登録します。【1】
        server.start(); // サーバーの待ち受けを開始します。【1】
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 指定された起動メッセージを表示します。【1】
    } // mainメソッドを終了します。【1】
} // Appクラスを終了します。
