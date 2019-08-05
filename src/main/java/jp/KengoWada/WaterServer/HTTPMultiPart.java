package jp.KengoWada.WaterServer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

class WaitingUploadData{
    //ダウンロードを管理するクラス
    CuttingTask q = null;

    byte[] data = null;
    Integer num = 0;

    public static WaitingUploadData create(CuttingTask q, byte[] data, Integer num){
        WaitingUploadData qud = new WaitingUploadData();
        qud.q = q;
        qud.data = data;
        qud.num = num;
        return qud;
    }
}

public class HTTPMultiPart {

    //シングルトン
    public static HTTPMultiPart self = new HTTPMultiPart();


    private Queue<WaitingUploadData> WaitingUpload = new LinkedBlockingQueue<>();

    private Timer timer = new Timer();

    //他のスレッドからアクセスされることを考慮して、キューの追加、取り出しと参照は同期制御する
    private synchronized WaitingUploadData pollQ(){
        return WaitingUpload.poll();
    }
    public synchronized void addQ(WaitingUploadData q){
        //強制終了時
        if(WebServer.safetyLock){
            return;
        }
        WaitingUpload.add(q);
    }
    public synchronized WaitingUploadData peekQ(){
        return WaitingUpload.peek();
    }

    public HTTPMultiPart(){
        Thread thread = new Thread(()->{
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    //先頭のキューを参照（削除されない）
                    final WaitingUploadData q = pollQ();
                    //もしリストが空なら何もしない
                    if(q == null){
                        return;
                    }
                    //強制終了時
                    if(WebServer.safetyLock){
                        timer.cancel();
                        return;
                    }
                    System.out.println("残っているタスク "+String.valueOf(WaitingUpload.size()));
                    Thread thread = new Thread(()->{
                        try {
                            q.q.onUploaded(sendData(q.data, q.num),q.data,q.num);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    });
                    thread.start();
                }
            };

            timer.schedule(task,0,10000);
        });
        thread.start();
    }

    private static String sendData(byte[] bytes,Integer num) throws IOException {

        //FileIO(API)に対して
        HttpPost request = new HttpPost("https://file.io/");
        HttpClient client = HttpClientBuilder.create().build();
        //マルチパート形式のデータを送る
        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("file",bytes,ContentType.DEFAULT_BINARY,"file-"+String.valueOf(num))
                .build();
        request.setEntity(entity);
        //ここで送信
        HttpResponse response = client.execute(request);


        BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        //読み取ったJSONを格納する 念の為StringではなくStringBuilderで実装
        StringBuilder sb = new StringBuilder();
        String line;
        //レスポンスを読み取る
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }
}
