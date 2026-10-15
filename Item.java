public class Item { // Itemという名前のクラス（プログラムの入れ物）を作ります。
    public static void main(String[] args) { // プログラムを起動したとき最初に動く場所です。
        String title = "授業を受ける"; // titleという文字列の変数（値を入れる箱）に「牛乳を買う」を入れます。
        String html = "<li>" + title + "</li>"; // +で文字列をつなぎ、1行分のHTML（Webページ用の文字）を作ります。
        System.out.println(html); // 作った文字列をターミナル（文字で操作する画面）に1行表示します。
        boolean done = false;
        System.out.println(done);
        int count = 3;
        System.out.println("いま" + count + "件");
    } // mainメソッド（処理のまとまり）をここで終わります。
} // Itemクラスをここで終わります。
