package jp.KengoWada.WaterServer;


public class Main {
    //エントリーポイント
    static WebServer webServer = null;

    public static void main(String... args){
        webServer = new WebServer();
        webServer.init();
    }
}
