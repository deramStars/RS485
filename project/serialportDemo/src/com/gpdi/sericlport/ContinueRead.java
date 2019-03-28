package com.gpdi.sericlport;


import gnu.io.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.gpdi.utils.*;


public class ContinueRead extends Thread implements SerialPortEventListener { // SerialPortEventListener
    // 监听器,我的理解是独立开辟一个线程监听串口数据
// 串口通信管理类
    static CommPortIdentifier portId;

    /* 有效连接上的端口的枚举 */

    static Enumeration<?> portList;
    InputStream inputStream; // 从串口来的输入流
    static OutputStream outputStream;// 向串口输出的流
    static SerialPort serialPort; // 串口的引用
    // 堵塞队列用来存放读到的数据
    private BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>();

    @Override
    /**
     * SerialPort EventListene 的方法,持续监听端口上是否有数据流
     */
    public void serialEvent(SerialPortEvent event) {//

        switch (event.getEventType()) {
            case SerialPortEvent.BI:
            case SerialPortEvent.OE:
            case SerialPortEvent.FE:
            case SerialPortEvent.PE:
            case SerialPortEvent.CD:
            case SerialPortEvent.CTS:
            case SerialPortEvent.DSR:
            case SerialPortEvent.RI:
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                break;
            case SerialPortEvent.DATA_AVAILABLE:// 当有可用数据时读取数据
                byte[] readBuffer = null;
                int availableBytes = 0;
                try {
                    availableBytes = inputStream.available();
                    while (availableBytes > 0) {
                        readBuffer = ContinueRead.readFromPort(serialPort);
                        String needData = printHexString(readBuffer);
                        System.out.println(new Date() + "真实收到的数据为：-----" + needData);
                        availableBytes = inputStream.available();
                        msgQueue.add(needData);
                    }
                } catch (IOException e) {
                }
            default:
                break;
        }
    }

    /**
     * 从串口读取数据
     *
     * @param serialPort 当前已建立连接的SerialPort对象
     * @return 读取到的数据
     */
    public static byte[] readFromPort(SerialPort serialPort) {
        InputStream in = null;
        byte[] bytes = {};
        try {
            in = serialPort.getInputStream();
            // 缓冲区大小为一个字节
            byte[] readBuffer = new byte[1];
            int bytesNum = in.read(readBuffer);
            while (bytesNum > 0) {
                bytes = MyUtils.concat(bytes, readBuffer);
                bytesNum = in.read(readBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bytes;
    }


    /**
     * 通过程序打开COM4串口，设置监听器以及相关的参数
     *
     * @return 返回1 表示端口打开成功，返回 0表示端口打开失败
     */
    public int startComPort() {
        // 通过串口通信管理类获得当前连接上的串口列表
        portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            // 获取相应串口对象
            portId = (CommPortIdentifier) portList.nextElement();

            System.out.println("设备类型：--->" + portId.getPortType());
            System.out.println("设备名称：---->" + portId.getName());
            // 判断端口类型是否为串口
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                // 判断如果COM4串口存在，就打开该串口
                if (portId.getName().equals(portId.getName())) {
                    try {
                        // 打开串口名字为COM_4(名字任意),延迟为1000毫秒
                        serialPort = (SerialPort) portId.open(portId.getName(), 1000);

                    } catch (PortInUseException e) {
                        System.out.println("打开端口失败!");
                        e.printStackTrace();
                        return 0;
                    }
                    // 设置当前串口的输入输出流
                    try {
                        inputStream = serialPort.getInputStream();
                        outputStream = serialPort.getOutputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return 0;
                    }
                    // 给当前串口添加一个监听器
                    try {
                        serialPort.addEventListener(this);
                    } catch (TooManyListenersException e) {
                        e.printStackTrace();
                        return 0;
                    }
                    // 设置监听器生效，即：当有数据时通知
                    serialPort.notifyOnDataAvailable(true);

                    // 设置串口的一些读写参数
                    try {
                        // 比特率、数据位、停止位、奇偶校验位
                        serialPort.setSerialPortParams(9600,
                                SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                                SerialPort.PARITY_NONE);
                    } catch (UnsupportedCommOperationException e) {
                        e.printStackTrace();
                        return 0;
                    }
                    return 1;
                }
            }
        }
        return 0;
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        try {
            System.out.println("--------------任务处理线程运行了--------------");
            while (true) {
                // 如果堵塞队列中存在数据就将其输出
                if (msgQueue.size() > 0) {
                    String vo = msgQueue.peek();
                    String vos[] = vo.split("  ", -1);
                    getData(vos);
                    sendOrder();
                    msgQueue.take();
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @Description: 发送获取数据指令
     * @Param:
     * @return:
     * @Author: LiangZF
     * @Date: 2019/1/3
     */
    public void sendOrder() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int i = 1;
        if (i == 1) {
            // 启动线程来处理收到的数据
            try {
                byte[] b = new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x0E, (byte) 0xC4, 0x0E};
                System.out.println("发送的数据:" + b);
                System.out.println("发出字节数：" + b.length);
                outputStream.write(b);
                outputStream.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                serialPort.close();
                e.printStackTrace();
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @Description:通过数组解析检测数据
     * @Param: [vo]
     * @return: void
     * @Author: LiangZF
     * @Date: 2019/1/4
     */
    public void getData(String[] vos) {
        // 数组不为空
        if (vos != null || vos.length != 0) {
            // 风向数据
            long wind_direction = getNum(vos[3], vos[4]);
            System.out.println(wind_direction);
            // 风速数据
            long wind_speech = getNum(vos[5], vos[6]);
            System.out.println(wind_speech);
            // pm2.5
            long polutionPm2 = getNum(vos[7], vos[8]);
            System.out.println(polutionPm2);
            // pm10
            long polutionPm10 = getNum(vos[9], vos[10]);
            System.out.println(polutionPm10);
            // VOC
            long voc = getNum(vos[11], vos[12]);
            System.out.println(voc);
            // 温度
            long polutionPm = getNum(vos[13], vos[14]) / 10;
            System.out.println(polutionPm);
            // 湿度
            long temperature = getNum(vos[15], vos[16]) / 10;
            System.out.println(temperature);
            // 大气压力
            long atmosphericPressure = getNum(vos[17], vos[18]);
            System.out.println(atmosphericPressure);
            // 臭氧
            long ozone = getNum(vos[19], vos[20]) / 1000;
            System.out.println(ozone);
            // CO
            long co = getNum(vos[21], vos[22]) / 100;
            System.out.println(co);
            Map<String, Long> map = new HashMap<>();
            map.put("O3", ozone);
            map.put("PM2.5", polutionPm2);
            map.put("PM10", polutionPm10);
            Map<String, Object> uu = AqiUtil.getAqiByPollutants(map);
            String pollutants = (String) uu.get("key");
            Integer aqi = (Integer) uu.get("value");
            insertDb(wind_direction, wind_speech, polutionPm2, polutionPm10, voc, polutionPm, temperature, atmosphericPressure, ozone, co, pollutants, aqi);
        }
    }

    // 16转10计算
    public long getNum(String num1, String num2) {
        long value = Long.parseLong(num1, 16) * 256 + Long.parseLong(num2, 16);
        return value;
    }
    /** 
    * @Description: 保存到数据库表中 
    * @Param: [wind_direction, wind_speech, polutionPm2, polutionPm10, voc, polutionPm, temperature, atmosphericPressure, ozone, co, pollution, aqi] 
    * @return: void 
    * @Author: LiangZF 
    * @Date: 2019/1/6 
    */ 
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
            ps.setInt(11, 0);
            Timestamp time = new Timestamp(System.currentTimeMillis());//获取系统当前时间
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timeStr = df.format(time);
            time = Timestamp.valueOf(timeStr);
            ps.setTimestamp(12, time);
            ps.setString(13, pollution);
            ps.setInt(14, aqi);
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

    public static void main(String[] args) {
        ContinueRead cRead = new ContinueRead();
        int i = cRead.startComPort();
        if (i == 1) {
            // 启动线程来处理收到的数据
            cRead.start();
            try {
                //根据提供的文档给出的发送命令，发送16进制数据给仪器
                byte[] b = new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x0E, (byte) 0xC4, 0x0E};
                System.out.println("发送的数据:" + b);
                System.out.println("发出字节数：" + b.length);
                outputStream.write(b);
                outputStream.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            return;
        }
    }

    // 字节数组转字符串
    private String printHexString(byte[] b) {

        StringBuffer sbf = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sbf.append(hex.toUpperCase() + "  ");
        }
        return sbf.toString().trim();
    }
}
