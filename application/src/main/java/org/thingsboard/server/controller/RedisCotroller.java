package org.thingsboard.server.controller;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.Response.QueryPaginationResult;
import org.thingsboard.server.cassandra.TsKvCfEntity;
import org.thingsboard.server.cassandra.TsKvHourHistoryEntity;
import org.thingsboard.server.cassandra.TsKvServer;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.sql.device.DeviceRepository;
import org.thingsboard.server.utils.DateUtils;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/noauth/redis")
public class RedisCotroller {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Autowired
    private TsKvServer tsKvServer;


    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    CassandraTemplate cassandraTemplate;


    @RequestMapping("/test")
    public QueryPaginationResult test(@RequestParam String id) throws Exception {
        Optional<DeviceEntity> optional = deviceRepository.findById(id);
        List<DeviceEntity> list = new LinkedList<>();
        list.add(optional.get());
        for (DeviceEntity deviceEntity : list) {
            System.out.println("开始执行数据");
            for (long i = 1593532800000l; i < DateUtils.getStartTime(); i = (i + DateUtils.getDayTime())) {
                boolean flag = true;
                while (flag) {
                    try {
                        List<TsKvCfEntity> list1 = tsKvServer.findAll1(deviceEntity.getId(), i, (i + DateUtils.getDayTime()), "11");
                        flag = false;
                        add(list1, deviceEntity.getId().toString(), i);
                    } catch (Exception e) {
//                        Thread.sleep(DateUtils.getHourTime() / 120);
                        flag = true;
                    }
                }
            }
        }
        QueryPaginationResult queryPaginationResult = new QueryPaginationResult();
        queryPaginationResult.setObject(null);
        return queryPaginationResult;
    }


    @RequestMapping("/test1")
    public QueryPaginationResult test1(@RequestParam String id) throws Exception {
        List<DeviceEntity> all = deviceRepository.findByTenantIdAndType("1eab9e8f977a32092284b8ee4a402d9","kaiterra");
        all.addAll(deviceRepository.findByTenantIdAndType("1eab9e8dae36bb092284b8ee4a402d9","前滩"));
        all.addAll(deviceRepository.findByTenantIdAndType("1eab9e802ba5aa092284b8ee4a402d9","速耐连七合一"));        List<DeviceEntity> list = new LinkedList<>();

        for (DeviceEntity deviceEntity : all) {
            System.out.println("开始执行数据");
            for (long i = 1604131200000l; i < DateUtils.getStartTime(); i = (i + DateUtils.getDayTime())) {
                boolean flag = true;
                while (flag) {
                    try {
                        List<TsKvCfEntity> list1 = tsKvServer.findAll1(deviceEntity.getId(), i, (i + DateUtils.getDayTime()), "11");
                        flag = false;
                        add(list1, deviceEntity.getId().toString(), i);
                    } catch (Exception e) {
//                        Thread.sleep(DateUtils.getHourTime() / 120);
                        flag = true;
                    }
                }
            }
        }
        QueryPaginationResult queryPaginationResult = new QueryPaginationResult();
        queryPaginationResult.setObject(null);
        return queryPaginationResult;
    }


    private void add(List<TsKvCfEntity> list, String id, Long start) throws Exception {
        list = list.stream().sorted(Comparator.comparing(TsKvCfEntity::getTs).reversed()).collect(Collectors.toList());
        Map<String, List<TsKvCfEntity>> map = list.stream().collect(Collectors.groupingBy(TsKvCfEntity::getKey));
        List<TsKvHourHistoryEntity> all = new LinkedList<>();
        for (String s : map.keySet()) {
            List<TsKvCfEntity> entities = map.get(s);
            List<TsKvHourHistoryEntity> historyEntities = new LinkedList<>();
            for (int i = 0; i < 24; i++) {
                TsKvHourHistoryEntity ts = new TsKvHourHistoryEntity();
                ts.setTs((start + 3600000 * i));
                ts.setEntityId(id);
                ts.setKey(s);
                try {
                    int finalI = i;
                    List<TsKvCfEntity> entityList = entities.stream().filter
                            (item -> item.getTs()
                                    >= (start + DateUtils.getHourTime() * finalI)).collect(Collectors.toList());
                    TsKvCfEntity tsKvCfEntity = entityList.stream().min(Comparator.comparingLong(TsKvCfEntity::getTs)).get();
                    ts.setValue(tsKvCfEntity.getLongV());
                } catch (Exception e) {
                    ts.setValue(0);
                }
                historyEntities.add(ts);
            }
            all.addAll(historyEntities);
        }
        stringRedisTemplate.opsForZSet().add(id, JSON.toJSONString(all), start);


        /**
         * 存在一个id的装换
         *
         * @param args
         */


//    public static void main(String[] args) {
//        /**
//         * 比较
//         */
//
//
//        for (int i = 0; i <100 ; i++) {
//            DecimalFormat df = new DecimalFormat("######0.00");
//            double shoot= (double) Math.random();    //  产生了一个随机数
//            String d = df.format(shoot);
//            System.out.println(Double.valueOf(d));
//        }
//


//        UUID uuid = UUID.fromString("0a42b2b0-0dbf-11eb-9e24-13f499e9a323");
//
//        String uuid = UUIDConverter.fromTimeUUID(UUID.fromString("8bf9b7b0-de06-11ea-ad61-a1a9e0015159"));
//        System.out.println(uuid);

//        Long start = 1588262400000l;
//        Long end = 1604592000000l;
//        int days = (int) ((end - start) / 3600000);
//        for (long i = 0; i < days; i++) {
//            System.out.println(start + 3600000 * i);
////            if((start + (36000000 * i))>=end){
////                System.out.println("没了");
////                break;
////            }
//        }

    }


    @RequestMapping("/find")
    public QueryPaginationResult find() throws InterruptedException {
        List<DeviceEntity> entityList = deviceRepository.findByTenantIdNot("1eab9e8ddca861092284b8ee4a402d9");

        for (DeviceEntity deviceEntity : entityList) {
            stringRedisTemplate.delete(deviceEntity.getId().toString());
        }

        QueryPaginationResult queryPaginationResult = new QueryPaginationResult();
        queryPaginationResult.setObject(11);
        return queryPaginationResult;
    }

    @RequestMapping("/backUp")
    public Boolean backUp() throws Exception {
        List<DeviceEntity> kaList = deviceRepository.findByTenantIdAndType("1eab9e8ddca861092284b8ee4a402d9","kaiterra");

        List<DeviceEntity> enList = deviceRepository.findByTenantIdAndType("1eab9e8ddca861092284b8ee4a402d9","energy");
        kaList.addAll(enList);
        for (DeviceEntity deviceEntity : kaList) {
            boolean flag = true;
            while (flag) {
                try {
                    System.out.println("开始查询");
                    List<TsKvCfEntity> list1 = tsKvServer.findAll1(deviceEntity.getId(), 1608220800000l, 1609776000000l, "11");
                    flag = false;
                    stringRedisTemplate.opsForZSet().add(deviceEntity.getId().toString(),
                            JSON.toJSONString(list1), DateUtils.getLastStartTime());
                    System.out.println("查询结束");
                } catch (Exception e) {
                    flag = false;
                }
            }
        }
        System.out.println("导入结束.....");
        return true;
    }


    /**
     * 保存数据
     *
     * @return
     */
    @RequestMapping("/backUpTo")
    public Boolean backUpTo() {
        List<DeviceEntity> kaList = deviceRepository.findByTenantIdAndType("1eab9e8ddca861092284b8ee4a402d9","kaiterra");

        List<DeviceEntity> enList = deviceRepository.findByTenantIdAndType("1eab9e8ddca861092284b8ee4a402d9","energy");

        kaList.addAll(enList);
        for (DeviceEntity deviceEntity : kaList) {
            Set<String> set = stringRedisTemplate.opsForZSet().rangeByScore(deviceEntity.getId().toString(), 0, 1640854181000L);
            for (String s : set) {
                List<TsKvCfEntity> array = JSON.parseArray(s, TsKvCfEntity.class);
                for (TsKvCfEntity tsKvCfEntity : array) {
                    System.out.println(tsKvCfEntity);
                    cassandraTemplate.insert(tsKvCfEntity);
                }

            }
        }


        return true;
    }

}
