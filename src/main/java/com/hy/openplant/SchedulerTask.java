package com.hy.openplant;


import com.hy.openplant.model.Gas;
import com.hy.openplant.model.Result;
import com.magus.net.IOPConnect;
import com.magus.net.OPDynamicData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SchedulerTask {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerTask.class);

    @Value("${push-url}")
    private String pushUrl;
    @Value("${points:}")
    private String[] points;

    @Resource
    MagusConnector connector;

    @Scheduled(fixedDelayString = "${interval}")
    public void transferSchedule() {
        logger.info("starting transfer...");
        List<Gas> list = this.getRecentGas();
        Result result = this.batchAddTaos(list);
        logger.info(result.getMessage());
    }

    public List<Gas> getRecentGas() {
        List<Gas> list = new ArrayList<>();

//        String[] pointNames = new String[] { POINT_AX_GLOBAL_NAME, POINT_DX_GLOBAL_NAME };

        // 取实时
        IOPConnect conn = connector.getConnect();
        Map<String, OPDynamicData> result = null;
        try {
            result = conn.getPointDynamicDatas(points);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 输出获取到的数据
        Date now = new Date();
        for (String key : result.keySet()) {
            Gas item = new Gas();
            OPDynamicData dynData = result.get(key);

            item.setTs(now);
            item.setValue(dynData.getAV());

            list.add(item);
        }

        logger.info("operate success!");
        connector.freeConnect(conn);

        return list;
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
        Result result = restTemplate.postForObject(pushUrl, r, Result.class);
        return result;
    }


}
