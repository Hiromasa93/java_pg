// import（機能の読み込み）: Javaに最初からある通信・文字・リストの機能を使えるようにします。
import com.sun.net.httpserver.HttpServer; // ブラウザーからのリクエストを受け付けるWebサーバーです。
import java.io.IOException; // 通信中に起きる入出力エラーを表します。
import java.net.InetSocketAddress; // サーバーのポート番号など、接続先の情報を表します。
import java.net.URLDecoder; // フォームから届く記号を元の文字に戻します。
import java.nio.charset.StandardCharsets; // 日本語を正しく扱うためUTF-8を指定します。
import java.util.ArrayList; // Todoを複数保存する入れ物を作ります。
import java.util.LinkedHashMap; // フォームの項目名と入力内容を組にして保存します。
import java.util.List; // Todo一覧の型です。
import java.util.Map; // 項目名と値の組を扱う型です。

// Appは、このアプリ全体の処理をまとめたクラス（設計図）です。
public class App {
    // staticは、アプリ全体で共有するデータという意味です。追加したTodoをここに保存します。
    static List<Todo> todos = new ArrayList<>();
    // 新しいTodoに重ならない番号を付けるためのカウンターです。
    static int nextId = 1;

    // mainはJavaを起動したとき最初に実行されるメソッド（処理のまとまり）です。
    public static void main(String[] args) throws Exception {
        // 起動時に表示するサンプルのTodoを2件用意します。
        todos.add(new Todo(nextId++, "牛乳を買う", "日時未設定"));
        Todo egg = new Todo(nextId++, "卵を買う", "日時未設定");
        // サンプルの卵のTodoだけ、最初から完了状態にします。
        egg.setDone(true);
        todos.add(egg);
        // 8080番ポート（通信の窓口番号）でWebサーバーを作ります。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        // URLの入口を登録し、ブラウザーから来たリクエスト（要求）に応じて処理します。
        server.createContext("/", exchange -> {
            // リクエストのURLの道筋（例: /add）と通信方法（GET/POST）を取り出します。
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            // POSTはフォーム送信です。受け取った内容から新しいTodoを作ります。
            if (path.equals("/add") && method.equals("POST")) {
                // フォームの本文をUTF-8で読み、項目名と入力内容に分けます。
                Map<String, String> form = parseForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                // todo欄の文字を取り出し、前後の空白を取り除きます。
                String title = form.getOrDefault("todo", "").trim();
                // タスク名が空でなければ、予定内容も含めて一覧に追加します。
                if (!title.isEmpty()) todos.add(new Todo(nextId++, title, makeSchedule(form)));
                redirect(exchange);
                return;
            // GETはページを開く通信です。idを使って完了または削除します。
            } else if ((path.equals("/done") || path.equals("/delete")) && method.equals("GET")) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("id=")) {
                    try {
                        // URLに含まれるTodo番号を文字から整数に変換します。
                        int id = Integer.parseInt(query.substring(3));
                        if (path.equals("/done")) {
                            // 番号が一致するTodoを完了状態にします。
                            for (Todo todo : todos) if (todo.getId() == id) { todo.setDone(true); break; }
                        // 削除の場合は、番号が一致するTodoを一覧から取り除きます。
                        } else todos.removeIf(todo -> todo.getId() == id);
                    // 数字ではない番号が届いた場合は、その操作を無視します。
                    } catch (NumberFormatException ignored) { }
                }
                redirect(exchange);
                return;
            // /以外の知らないURLには「ページが見つかりません」と返します。
            } else if (!path.equals("/")) {
                send(exchange, "ページが見つかりません", "text/plain; charset=UTF-8");
                return;
            }

            // HTML（ブラウザーに見せるページ）を文字列として組み立てます。
            // StringBuilderは文字をつなげる道具です。ここでは画面のHTMLを順番に足します。
            StringBuilder html = new StringBuilder("<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width, initial-scale=1'><title>わたしのTodo</title>"
                    + "<style>body{max-width:720px;margin:24px auto;padding:0 16px;font:16px sans-serif}"
                    + "form{display:grid;gap:12px;padding:16px;border:1px solid #ddd;border-radius:8px}"
                    + ".days{display:flex;flex-wrap:wrap;gap:12px}li{margin:12px 0}"
                    + ".schedule{display:block;color:#555;margin-top:4px}input,select,button{font:inherit;padding:7px}</style>"
                    + "</head><body><h1>わたしのTodo</h1><form method='post' action='/add'>"
                    // inputは入力欄、selectは選択欄です。nameは送信時に使う項目名です。
                    + "<label>タスク名 <input name='todo' required></label>"
                    + "<label>繰り返し <select name='repeat'><option value='none'>繰り返さない</option>"
                    + "<option value='daily'>毎日</option><option value='weekly'>毎週</option>"
                    + "<option value='monthly'>毎月</option></select></label>"
                    + "<label>間隔（何日・何週間・何か月ごと） <input type='number' name='interval' min='1' value='1'></label>"
                    // checkboxは複数の曜日を選べるチェック欄です。
                    + "<fieldset><legend>曜日（毎週の場合）</legend><div class='days'>"
                    + day("mon", "月") + day("tue", "火") + day("wed", "水") + day("thu", "木")
                    + day("fri", "金") + day("sat", "土") + day("sun", "日")
                    + "</div></fieldset><label>日付（毎月の場合） <input type='number' name='monthday' min='1' max='31' placeholder='例: 15'></label>"
                    // dateは日付欄、timeは時刻欄です。入力値もTodoの予定として保存します。
                    + "<label>開始日 <input type='date' name='startDate'></label>"
                    + "<label>時刻 <input type='time' name='time'></label><button>追加</button></form>");
            // Todoがまだない場合は案内を出し、ある場合は1件ずつ一覧にします。
            if (todos.isEmpty()) html.append("<p>やることは、いまゼロです</p>");
            else {
                html.append("<ul>");
                for (Todo todo : todos) {
                    // 完了済みなら「済」の印を追加します。
                    html.append("<li>").append(escape(todo.getTitle()))
                            .append(todo.isDone() ? " <span style='color:#8a5a8a'>【済】</span>" : "")
                            // 予定と、完了・削除のリンクを一覧に加えます。
                            .append("<span class='schedule'>予約: ").append(escape(todo.getSchedule())).append("</span>")
                            .append(" <a href='/done?id=").append(todo.getId()).append("'>完了</a>")
                            .append(" <a href='/delete?id=").append(todo.getId()).append("'>削除</a></li>");
                }
                html.append("</ul>");
            }
            html.append("</body></html>");
            // 作ったHTMLをブラウザーへ返します。
            send(exchange, html.toString(), "text/html; charset=UTF-8");
        });
        // サーバーの待ち受けを開始します。
        server.start();
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）");
    }

    // 曜日チェック欄のHTMLを作る補助メソッドです。codeは送信値、nameは画面表示です。
    private static String day(String code, String name) {
        return "<label><input type='checkbox' name='weekday' value='" + code + "'>" + name + "</label>";
    }

    // 処理後にブラウザーをTodo一覧へ戻す転送（リダイレクト）です。
    private static void redirect(com.sun.net.httpserver.HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Location", "/");
        ex.sendResponseHeaders(303, -1);
        ex.close();
    }

    // 文字列の返事をブラウザーへ送信します。IOExceptionは通信エラーの種類です。
    private static void send(com.sun.net.httpserver.HttpExchange ex, String msg, String type) throws IOException {
        // ブラウザーへ送るため、文字をUTF-8のバイト（通信で送るデータ）に変換します。
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    // フォームの本文を読み取り、各入力項目を「名前: 値」の組にします。
    private static Map<String, String> parseForm(String body) {
        // Mapは項目名から入力値を探せる辞書のような入れ物です。
        Map<String, String> values = new LinkedHashMap<>();
        // &で分かれたフォーム項目を1つずつ読み取ります。
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            // 曜日は複数選べるので、2つ目以降は前の値につなげて保存します。
            if (key.equals("weekday") && values.containsKey(key)) values.put(key, values.get(key) + "," + value);
            else values.put(key, value);
        }
        return values;
    }

    // 頻度・曜日・日付・時刻から、一覧に表示する予定の文章を作ります。
    private static String makeSchedule(Map<String, String> f) {
        String repeat = f.getOrDefault("repeat", "none");
        // interval（間隔）が未入力や数字以外なら、初期値の1を使います。
        int interval = 1;
        try { interval = Math.max(1, Integer.parseInt(f.getOrDefault("interval", "1"))); }
        catch (NumberFormatException ignored) { }
        StringBuilder s = new StringBuilder();
        // 選ばれた頻度に応じて、毎日・毎週・毎月などの説明を組み立てます。
        if (repeat.equals("daily")) s.append(interval == 1 ? "毎日" : "毎" + interval + "日ごと");
        else if (repeat.equals("weekly")) {
            s.append(interval == 1 ? "毎週" : interval + "週間ごと");
            // フォームから届く曜日コードと、画面に表示する日本語を同じ順番で並べます。
            String[] codes = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};
            String[] names = {"月", "火", "水", "木", "金", "土", "日"};
            String selected = f.getOrDefault("weekday", "");
            boolean first = true; // 最初の曜日だけ区切り記号を変えるための目印です。
            for (int i = 0; i < codes.length; i++) if (selected.contains(codes[i])) {
                s.append(first ? " " : "・").append(names[i]);
                first = false;
            }
        } else if (repeat.equals("monthly")) {
            s.append(interval == 1 ? "毎月" : interval + "か月ごと");
            try {
                int d = Integer.parseInt(f.getOrDefault("monthday", ""));
                if (d >= 1 && d <= 31) s.append(" ").append(d).append("日");
            } catch (NumberFormatException ignored) { }
        } else {
            String date = f.getOrDefault("startDate", "");
            s.append(date.isEmpty() ? "日時未設定" : date.replace('-', '/'));
        }
        // 繰り返し予定には開始日も付け、時刻が入力されていれば末尾に加えます。
        String start = f.getOrDefault("startDate", "");
        if (!start.isEmpty() && !repeat.equals("none")) s.append("（開始 ").append(start.replace('-', '/')).append("）");
        String time = f.getOrDefault("time", "");
        if (!time.isEmpty()) s.append(" ").append(time);
        return s.toString();
    }

    // タスク名にHTMLの記号があってもページの命令として動かないよう、表示用に置き換えます。
    private static String escape(String v) {
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}

// Todo1件分の情報（番号、名前、予定、完了したか）をまとめるクラスです。
class Todo {
    // finalは作成後に変えない値、booleanは「はい/いいえ」を表す型です。
    private final int id;
    private final String title;
    private final String schedule;
    private boolean done;
    // コンストラクター（作成時の初期設定）で各情報を保存し、最初は未完了にします。
    Todo(int id, String title, String schedule) {
        this.id = id; this.title = title; this.schedule = schedule; this.done = false;
    }
    // getで始まるメソッドは、Todoの情報を外から読み取るための窓口です。
    int getId() { return id; }
    String getTitle() { return title; }
    String getSchedule() { return schedule; }
    // isDoneは完了しているかを返し、setDoneは完了状態を書き換えます。
    boolean isDone() { return done; }
    void setDone(boolean done) { this.done = done; }
}

