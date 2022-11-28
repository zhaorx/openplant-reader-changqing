package com.hy.openplant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class MagusConnector {
    @Value("${magus.magus.address}")
    private static String address;
    @Value("${magus.magus.user}")
    private static String user;
    @Value("${magus.magus.passwd}")
    private static String passwd;
    public static String className = "com.magus.jdbc.Driver";
    // jdbc:产品系列://IP:端口/服务
    // jdbc:产品系列://IP:端口/
    private static ApplicationContext ctx = new ClassPathXmlApplicationContext("mesconfig.xml");
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static Connection connect() {
        Connection conn = null;
        try {
            String url = "jdbc:openplant://" + address + "/RTDB";
            Class.forName(className);//反射，使用装载当前类的类装载器来装载指定的类并且对其进行实例化
            conn = DriverManager.getConnection(url, user, passwd);
        } catch (Exception e) {
            System.out.println("Connect faild: " + e.toString());
        }
        return conn;
    }

    public static boolean executeSQL(String sql) {
        try {
            Connection conn = connect();
            Statement st = conn.createStatement();
            boolean success = st.execute(sql);
            st.close();
            conn.close();
            return success;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 向scada实时表更新对应点名(gn)的值(av)
     **/
    public boolean setGnValue(String gn, String value) {
        try {
            String sql = "insert into  realtime (AV, GN) values ('" + value + "','" + gn + "')";
            Connection conn = connect();
            Statement st = conn.createStatement();
            boolean success = st.execute(sql);
            st.close();
            conn.close();
            return success;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getGnValue(String gn) {
        String value = "";
        try {
            String sql = "select AV from realtime where GN='" + gn + "'";
            Connection conn = connect();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                value = rs.getString(1);
            }
            rs.close();
            st.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public boolean setValueIOserver(String value) {
        String sql = "insert into  realtime (AV, GN) values ( '" + value + "','LHJ.MSG.MSG_TO_IOSERVER')";
        return executeSQL(sql);
    }

    //查询IOserver值是否为空
    public boolean isIOserverNull() {
        boolean flag = false;
        String sql = "select AV from realtime where GN='LHJ.MSG.MSG_TO_IOSERVER'";
        try {
            Connection conn = connect();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                if (rs.getString(1).equals("")) {
                    flag = true;
                }
            }
            rs.close();
            st.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    //获取某个点的历史数据
    public List<String[]> getGnHisData(String gn, String startTime, String endTtme) {
        List<String[]> list = new ArrayList<>();
        String sql = "select AV,TM from Archive where mode='raw' and interval='1h' and GN='" + gn + "' and TM between '" + startTime + "' and '" + endTtme + "'";
        try {
            Connection conn = connect();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                String[] strs = new String[2];
                strs[0] = rs.getString(1);
                strs[1] = rs.getString(2);
                list.add(strs);
            }
            rs.close();
            st.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    //对查询结果进行整理
    private static void showResultSet(ResultSet rs) throws SQLException, InterruptedException {
        ResultSetMetaData rsmd = rs.getMetaData();
        String formatStr = "%-20s";
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            System.err.print(String.format(formatStr, rsmd.getColumnLabel(i).toUpperCase()) + "\t");
        }
        System.err.print("\n");
        Thread.sleep(100);
        while (rs.next()) {
            StringBuffer sb = new StringBuffer();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                if (rsmd.getColumnType(i) == Types.DATE) {
                    sb.append(sdf.format(rs.getDate(i)) + "\t");
                } else if (rsmd.getColumnType(i) == Types.BOOLEAN) {
                    sb.append(rs.getBoolean(i) + "\t");
                } else {
                    sb.append(String.format(formatStr, rs.getString(i)) + "\t");
                }
            }
            System.out.println(sb.toString());
        }

    }


    public static void main(String[] args) {
        try {
            Connection conn = connect();
            String sql = "select * from realtime where GN='LHJ.SCADA.MES_ODR_8_3_2'";
            Statement st = null;
            st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            showResultSet(rs);
            rs.close();
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
