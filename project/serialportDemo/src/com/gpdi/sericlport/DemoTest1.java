package com.gpdi.sericlport;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.gpdi.utils.*;

/**
 * @program: project
 * @description:
 * @author: Mr.Wang
 * @create: 2019-01-04 09:18
 **/
public class DemoTest1 {

    public static long getNum(String num1,String num2){
        long value= Long.parseLong( num1, 16) * 256 + Long.parseLong( num2, 16);
        return value;
    }

    public static void main(String[] args){
        String[] vos = {"01","03","1C","00","07","00","00","00","10","00","25","00","0F","00","D6","01","E6","03","F6","00","00","00","00","00","00","00","00","FF","FF","FF","FF","19","E5"};
        // 风向数据
        long wind_direction = getNum(vos[3],vos[4]);
        System.out.println(wind_direction);
        // 风速数据
        long wind_speech = getNum(vos[5],vos[6]);
        System.out.println(wind_speech);
        // pm2.5
        long polutionPm2 =  getNum(vos[7],vos[8]);
        System.out.println(polutionPm2);
        // pm10
        long polutionPm10 =  getNum(vos[9],vos[10]);
        System.out.println(polutionPm10);
        // VOC
        long voc =  getNum(vos[11],vos[12]);
        System.out.println(voc);
        // 温度
        long polutionPm =  getNum(vos[13],vos[14])/10;
        System.out.println(polutionPm);
        // 湿度
        long temperature =  getNum(vos[15],vos[16])/10;
        System.out.println(temperature);
        // 大气压力
        long atmosphericPressure =  getNum(vos[17],vos[18]);
        System.out.println(atmosphericPressure);
        // 臭氧
        long ozone =  getNum(vos[19],vos[20])/1000;
        System.out.println(ozone);
        // CO
        long co =  getNum(vos[21],vos[22])/100;
        System.out.println(co);
        Map<String,Long> map = new HashMap<>();
        map.put("O3",ozone);
        map.put("PM2.5",polutionPm2);
        map.put("PM10",polutionPm10);
        Map<String,Object> uu =  AqiUtil.getAqiByPollutants(map);
        System.out.println("----------------------------------------------");
        System.out.println(uu.get("key"));
        System.out.println(uu.get("value"));
        String pollutants = (String)uu.get("key");
        Integer aqi = (Integer)uu.get("value");
        DemoTest1 demo1 = new DemoTest1();
        demo1.insertDb(wind_direction,wind_speech,polutionPm2,polutionPm10,voc,polutionPm,temperature,atmosphericPressure,ozone,co,pollutants,aqi);
    }

    public void insertDb(long wind_direction, long wind_speech, long polutionPm2, long polutionPm10, long voc, long polutionPm, long temperature, long atmosphericPressure, long ozone, long co, String pollution, Integer aqi) {
        Connection conn = null;
        PreparedStatement ps = null;
        FileInputStream in = null;
        try {
            conn = DBUtil.getConn();
            String sql = "insert into air_status (wind_direction,wind_speed,particulate_matter,particulate_matter_one,voc,weather,humidity,air_pre,ozone,carbon_monoxide,del_flag,create_time,primary_pollutants,aqi)values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            ps = conn.prepareStatement(sql);
            ps.setLong(1, wind_direction);
            ps.setLong(2, wind_speech);
            ps.setLong(3, polutionPm2);
            ps.setLong(4, polutionPm10);
            ps.setLong(5, voc);
            ps.setLong(6, polutionPm);
            ps.setLong(7, temperature);
            ps.setLong(8, atmosphericPressure);
            ps.setLong(9, ozone);
            ps.setLong(10, co);
            ps.setInt(11,0);
            Timestamp time= new Timestamp(System.currentTimeMillis());//获取系统当前时间
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timeStr = df.format(time);
            time = Timestamp.valueOf(timeStr);
            ps.setTimestamp(12,time);
            ps.setString(13,pollution);
            ps.setInt(14,aqi);
            int count = ps.executeUpdate();
            if (count > 0) {
                System.out.println("插入成功！");
            } else {
                System.out.println("插入失败！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.closeConn(conn);
            if (null != ps) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
