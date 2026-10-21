public class Items { // Itemsという名前のクラス（コードをまとめる箱）を作ります
    public static void main(String[] args) { // プログラムを実行したとき、最初に動く場所です
        String[] todos = { "牛乳を買う", "", "パンを買う", "掃除をする" }; // Todoの文字列4件をtodosという配列（複数の値を並べたもの）に入れます
        boolean[] done = { true, false, false, false }; // Todoごとの完了状態をdoneという配列に入れます
        for (int i = 0; i < todos.length; i++) { // iを使って、配列の最初から最後まで1件ずつ取り出します
            if (!todos[i].isEmpty()) { // 項目が空の文字列ではないときだけ、次の行を実行します
                String mark = ""; // 済んでいない項目には、最初は印を付けません
                if (done[i]) { // 同じ番号のdoneがtrueなら、済んだ項目として扱います
                    mark = "[済] "; // 済んだ項目に付ける印を用意します
                } // 済んだ項目の印付けをここで終えます
                System.out.println("<li>" + mark + todos[i] + "</li>"); // 印とTodoを<li>と</li>で囲んで1行に出力します
            } // 空でない項目の出力をここで終えます
        } // for文の繰り返しをここで終えます
    } // mainメソッド（実行の開始場所）をここで終えます
} // Itemsクラスをここで終えます
