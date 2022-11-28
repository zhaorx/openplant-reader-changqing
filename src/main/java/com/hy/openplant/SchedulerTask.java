package com.hy.openplant;


import com.hy.openplant.model.Gas;
import com.hy.openplant.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SchedulerTask {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerTask.class);

    @Scheduled(fixedDelayString = "${interval}")
    public void transferSchedule() {
        logger.info("starting transfer...");
        List<Gas> list = this.getRecentGas();
        Result result = this.batchAddTaos(list);
        logger.info(result.getMessage());
//        Result result = this.addTaos(list.get(0));
//        System.out.println(result.getMessage());
    }

    private Result addTaos(Gas data) {
        String url = "http://localhost:6666/gas/add";
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        RestTemplate restTemplate = new RestTemplate();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = sdf.format(data.getTs());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("ts", dateString);
        requestBody.put("point", data.getPoint());
        requestBody.put("pname", data.getPname());
        requestBody.put("unit", data.getUnit());
        requestBody.put("region", data.getRegion());
        requestBody.put("value", data.getValue());

        HttpEntity<Map<String, Object>> r = new HttpEntity<>(requestBody, requestHeaders);

        // 请求服务端添加玩家
        Result result = restTemplate.postForObject(url, r, Result.class);

        return result;

    }

    private Result batchAddTaos(List<Gas> list) {
        String url = "http://localhost:6666/gas/batch/add";
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        RestTemplate restTemplate = new RestTemplate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        List<String> tsList = list.stream().map(item -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(item.getTs())).collect(Collectors.toList());
        List<Double> valueList = list.stream().map(Gas::getValue).collect(Collectors.toList());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("tss", tsList);
        requestBody.put("values", valueList);
        requestBody.put("point", list.get(0).getPoint());
        requestBody.put("pname", list.get(0).getPname());
        requestBody.put("unit", list.get(0).getUnit());
        requestBody.put("region", list.get(0).getRegion());
        HttpEntity<Map<String, Object>> r = new HttpEntity<>(requestBody, requestHeaders);

        // 请求服务端添加玩家
        Result result = restTemplate.postForObject(url, r, Result.class);
        return result;
    }


    public List<Gas> getRecentGas() {
        List<Gas> list = new ArrayList<>();

        try {
            Connection conn = MagusConnector.connect();
            String sql = "select * from realtime where GN='LHJ.SCADA.MES_ODR_8_3_2'";
            Statement st = null;
            st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            ResultSetMetaData md = rs.getMetaData();

            int columnCount = md.getColumnCount();


            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            while (rs.next()) { //rowData = new HashMap(columnCount);
                Gas item = new Gas();
                for (int i = 1; i <= columnCount; i++) {
                    item.setTs(sdf.parse(md.getColumnName(i)));
                    item.setValue(Double.parseDouble(md.getColumnName(i)));

                }
                list.add(item);
            }

            rs.close();
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
