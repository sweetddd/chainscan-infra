/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.everylink.chainscan.watcher.plugin.service;
import ai.everylink.chainscan.watcher.core.config.BatchConfig;
import ai.everylink.chainscan.watcher.core.config.DataSourceEnum;
import ai.everylink.chainscan.watcher.core.config.TargetDataSource;
import ai.everylink.chainscan.watcher.core.util.SpringApplicationUtils;
import ai.everylink.chainscan.watcher.dao.BatchDao;
import ai.everylink.chainscan.watcher.entity.Batch;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.alibaba.fastjson.JSONArray;

/**
 * Batch数据服务
 *
 * @author sunny.shi@everylink.ai
 * @since 2023-06-29
 */
@Slf4j
@Service
public class BatchDataServiceImpl implements BatchDataService {

    @Resource
    private RestTemplate restTemplate;

    @Autowired
    Environment environment;

    @Autowired
    private BatchDao batchDao;


    @Override
    @TargetDataSource(value = DataSourceEnum.chainscan)
    public Long getLatestPendingBatchNum() {

        String coordinatorRpcUrl = SpringApplicationUtils.getBean(BatchConfig.class).getCoordinatorRpcUrl();

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 设置请求体
//        String json = "{\"name\": \"John\", \"age\": 30}";
//        String json = "{\"jsonrpc\": \"2.0\", \"method\": \"batch_list\", \"params\": [], \"id\": 0}";
//        HttpEntity<String> requestEntity = new HttpEntity<>(json, headers);

        JSONObject requestBody = new JSONObject();
        requestBody.put("jsonrpc", "2.0");
        requestBody.put("method", "get_latest_pending_batch_num");
        requestBody.put("params", new JSONArray());
        requestBody.put("id", 0);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            JSONObject response = restTemplate.postForObject(coordinatorRpcUrl + "/rpc", requestEntity, JSONObject.class);
            String num=response.getString("result");
            long num_long = Long.parseLong(num.substring(2), 16);
            return num_long;
        } catch (Exception e) {
            log.error("[BatchWatcher] getLatestPendingBatchNum error:", e);
            return null;
        }
    }

    @Override
    @TargetDataSource(value = DataSourceEnum.chainscan)
    public Long getLatestSubmittedBatchNum() {

        String coordinatorRpcUrl = SpringApplicationUtils.getBean(BatchConfig.class).getCoordinatorRpcUrl();

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject requestBody = new JSONObject();
        requestBody.put("jsonrpc", "2.0");
        requestBody.put("method", "get_latest_submitted_batch_num");
        requestBody.put("params", new JSONArray());
        requestBody.put("id", 0);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            JSONObject response = restTemplate.postForObject(coordinatorRpcUrl + "/rpc", requestEntity, JSONObject.class);

            String num=response.getString("result");

            long num_long = Long.parseLong(num.substring(2), 16);

            return num_long;

        } catch (Exception e) {
            log.error("[BatchWatcher] getLatestSubmittedBatchNum error:", e);
            return null;
        }
    }

    @Override
    @TargetDataSource(value = DataSourceEnum.chainscan)
    public Long getLatestFinalizedBatchNum() {

        String coordinatorRpcUrl = SpringApplicationUtils.getBean(BatchConfig.class).getCoordinatorRpcUrl();

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject requestBody = new JSONObject();
        requestBody.put("jsonrpc", "2.0");
        requestBody.put("method", "get_latest_finalized_batch_num");
        requestBody.put("params", new JSONArray());
        requestBody.put("id", 0);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            JSONObject response = restTemplate.postForObject(coordinatorRpcUrl + "/rpc", requestEntity, JSONObject.class);

            String num=response.getString("result");

            long num_long = Long.parseLong(num.substring(2), 16);

            return num_long;

        } catch (Exception e) {
            log.error("[BatchWatcher] getLatestFinalizedBatchNum error:", e);
            return null;
        }
    }

    @Override
    @TargetDataSource(value = DataSourceEnum.chainscan)
    public List<Batch> getBatchList() {
        String coordinatorRpcUrl = SpringApplicationUtils.getBean(BatchConfig.class).getCoordinatorRpcUrl();

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject requestBody = new JSONObject();
        requestBody.put("jsonrpc", "2.0");
        requestBody.put("method", "get_batch_list");
        requestBody.put("params", new JSONArray());
        requestBody.put("id", 0);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            JSONObject response = restTemplate.postForObject(coordinatorRpcUrl + "/rpc", requestEntity, JSONObject.class);

            // 解析响应并转换为自定义类型 Batch 的数组 List<Batch>
            JSONArray batchArray = response.getJSONArray("result");
            List<Batch> batches = new ArrayList<>();

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer());
            Gson gson = gsonBuilder.create();

            for (int i = 0; i < batchArray.size(); i++) {
                JSONObject batch_json_object = batchArray.getJSONObject(i);

                // 将JSON字符串解析为Batch对象

                toLong(batch_json_object,"batch_number");
                toLong(batch_json_object,"blocks");
                toLong(batch_json_object,"end_block_number");
                toLong(batch_json_object,"start_block_number");
                toLong(batch_json_object,"transactions");

                convertTime(batch_json_object,"commit_time");
                convertTime(batch_json_object,"finalized_time");
                convertTime(batch_json_object,"time");

                // 将JSON字符串解析为Batch对象
                Batch batch = gson.fromJson(batch_json_object.toString(), Batch.class);

                batches.add(batch);
            }
            return batches;
        } catch (Exception e) {
            log.error("[BatchWatcher] getLatestBatchNum error:", e);
            return null;
        }
    }


    @TargetDataSource(value = DataSourceEnum.chainscan)
    @Override
    public Batch getBatchByNum(Long num) {
        String coordinatorRpcUrl = SpringApplicationUtils.getBean(BatchConfig.class).getCoordinatorRpcUrl();

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject requestBody = new JSONObject();
        requestBody.put("jsonrpc", "2.0");
        requestBody.put("method", "get_batch_by_num");
        JSONArray jsonArray = new JSONArray();
        String num_string=num.toString();
        jsonArray.add(num_string);
        requestBody.put("params", jsonArray);
        requestBody.put("id", 0);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            JSONObject response = restTemplate.postForObject(coordinatorRpcUrl + "/rpc", requestEntity, JSONObject.class);

            JSONObject batch_json_object=response.getJSONObject("result");

            toLong(batch_json_object,"batch_number");
            toLong(batch_json_object,"blocks");
            toLong(batch_json_object,"end_block_number");
            toLong(batch_json_object,"start_block_number");
            toLong(batch_json_object,"transactions");

            convertTime(batch_json_object,"commit_time");
            convertTime(batch_json_object,"finalized_time");
            convertTime(batch_json_object,"time");


            // 创建Gson实例
//            Gson gson = new Gson();

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer());

            Gson gson = gsonBuilder.create();


            // 将JSON字符串解析为Batch对象
            Batch batch = gson.fromJson(batch_json_object.toString(), Batch.class);

            return batch;

        } catch (Exception e) {
            log.error("[BatchWatcher] getLatestBatchNum error:", e);
            return null;
        }
    }



    void toLong(JSONObject batch_json_object, String s){
        String s_string=batch_json_object.getString(s);
        long s_long = Long.parseLong(s_string.substring(2), 16);
        batch_json_object.put(s,String.valueOf(s_long));
    }

    private void convertTime(JSONObject batch_json_object,String s) {
        String s_string=batch_json_object.getString(s);
        long s_long = Long.parseLong(s_string.substring(2), 16);

        batch_json_object.put(s,String.valueOf(s_long));
    }

    @TargetDataSource(value = DataSourceEnum.chainscan)
    @Override
    public void saveBatch(Batch batch) {
        batchDao.save(batch);
    }

    @TargetDataSource(value = DataSourceEnum.chainscan)
    @Override
    public Long getMaxBatchNum() {
        Long maxBatchNum = batchDao.getMaxBatchNum();
        return maxBatchNum == null ? 0L : maxBatchNum;
    }


    @TargetDataSource(value = DataSourceEnum.chainscan)
    @Override
    public void syncBatchStatus(Long id, int status) {
        batchDao.syncBatchStatus(id,status);

    }
}