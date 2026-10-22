import java.util.ArrayList; // ArrayList（追加できるリスト）を使う準備
import java.util.List; // List（複数の値を順番に並べる入れ物）を使う準備

class Todo { // Todo（やること1件）の設計図
    String title; // title（タイトル）に、やることの名前を保存
    boolean done; // done（完了したか）に、true または false を保存

    Todo(String title, boolean done) { // Todo を作るときにタイトルと完了状態を受け取る
        this.title = title; // 受け取ったタイトルを、この Todo の title に保存
        this.done = done; // 受け取った完了状態を、この Todo の done に保存
    } // Todo を作る処理の終わり

    String toItem() { // Todo を HTML の li 形式の文字列に変える処理
        if (done) { // done が true（完了）なら
            return "<li>[完了] " + title + "</li>"; // 完了の印を付けた1行を返す
        } // 完了かどうかの確認の終わり
        return "<li>" + title + "</li>"; // 未完了なら印なしの1行を返す
    } // toItem の処理の終わり
} // Todo クラスの終わり

public class Main { // Main（プログラムの開始点）という名前のクラス
    public static void main(String[] args) { // Java が最初に実行する main メソッド
        List<Todo> todos = new ArrayList<>(); // Todo を並べる空のリストを作る
        todos.add(new Todo("牛乳を買う", true)); // 未完了の「牛乳を買う」をリストに追加
        todos.add(new Todo("ゴミを出す", true)); // 完了した「ゴミを出す」をリストに追加
        todos.add(new Todo("勉強をする", true));

        for (Todo todo : todos) { // リストの Todo を先頭から1件ずつ取り出す
            System.out.println(todo.toItem()); // 変換した1行をターミナルに表示
        } // for（繰り返し）の終わり
    } // main メソッドの終わり
} // Main クラスの終わり
