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
    @Value("${push-multi-url}")
    private String pushMultiUrl;
    @Value("${points:}")
    private String[] points;
    @Value("${region}")
    private String region;
    private String sep = "_";

    @Resource
    MagusConnector connector;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Scheduled(fixedDelayString = "${interval}")
    public void transferSchedule() {
        logger.info("starting transfer...");
        List<Gas> list = this.getRecentGas();
        if (list.size() > 0) {
            Result result = this.addMultiTaos(list);
            logger.info(result.getMessage());
        } else {
            logger.info("getRecentGas no data");
        }
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
        String dateStr = sdf.format(new Date());
        for (String key : result.keySet()) {
            OPDynamicData dynData = result.get(key);

            Gas g = new Gas();
            g.setTs(dateStr);
            g.setRegion(region);
            // !taos表名不允许有'.' 所以替换成'_'
            String _key = key.replaceAll("\\.", "_");
            g.setPoint(region + sep + _key);
            g.setValue(dynData.getAV());

            list.add(g);

            logger.debug("######get_data1:" + dynData);
            logger.debug("######get_data:" + g.toString());
        }

        logger.info("operate success!");
        connector.freeConnect(conn);

        return list;
    }

    public Result addMultiTaos(List<Gas> list) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("gasList", list);

        HttpEntity<Map<String, Object>> r = new HttpEntity<>(requestBody, requestHeaders);
        Result result = restTemplate.postForObject(pushMultiUrl, r, Result.class);

        return result;
    }

    private Result addTaos(Gas data) {
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
        Result result = restTemplate.postForObject(pushUrl, r, Result.class);

        return result;

    }

}
