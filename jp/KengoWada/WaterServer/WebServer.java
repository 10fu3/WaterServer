package jp.KengoWada.WaterServer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

public class WebServer {

    List<CuttingTask> queue = new ArrayList<>();

    //強制終了時にTrueになる
    public static Boolean safetyLock = false;

    public static void stop(){
        safetyLock = true;
        spark.Spark.stop();
    }

    public void init(){

        Optional<String> optionalPort = Optional.ofNullable(System.getenv("PORT"));
        optionalPort.ifPresent(p -> {
            int port = Integer.parseInt(p);
            spark.Spark.port(port);
        });

        //ダウンロード/分割処理の終わったリクエストを返す
        spark.Spark.get("/ended",(req,res)-> {
            return String.join("<br>",queue.stream().filter(CuttingTask::isFinished)
                    .map(CuttingTask::getID)
                    .collect(Collectors.toList()));
        });

        spark.Spark.get("/get/:id",(req,res)->{
            String id = req.params(":id");
            Optional<CuttingTask> Optionalresult = queue.stream().filter(q -> q.getID().equalsIgnoreCase(id)).findAny();
            if(Optionalresult.isPresent()){
                CuttingTask result = Optionalresult.get();
                List<String> re = new LinkedList<>();
                for(Tapple<String,String> v : result.getPartOfUrlAndName()){
                    re.add(v.Left+","+v.Right);
                }

                return String.join("<br>",re);
            }else{
                return "";
            }
        });

        spark.Spark.get("/list",(req,res)->
                String.join("<br>",queue.stream().filter(q-> !q.isFinished())
                .map(CuttingTask::getID)
                .collect(Collectors.toList())));

        //ダウンロード/分割リクエストを受け付ける(POST)
        spark.Spark.post("/request",(req,res)->{
            ObjectMapper mapper = new ObjectMapper();
            try {
                //リクエストを一度Jsonに変換
                JsonNode node = mapper.readTree(req.body());
                if(node.has("url")){
                    String url = node.get("url").asText();
                    final CuttingTask queue = CuttingTask.Init(null,
                            UUID.randomUUID().toString(),
                            url);
                    this.queue.add(queue);


                    queue.startDownload();
                    System.out.println("ダウンロード開始");
                    return queue.getID();
                }

            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
            return "";
        });

        spark.Spark.get("/reset",(req,res)->{
            safetyLock = true;

            return "";
        });

        //サーバーを停止する 仮でつけたもの
        spark.Spark.get("/stop",(req,res)->{
            stop();
            return "";
        });

        System.out.println(spark.Spark.port());
    }
}