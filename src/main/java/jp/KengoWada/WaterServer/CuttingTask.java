package jp.KengoWada.WaterServer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.Supplier;

//サーバー内でリクエストを受けたURLの状況を管理するためのクラス
public class CuttingTask {
    private Integer partNumber = 0; //Fileサイズを把握するための変数
    private Runnable onFinished = null;//ダウンロードが終わったときに処理されるクロージャ 未使用
    private String ID = UUID.randomUUID().toString();//サーバー内での識別コード
    private List<String> partOfUrl = new ArrayList<>();//ファイルを分割したときのダウンロードURL
    private Boolean Finished = false;//ダウンロードが完了しているか
    private String TargetUrl = "";//ダウンロード先のURL

    //private String FileName = "";

    public static CuttingTask Init(Runnable onFinished, String ID, String TargetUrl){
        CuttingTask queue = new CuttingTask();
        System.out.println("分割するファイルのURLとして次のURLが指定されました "+TargetUrl);
        return queue
                .setOnDownloaded(onFinished)
                .setID(ID)
                .setTargetUrl(TargetUrl);
    }

    //アップロードされたあとの処理
    public CuttingTask onUploaded(String response, byte[] uploaded, Integer num){
        if(WebServer.safetyLock){
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();

        try {
            //リクエストを一度Jsonに変換
            System.out.println(response);
            //まれにあるアップロード時に502を返す現象　リトライにより対処
            if(response.equalsIgnoreCase("<html><head><title>502 Bad Gateway")){
                this.uploadFile(uploaded,num);
            }
            //JSONをMapに変換
            JsonNode node = mapper.readTree(response);
            //アップロード頻度によって現れるアップロードしすぎメッセージ
            if(node.has("error")){
                //もう一度アップロードを申請しなおす
                this.uploadFile(uploaded,num);
            }else{
                //ダウンロードに必要なURLをレスポンスJSONから回収
                partOfUrl.add(node.get("link").asText());
                //もし分割回数とダウンロードURLのリストサイズが一致したとき、ダウンロードが終了したものとみなす
                if(this.partNumber == partOfUrl.size()){
                    this.setFinished(true);
                }

            }
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
        return this;
    }

    //アップロードをする
    public CuttingTask uploadFile(byte[] data, Integer num){
        System.out.println("Upload start1");
        //アップロード対象のキューに追加する
        HTTPMultiPart.self.addQ(WaitingUploadData.create(this,data,num));
        System.out.println("Upload start2");
        return this;
    }

    //分割対象のファイルをダウンロードする 次のページのコードを参考にした https://www.yoheim.net/blog.php?q=20111119
    public CuttingTask startDownload(){
        System.out.println("Download start1");
        if(!Finished){
            System.out.println("Download start2");
            Thread thread = new Thread(()->{
                //強制終了時
                if(WebServer.safetyLock){
                    return;
                }
                //何回ファイルを分割したのかというカウンタ
                int count = 0;
                try {
                    System.out.println("Download start3");
                    //文字列からURLに変換
                    URI uri = URI.create(getTargetUrl());
                    URL url = uri.toURL();
                    //以下HTTPのGET処理 次のページを参照した https://www.yoheim.net/blog.php?q=20111119
                    URLConnection urlcon =url.openConnection();
                    InputStream fileIS =urlcon.getInputStream();
                    ByteArrayOutputStream bais = new ByteArrayOutputStream();
                    System.out.println("Download start4");
                    int c;
                    //順に読み出していく
                    while((c =fileIS.read()) != -1){
                        bais.write((byte) c);
                        //1MBでカット
                        if(bais.size() == 1048576){
                            //分割回数をカウントする
                            count += 1;
                            System.out.println("Download start8");
                            bais.close();
                            //アップロード処理の開始
                            if(WebServer.safetyLock){
                                return;
                            }
                            this.uploadFile(bais.toByteArray(),count);
                            //バイト配列を初期化
                            bais = new ByteArrayOutputStream();
                            System.out.println("Download start9");
                        }
                    }
                    //もし残りのバイト配列が１MB未満の場合、アップロードされるようにする
                    if(1048576 > bais.size()){
                        count += 1;
                        this.uploadFile(bais.toByteArray(),count);
                    }

                    bais.close();
                    fileIS.close();
                    System.out.println("Download start6");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.partNumber = count;
                System.out.println("分割回数は"+String.valueOf(count));
            });

            //他スレッドによる処理を開始する
            thread.start();
        }
        return this;
    }

    public String getID() {
        return ID;
    }

    public CuttingTask setID(String ID) {
        this.ID = ID;
        return this;
    }

    public List<String> getPartOfUrl() {
        return partOfUrl;
    }

    public CuttingTask setPartOfUrl(List<String> partOfUrl) {
        this.partOfUrl = partOfUrl;
        return this;
    }

    public Boolean isFinished() {
        return Finished;
    }

    public CuttingTask setFinished(Boolean finished) {
        Finished = finished;
        if(finished && onFinished != null){
            //終了時に実行
            onFinished.run();
        }
        return this;
    }

    public String getTargetUrl() {
        return TargetUrl;
    }

    public CuttingTask setTargetUrl(String targetUrl) {
        TargetUrl = targetUrl;


        //URLからファイル名を抽出するクロージャ　未使用
        Supplier<String> getFileNameLamda = ()->{
            String[] array = getTargetUrl().split("/");
            if(array[array.length-1].length() == 0 && array.length > 1){
                return array[array.length-2];
            }
            return array[array.length-1];
        };

         //this.FileName = getFileNameLamda.get();

        return this;
    }

    public CuttingTask setOnDownloaded(Runnable runnable){
        this.onFinished = runnable;
        return this;
    }


}
