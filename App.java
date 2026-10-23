import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を読み込みます。
import java.net.InetSocketAddress; // 待ち受ける住所とポートを指定する機能を読み込みます。

public class App { // Appという名前のクラスを定義します。
    public static void main(String[] args) throws Exception { // Javaプログラムの開始位置を定義します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートでサーバーを用意します。
        server.createContext("/", exchange -> { // ルートURLへのリクエストを処理します。
            String message = "こんにちは、世界"; // ブラウザに返す文字を用意します。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 返す文字がUTF-8であることを指定します。
            byte[] body = message.getBytes("UTF-8"); // 返す文字をUTF-8のバイト列にします。
            exchange.sendResponseHeaders(200, body.length); // 成功の応答と本文の長さを送ります。
            exchange.getResponseBody().write(body); // 本文をブラウザへ送ります。
            exchange.getResponseBody().close(); // 応答の送信を終了します。
        }); // ルートURLの処理を登録します。
        server.start(); // サーバーの待ち受けを開始します。
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 指定された起動メッセージを表示します。
    } // mainメソッドを終了します。
} // Appクラスを終了します。
