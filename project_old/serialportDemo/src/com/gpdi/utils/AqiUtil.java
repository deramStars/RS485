package com.gpdi.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
  * @Description:     计算aqi
  * @Author:         Hman
  * @CreateDate:    2019/1/2 16:29
  * @Version:        1.0
 */
public class AqiUtil {

    private static float[] PM_stp=new float[]{0,35,75,115,150,250,350,500};
    private static float[] SO2_stp=new float[]{0,50,150,475,800,1600,2100,2620};
    private static float[] NO2_stp=new float[]{0,40,80,180,280,565,750,940};
    private static float[] PM10_stp=new float[]{0,50,150,250,350,420,500,600};
    private static float[] O3_stp=new float[]{0,160,200,300,400,800,1000,1200};
    private static float[] CO_stp=new float[]{0,5,10,35,60,90,120,150};

    public static float[] aqi_stp=new float[]{0,50,100,150,200,300,400,500};

    public static Map<String,float[]> aqiMap=new HashMap<>();

    static {
        aqiMap.put("PM2.5",PM_stp);
        aqiMap.put("SO2",SO2_stp);
        aqiMap.put("NO2",NO2_stp);
        aqiMap.put("O3",O3_stp);
        aqiMap.put("PM10",PM10_stp);
        aqiMap.put("CO",CO_stp);
    }



    public static Map<String,Object> getAQIByPollutant(String pollutant,Long num){
         return compute(pollutant,num);
    }

    private static Map<String,Object> compute(String pollutant,Long value){
        float[] arr=aqiMap.get(pollutant);

        Integer v=0;
        // 通过传入的数组 计算对应的aqi
        for (int i=0;i<arr.length;i++){
            if(arr[i]>value){
                Float aqi=
                        (aqi_stp[i]-aqi_stp[i-1])/ (arr[i]-arr[i-1])*(value-arr[i-1])+aqi_stp[i-1];
                v= Math.round(aqi);
                break;
            }
        }

        Map<String,Object> map=new HashMap<>();

        map.put("key",pollutant);
        map.put("value",v);

        return map;
    }

    public static  Map<String,Object> getAqiByPollutants(Map<String,Long> pollutants){

        Map<String,Object> apiMap=new HashMap<>();
        apiMap.put("value",0);
        apiMap.put("key","NULL");
        Set<String> keys= pollutants.keySet();
        for (String key:keys ) {
            Map<String,Object> map= getAQIByPollutant(key,pollutants.get(key));
            if ((Integer)map.get("value")>(Integer)apiMap.get("value")){
                apiMap=map;
            }
        }
        return apiMap;
    }

    public static void main(String[] args) {
        //Map<String,Object> map=getAQIByPollutant("PM2.5",72);


    }
}
