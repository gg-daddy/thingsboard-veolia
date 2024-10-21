package org.thingsboard.server.task;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.thingsboard.server.cassandra.TsKvCfEntity;
import org.thingsboard.server.cassandra.TsKvHourHistoryEntity;
import org.thingsboard.server.cassandra.TsKvServer;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.sql.device.DeviceRepository;
import org.thingsboard.server.utils.DateUtils;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Configuration      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class HuizhongxingJob extends Job {


    @Autowired
    private DeviceRepository deviceRepository;


    @Autowired
    private TsKvServer tsKvServer;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /***
     *
     * @throws Exception
     */
    @Override
    @Scheduled(cron = "0 30 0 * * ? ")
//    @Scheduled(cron = "0 0,42 * * * ? ")
    public void processingTasks() throws Exception {

        System.out.println("定时任务开始... 会中心");
        List<DeviceEntity> kaList = deviceRepository.findByTenantIdAndType("1eab9e8ddca861092284b8ee4a402d9", "kaiterra");
        List<DeviceEntity> enList = deviceRepository.findByTenantIdAndType("1eab9e8ddca861092284b8ee4a402d9", "energy");
        List<DeviceEntity> list = deviceRepository.findByTenantIdAndType("1eab9e8ddca861092284b8ee4a402d9", "SUM");
        kaList.addAll(enList);
        kaList.addAll(list);

        for (DeviceEntity deviceEntity : kaList) {
            boolean flag = true;
            while (flag) {
                try {
                    List<TsKvCfEntity> list1 = tsKvServer.findAll1(deviceEntity.getId(), DateUtils.getBeginDayOfYesterday(), DateUtils.getEndDayOfYesterDay(), "11");
                    flag = false;
                    List<TsKvHourHistoryEntity> entityList = getList(list1);
                    stringRedisTemplate.opsForZSet().add(deviceEntity.getId().toString(),
                            JSON.toJSONString(entityList), DateUtils.getBeginDayOfYesterday());

                    /**
                     * 备份进入cassandra
                     */
                    for (TsKvHourHistoryEntity thh : entityList) {
                        tsKvServer.add(thh);
                    }


                } catch (Exception e) {
                    e.printStackTrace();
//                    Thread.sleep(DateUtils.getHourTime() / 120);
                    flag = true;
                }
            }
        }
        System.out.println("定时任务结束...");
    }


    /***
     * 获取历史曲线
     * @param list
     * @return
     */
    private List<TsKvHourHistoryEntity> getList(List<TsKvCfEntity> list) {
        List<TsKvHourHistoryEntity> historyEntityList = new LinkedList<>();
        Map<String, List<TsKvCfEntity>> listMap = list.stream().collect(Collectors.groupingBy(TsKvCfEntity::getKey));
        for (String s : listMap.keySet()) {
            List<TsKvCfEntity> entityList = listMap.get(s);
            for (int i = 0; i < 24; i++) {
                TsKvHourHistoryEntity ts = new TsKvHourHistoryEntity();
                ts.setTs((DateUtils.getBeginDayOfYesterday() + DateUtils.getHourTime() * i));
                ts.setKey(s);
                try {
                    int finalI = i;
                    Long end = DateUtils.getBeginDayOfYesterday() + DateUtils.getHourTime() * finalI;
                    List<TsKvCfEntity> collect = entityList.stream().filter
                            (item -> item.getTs()
                                    >= (end)).collect(Collectors.toList());
                    TsKvCfEntity tsKvCfEntity = collect.stream().min(Comparator.comparingLong(TsKvCfEntity::getTs)).get();
                    System.out.println(tsKvCfEntity.getLongV());
                    ts.setValue(tsKvCfEntity.getLongV());
                    ts.setEntityId(tsKvCfEntity.getEntityId().toString());
                } catch (Exception e) {
                    ts.setValue(1);
                }
                historyEntityList.add(ts);
            }
        }
        return historyEntityList;
    }


    public static void main(String[] args) {


    }

}
