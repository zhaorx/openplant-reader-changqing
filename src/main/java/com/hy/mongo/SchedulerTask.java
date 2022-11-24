package com.hy.mongo;


import com.hy.mongo.model.Gas;
import com.hy.mongo.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SchedulerTask {


    @Resource
    private MongoTemplate mongoTemplate;

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
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);

        Query query = new Query(Criteria
                .where("ts").gt(cal.getTime()));
        List<Gas> list = mongoTemplate.find(query, Gas.class);
        return list;
    }
}
