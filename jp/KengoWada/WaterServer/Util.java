package jp.KengoWada.WaterServer;

public class Util {
//    public static Integer getIntByStr(String word){
//        Integer value = 0;
//        Integer[] intchars = new Integer[word.toCharArray().length];
//        char[] wordArray = word.toCharArray();
//        for(int i=0; i< wordArray.length;i++) {
//            try {
//                Integer cache = Integer.valueOf(String.valueOf(wordArray[i]));
//                intchars[(wordArray.length-1)-i] = cache;
//            } catch (NumberFormatException e) {
//                System.out.println(String.valueOf(wordArray[i]) + " が変換不可");
//            }
//
//        }
//        for(int i=0; i< intchars.length;i++) {
//            value = value + (intchars[i]*pow(10,i));
//        }
//
//        if(value < 1){
//            throw new NumberFormatException("値: "+word+" はIntの対応範囲を超えています");
//        }
//
//        return value;
//    }
//
//    //10^3 → base = 10 , exp = 3
//    public static Integer pow(Integer base,Integer exp){
//        Integer value = 1;
//        if(exp < 0){
//            throw new UnsupportedOperationException("指数は0以上をサポートしています");
//        }else{
//            if(exp == 0){
//                return 1;
//            }else{
//                for (int i=0;i<exp;i++){
//                    value *= base;
//                }
//            }
//        }
//        return value;
//    }
}
