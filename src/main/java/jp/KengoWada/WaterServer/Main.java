package jp.KengoWada.WaterServer;


import spark.Spark;

public class Main {
    //エントリーポイント
    static WebServer webServer = null;

    public static void main(String... args){
        webServer = new WebServer();
        webServer.init();
    }

    public static void restart(){
        Spark.stop();
        webServer = new WebServer();
        webServer.init();
    }
}
