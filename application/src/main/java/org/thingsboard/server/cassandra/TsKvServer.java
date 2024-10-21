package org.thingsboard.server.cassandra;/*
 * @Author:${zhangrui}
 * @Date:2020/9/28 17:50
 */

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.thingsboard.server.Response.IndexOpenResponse;
import org.thingsboard.server.Response.NewResponse;
import org.thingsboard.server.Response.WeatherResponse;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.model.sql.TenantPerssionChangeTableEntity;
import org.thingsboard.server.dao.sql.tenantPerssionChange.TenantPerssionChangeRepository;
import org.thingsboard.server.service.execl.DemoExport;
import org.thingsboard.server.service.execl.ExeclExportUtils;
import org.thingsboard.server.utils.DateUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.cassandra.core.query.Criteria.where;

@Slf4j
@Service
public class TsKvServer {


    @Autowired
    private CassandraTemplate cassandraTemplate;

    @Autowired
    private TenantPerssionChangeRepository tenantPerssionChangeRepository;


    public List<TsKvHourHistoryEntity> findAll() {
        List<TsKvHourHistoryEntity> entityList = cassandraTemplate.select(
                Query.query(where("ts").gt(0)).withAllowFiltering(), TsKvHourHistoryEntity.class);
        return entityList;

    }


    public List<TsKvCfEntity> findAll1(UUID id, Long startTime, Long endTime, String type) {
        List<TsKvCfEntity> entityList = cassandraTemplate.select(
                Query.query(where("entity_id").is(id)).and(where("ts").gt(startTime)).and(where("ts").lt(endTime)).withAllowFiltering(), TsKvCfEntity.class);
        return entityList;

    }


    /***
     * 时间范围查询
     * @param id
     * @param startTime
     * @param endTime
     * @return
     */
    public List<TsKvCfEntity> findAllBetweenTime(UUID id, Long startTime, Long endTime,String type) {

        List<TsKvCfEntity> entityList = cassandraTemplate.select(
                Query.query(where("entity_id").is(id)).and(where("key").is(type)).and(where("ts").gt(startTime)).and(where("ts").lt(endTime)).withAllowFiltering(), TsKvCfEntity.class);
        return entityList;

    }

    /**
     * 封装成数据返回
     *
     * @return
     */
    public List<DemoExport> toExport(List<DeviceEntity> entityList) {
        List<DemoExport> demoExportList = new LinkedList<>();

        Long time = DateUtils.getDayTime();

        DateUtils.getLastMonthEndTime();
        /***
         * 循环封装
         */


        entityList.stream().forEach(p -> {


            if ("月均值".equals(p.getLabel()) || "实时值".equals(p.getLabel())) {

            } else {

                try {
                    for (Long i = DateUtils.getLastMonthStartTime(); i < DateUtils.getLastMonthEndTime(); i = i + time) {
                        DemoExport demoExport = new DemoExport();
                        demoExport.setPointPosition(p.getLabel());
                        demoExport.setDate(DateUtils.getDate(i));
                        demoExport.setDateTime(i);
                        demoExport.setUuid(p.getId());
                        demoExportList.add(demoExport);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }


        });

        return demoExportList;

    }

    /**
     * 循环查询出需要导出的数据
     *
     * @param id
     * @param date
     * @return
     * @throws Exception
     */
    public List<TsKvCfEntity> findAll(UUID id, String date) throws Exception {

        Long start = DateUtils.getDayStartWorkTime(date);

        Long end = DateUtils.getDayEndWorkTime(date);

        Long startTime = System.currentTimeMillis();

        List<TsKvCfEntity> entityList = cassandraTemplate.select(
                Query.query(where("ts").lt(end))
                        .and(where("ts").gt(start)).and(where("entity_id").is(id)).withAllowFiltering(), TsKvCfEntity.class);

        Long endTime = System.currentTimeMillis();
        log.info("time" + (endTime - startTime));

        return entityList;

    }


    /***
     * 前期用于数据的导出的
     * @param id
     * @param type
     * @return
     * @throws Exception
     */
    public List<TsKvCfEntity> findAll1(UUID id, String type) throws Exception {

        List<TsKvCfEntity> entityList = cassandraTemplate.select(
                Query.query(where("entity_id").is(id)).and(where("key").is(type))
                        .and(where("ts").lt(1604851200000l))
                        .and(where("ts").gt(1588262400000l))
                        .withAllowFiltering(), TsKvCfEntity.class);

        return entityList;

    }


    public List<TsKvCfEntity> toto() throws Exception {

        List<TsKvCfEntity> entityList = cassandraTemplate.select(
                Query.query(where("ts").lt(1608134400000l))
                        .withAllowFiltering(), TsKvCfEntity.class);
        return entityList;
    }


    /***
     *
     * @param id
     * @return
     */
    public List<Map<String,String>> getList(String id){

        UUID uuid = UUID.fromString(id);
        List<Map<String,String>> mapList = new LinkedList<>();


        for (long i = 1611158400000l; i <= System.currentTimeMillis() ; i=i+ DateUtils.getHourTime()) {
            Map<String,String> map = new HashMap<>();
            List<TsKvCfEntity> entityList = cassandraTemplate.select(
                    Query.query(where("entity_id").is(uuid)).and(where("key").is("PM2.5"))
                            .and(where("ts").lt(i+ DateUtils.getHourTime()))
                            .and(where("ts").gt(i))
                            .withAllowFiltering(), TsKvCfEntity.class);
            System.out.println(entityList.size());
            Optional<TsKvCfEntity> entity = entityList.stream().max(Comparator.comparingLong(TsKvCfEntity::getTs));

            map.put(DateUtils.getDateForHour(entity.get().getTs()),entity.get().getLongV()+"");
            mapList.add(map);
        }
        return mapList;
    }



    /***
     * 统计昨天的数据
     * @param id
     * @return
     */
    public List<TsKvCfEntity> findByDay(UUID id, String type) throws InterruptedException {
        List<TsKvCfEntity> list = new LinkedList<>();
        Long startTime = DateUtils.getLastStartTime();
        Long endTime = DateUtils.getLastEndTime();
        boolean flag = true;
        while (flag) {
            try {
                list = cassandraTemplate.select(
                        Query.query(where("entity_id").is(id)).and(where("key").is(type))
                                .and(where("ts").lt(endTime))
                                .and(where("ts").gt(startTime))
                                .withAllowFiltering(), TsKvCfEntity.class);
                flag = false;
            } catch (Exception e) {
                Thread.sleep(DateUtils.getHourTime() / 60);
                flag = true;
            }
        }
        return list;
    }


    public List<TsKvCfEntity> findByLastDay(UUID id, String type) {

        Long startTime = DateUtils.getLastStartTime();
        Long endTime = DateUtils.getLastEndTime();
        return cassandraTemplate.select(
                Query.query(where("entity_id").is(id)).and(where("key").is(type))
                        .and(where("ts").lt(endTime))
                        .and(where("ts").gt(startTime))
                        .withAllowFiltering(), TsKvCfEntity.class);

    }


    /**
     * 循环查询出需要导出的数据
     *
     * @param id
     * @param id
     * @return
     * @throws Exception
     */
    public List<TsKvHourHistoryEntity> findForOne(String id) throws Exception {

        Long endTime = DateUtils.getLastMonthEndTime();
        Long startTime = DateUtils.getLastMonthStartTime();
        List<TsKvHourHistoryEntity> list = cassandraTemplate.select(
                Query.query(where("ts").lt(endTime))
                        .and(where("ts").gt(startTime)).and(where("entity_id").is(id)).withAllowFiltering(), TsKvHourHistoryEntity.class);
        log.info("time" + (endTime - startTime));
        return list;

    }


    /***
     * 获取不同类型的数据
     * @return
     */
    public List<Double> getType(List<TsKvHourHistoryEntity> entityList, String type) {
        ExeclExportUtils execlExportUtils = new ExeclExportUtils();
        List<Double> doubleList = execlExportUtils.getList(entityList, type);
        return doubleList;
    }


    public void add(TsKvHourHistoryEntity ts) {
        cassandraTemplate.insert(ts);
    }


    /***
     * 上海
     *查询最新的天气
     */
    public WeatherResponse findWetather(String name) {

        TenantPerssionChangeTableEntity tableEntity = tenantPerssionChangeRepository.findByUsername(name).get(0);

        UUID uuid = UUID.fromString(tableEntity.getWeatherId());
        List<TsKvLatestcfEntity> list = cassandraTemplate.select(
                Query.query(where("entity_id").is(uuid))
                        .withAllowFiltering(), TsKvLatestcfEntity.class);

        WeatherResponse weatherResponse = new WeatherResponse();


        list.stream().forEach(p -> {

            if (p.getKey().equals("tem1")) {
                weatherResponse.setMaxTem(p.getStrV() );
            }

            if (p.getKey().equals("tem2")) {
                weatherResponse.setMinTem(p.getStrV());
            }

            if (p.getKey().equals("flag")) {
                weatherResponse.setWeatherLevel(p.getLongV().intValue());
                weatherResponse.setWeatherDesc(getWeatherValue(p.getLongV().intValue()));
            }

            if (p.getKey().equals("air")) {
                weatherResponse.setAirLevel(p.getLongV().intValue());
                weatherResponse.setAirLevelDesc(getAirDesc(p.getLongV().intValue()));
            }
        });


        return weatherResponse;
    }


    /***
     * 查询
     * @return
     */
    public List<TsKvLatestcfEntity> findByList(UUID uuid) {
        List<TsKvLatestcfEntity> list = cassandraTemplate.select(
                Query.query(where("entity_id").is(uuid)).withAllowFiltering(), TsKvLatestcfEntity.class);
        return list;
    }


    /***
     * 获取到
     * @return
     */
    public  Map<String,List<List<Long>>> getIndexOpenResponses(List<TsKvLatestcfEntity> latestcfEntityList){

        /***
         * 封装数据
         */
        Map<String,List<List<Long>>> map = new HashMap<>();
        for (TsKvLatestcfEntity tsKvLatestcfEntity : latestcfEntityList) {

            List<Long> longList = new LinkedList<>();
            longList.add(tsKvLatestcfEntity.getTs());
            longList.add(tsKvLatestcfEntity.getDblV() != null ? tsKvLatestcfEntity.getDblV():tsKvLatestcfEntity.getLongV()  );

            List<List<Long>> dataList = new LinkedList<>();
            dataList.add(longList);
            map.put(tsKvLatestcfEntity.getKey(),dataList);
        }
        return map;
    }

    /***
     *查询1分钟前的数据
     * @return
     */
    public List<Object> findForDetailLimit1(UUID uuid, String type) {
        Long startTime = System.currentTimeMillis() - 2 * 60 * 1000;
        Long endTime = System.currentTimeMillis();
        List<TsKvCfEntity> listAll = cassandraTemplate.select(
                Query.query(where("ts").lt(endTime))
                        .and(where("ts").gt(startTime)).and(where("entity_id").is(uuid)).and(where("key").is(type)).withAllowFiltering(), TsKvCfEntity.class);
        return this.orderData(listAll);
    }


    /**
     * 详情的数据封装
     *
     * @return
     */
    private List<Object> orderData(List<TsKvCfEntity> list) {
        list = list.stream().sorted(Comparator.comparing(TsKvCfEntity::getTs)).collect(Collectors.toList());
        List<Object> values = new LinkedList<>();
        list.stream().forEach(p -> {
            Object[] value = new Object[2];
            value[0] = p.getTs();
            value[1] = p.getLongV();
            values.add(value);
        });
        return values;
    }


    /**
     * 获取空气的质量
     *
     * @return
     */
    private String getAirDesc(Integer air) {
        if (air <= 50) {
            return "优";
        } else if (50 < air && air <= 100) {
            return "良";
        } else if (100 < air && air <= 150) {
            return "轻微污染";
        } else if (150 < air && air <= 250) {
            return "轻度污染";
        } else if (250 < air && air <= 300) {
            return "中度污染";
        } else if (air > 300) {
            return "重污染";
        }
        return "获取失败";
    }


    private String getWeatherValue(Integer flag) {


        switch (flag) {
            case 1:
                return "雪";
            case 2:
                return "雷阵雨";
            case 3:
                return "沙尘";
            case 4:
                return "雾";
            case 5:
                return "冰雹";
            case 6:
                return "多云";
            case 7:
                return "雨";
            case 8:
                return "阴";
            case 9:
                return "晴";
            default:
                return "获取失败";

        }
    }


    /***
     * 查询污染物
     */
    public List<Object> getWarnListForName(String projectName) {
        Long startTime = System.currentTimeMillis() - 3 * 60 * 1000;
        Long endTime = System.currentTimeMillis();
        TenantPerssionChangeTableEntity tableEntity = tenantPerssionChangeRepository.findByProjectName(projectName);
        UUID uuidString = UUID.fromString(tableEntity.getSumId());
        List<TsKvCfEntity> listAll = cassandraTemplate.select(
                Query.query(where("ts").lt(endTime))
                        .and(where("ts").gt(startTime)).and(where("entity_id").is(uuidString)).and(where("key").is("PM2.5")).withAllowFiltering(), TsKvCfEntity.class);
        return this.orderData(listAll);
    }

    /***
     * 查询污染物
     */
    public List<Object> getWarnList(String uuid) {
        Long startTime = System.currentTimeMillis() - 3 * 60 * 1000;
        Long endTime = System.currentTimeMillis();
        TenantPerssionChangeTableEntity tableEntity = tenantPerssionChangeRepository.findByTenantId(uuid).get(0);

        UUID uuidString = UUID.fromString(tableEntity.getSumId());
        List<TsKvCfEntity> listAll = cassandraTemplate.select(
                Query.query(where("ts").lt(endTime))
                        .and(where("ts").gt(startTime)).and(where("entity_id").is(uuidString)).and(where("key").is("PM2.5")).withAllowFiltering(), TsKvCfEntity.class);
        return this.orderData(listAll);
    }


    public NewResponse getPm25New(String uuid) {
        TenantPerssionChangeTableEntity tableEntity = tenantPerssionChangeRepository.findByTenantId(uuid).get(0);
        UUID uuidString = UUID.fromString(tableEntity.getSumId());
        NewResponse newResponse = new NewResponse();
        /***
         * PM2.5下的
         */
        List<TsKvLatestcfEntity> tsKvLatestcfEntity = cassandraTemplate.select(
                Query.query(where("entity_id").is(uuidString)).and(where("key").is("PM2.5")).limit(1).withAllowFiltering(), TsKvLatestcfEntity.class);
        try {
            newResponse.setPm25(tsKvLatestcfEntity.get(0).getLongV());
        } catch (Exception e) {
            newResponse.setPm25(0l);
        }

        /***
         * AQI下的
         */
        List<TsKvLatestcfEntity> tsKvLatestcfEntity1 = cassandraTemplate.select(
                Query.query(where("entity_id").is(uuidString)).and(where("key").is("AQI")).limit(1).withAllowFiltering(), TsKvLatestcfEntity.class);
        try {
            newResponse.setAQI(tsKvLatestcfEntity1.get(0).getLongV());
        } catch (Exception e) {
            newResponse.setAQI(0l);
        }
        return newResponse;
    }


    public NewResponse getPm25NewForName(String name) {


        TenantPerssionChangeTableEntity tableEntity = tenantPerssionChangeRepository.findByProjectName(name);
        UUID uuidString = UUID.fromString(tableEntity.getSumId());
        NewResponse newResponse = new NewResponse();

        /***
         * PM2.5下的
         */
        List<TsKvLatestcfEntity> tsKvLatestcfEntity = cassandraTemplate.select(
                Query.query(where("entity_id").is(uuidString)).and(where("key").is("PM2.5")).limit(1).withAllowFiltering(), TsKvLatestcfEntity.class);
        try {
            newResponse.setPm25(tsKvLatestcfEntity.get(0).getLongV());
        } catch (Exception e) {
            newResponse.setPm25(0l);
        }


        /***
         * AQI下的
         */
        List<TsKvLatestcfEntity> tsKvLatestcfEntity1 = cassandraTemplate.select(
                Query.query(where("entity_id").is(uuidString)).and(where("key").is("AQI")).limit(1).withAllowFiltering(), TsKvLatestcfEntity.class);
        try {
            newResponse.setAQI(tsKvLatestcfEntity1.get(0).getLongV());
        } catch (Exception e) {
            newResponse.setAQI(0l);
        }

        return newResponse;
    }




}
