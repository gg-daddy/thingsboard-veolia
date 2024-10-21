package org.thingsboard.server.service.collection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.Request.CollectionRequest;
import org.thingsboard.server.Response.*;
import org.thingsboard.server.cassandra.TsKvLatestcfEntity;
import org.thingsboard.server.cassandra.TsKvServer;
import org.thingsboard.server.dao.model.sql.CollectionTableEntity;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.sql.collection.CollectionRepository;
import org.thingsboard.server.dao.sql.device.DeviceRepository;
import org.thingsboard.server.utils.NumUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DefaultServer implements CollectionServer {


    @Autowired
    CollectionRepository collectionRepository;


    @Autowired
    private DeviceRepository deviceRepository;


    @Autowired
    private TsKvServer tsKvServer;

    /***
     * 传入是true的时候为收藏
     * false 是取消收藏
     * @param collectionRequest
     * @param userId
     */


    @Override
    @Transactional
    public ResponseResult collection1(CollectionRequest collectionRequest, String userId, String phone) throws IOException {
        List<String> list = Arrays.asList(collectionRequest.getDeviceIds().split(",")).stream().map(s -> (s.trim())).collect(Collectors.toList());


        if (collectionRequest.getFlag()) {

            List<CollectionTableEntity> entityList = new LinkedList<>();
            for (Object s : list) {
                CollectionTableEntity collectionTableEntity = new CollectionTableEntity();
                collectionTableEntity.setUserId(userId);
                collectionTableEntity.setDeviceId(String.valueOf(s));
                collectionTableEntity.setCollectionId(UUID.randomUUID().toString());
                collectionTableEntity.setPhone(phone);
                entityList.add(collectionTableEntity);
            }
            collectionRepository.saveAll(entityList);

        } else {

            for (Object s : list) {
                collectionRepository.deleteByDeviceIdAndUserIdAndPhone(String.valueOf(s), userId, phone);
            }
        }

        if (collectionRequest.getFlag()) {
            return ResponseResult.CUSTOMIZE(CommonCode.COLLECTION_YES_SUCCESS);
        } else {
            return ResponseResult.CUSTOMIZE(CommonCode.COLLECTION_NO_SUCCESS);
        }
    }


    /***
     * 查询全部的
     * @return
     */
    @Override
    public QueryPaginationResult findAll(String uuid) {

        /***
         * 死写法
         */
        QueryPaginationResult queryPaginationResult = new QueryPaginationResult();
        if ("1eab9e802ba5aa092284b8ee4a402d9".equals(uuid)) {
            //市政院
            List<DeviceEntity> list = deviceRepository.findByTenantIdAndTypeAndLabelNotNull(uuid, "速耐连七合一");
            List<CollReponse> reponseList = this.order(getList(list, "楼"));
            queryPaginationResult.setPage(reponseList.subList(0, reponseList.size() - 1));
        } else if ("1eab9e8dae36bb092284b8ee4a402d9".equals(uuid)) {

            List<DeviceEntity> list = deviceRepository.findByTenantIdAndType(uuid, "前滩");
            List<CollReponse> reponseList = getList(list);
            queryPaginationResult.setPage(reponseList);

        } else if ("1eab9e8f0aa111092284b8ee4a402d9".equals(uuid)) {
            /***
             * 703
             * 数据实现改变 -------
             * 效果排序
             */
            List<DeviceEntity> deviceEntities = getListDeviceEntity(uuid);
            List<CollReponse> reponseList = getList(deviceEntities);
            List<CollReponse> order703 = order703(reponseList);
            order703.stream().forEach(p -> {
                p.setName(p.getName().split("-")[1]);
            });
            queryPaginationResult.setPage(order703);
        } else if ("1eab9e8ddca861092284b8ee4a402d9".equals(uuid)) {
            /***
             * 会中心
             * 数据实现改变 -------
             */
            List<DeviceEntity> deviceEntities = getListDeviceEntity(uuid);
            List<CollReponse> reponseList = getList(deviceEntities);
            List<CollReponse> zhongXing = orderHuiZhongXing(reponseList);
            zhongXing.stream().forEach(p -> {
                p.setName(p.getLabal().split("_")[1].substring(2));
            });
            queryPaginationResult.setPage(zhongXing);
        } else if("1eb14336092c090a0fa793db9637587".equals(uuid)){
            /***
             * 市政院的1号楼
             */
            List<DeviceEntity> entityList = this.getListDeviceEntityOrder(uuid);
            List<CollReponse> reponseList = getList(entityList);
            queryPaginationResult.setPage(reponseList);

        }else {
            /***
             * 会中心 ,昆明
             * 数据实现改变 -------
             */
            List<DeviceEntity> list = getListDeviceEntity(uuid);
            List<CollReponse> reponseList = getList(list);
            queryPaginationResult.setPage(reponseList);
        }


        return queryPaginationResult;
    }


    private List<DeviceEntity> getListDeviceEntity(String uuid) {
        return deviceRepository.findByTenantIdAndType(uuid, "kaiterra");
    }

    /***
     * 后续的项目的排序不改变原有的逻辑
     * @param uuid
     * @return
     */
    private List<DeviceEntity> getListDeviceEntityOrder(String uuid) {
        return deviceRepository.findByTenantIdAndTypeOrderByNameAsc(uuid, "kaiterra");
    }






    /***
     * 判断是不是收藏了
     */
    @Override
    public List<CollReponse> collList1(List<CollReponse> reponseList, String userId, String phone) {
        List<CollectionTableEntity> entityList = collectionRepository.findByUserIdAndPhone(userId, phone);
        Set<String> collect = entityList.stream().map(CollectionTableEntity::getDeviceId).collect(Collectors.toSet());

        /***
         * 市政院交行的写法可以兼容
         */
        reponseList.stream().forEach(p -> {

            for (String s : p.getPid()) {
                if (collect.contains(s)) {
                    p.setFlag(true);

                }
            }
        });
        return reponseList;
    }


    /***
     * pid 进来   cid出去
     * @param str
     * @return
     */
    @Override
    public List<SelectBean> findSelect(String str, String uuid) {
        List<String> list = Arrays.asList(str.split(",")).stream().map(s -> (s)).collect(Collectors.toList());


        List<DeviceEntity> entityList = deviceRepository.findByIdIn(list);
        List<SelectBean> beanList = new LinkedList<>();
        for (DeviceEntity deviceEntity : entityList) {
            SelectBean selectBean = new SelectBean();
            selectBean.setName(selectChange(deviceEntity, uuid));
            selectBean.setPid(deviceEntity.getStrId());
            selectBean.setCid(deviceEntity.getId().toString());
            beanList.add(selectBean);
        }
        return this.orderSelect(beanList);
    }

    /***
     * 下拉菜单改变
     * @return
     */
    private String selectChange(DeviceEntity deviceEntity, String uuid) {


        /**
         * 703
         */
        if ("1eab9e8f0aa111092284b8ee4a402d9".equals(uuid)) {
            return deviceEntity.getName().split("-")[1];
            /***
             * 荟中心
             */
        } else if ("1eab9e8ddca861092284b8ee4a402d9".equals(uuid)) {
            System.out.println(deviceEntity.getLabel());
            return deviceEntity.getLabel().split("_")[1].substring(2);
            /**
             * 昆明
             */
        } else if ("1eab9e8f977a32092284b8ee4a402d9".equals(uuid)) {
            return deviceEntity.getName().substring(2);
        } else {
            return deviceEntity.getLabel();
        }
    }


    /***
     * 市政院的写法
     * 获取到集合
     * 走了40多次的循环不好后期改
     * @return
     */
    private List<CollReponse> getList(List<DeviceEntity> list, String str) {
        List<CollReponse> reponseList = new LinkedList<>();
        list.stream().forEach(p -> {
            StringBuilder sb = new StringBuilder(p.getLabel().split(str)[0]).append(str);
            p.setLabel(sb.toString());
        });
        Map<String, List<DeviceEntity>> userGroupMap = list.stream().collect(Collectors.groupingBy(DeviceEntity::getLabel));
        //集合的收集
        for (String s : userGroupMap.keySet()) {
            CollReponse collReponse = new CollReponse();
            collReponse.setName(s);
            List<DeviceEntity> entityList = userGroupMap.get(s);
            List<String> pid = new LinkedList<>();
            List<String> cid = new LinkedList<>();
            long pm25 = 0l;
            long template = 0l;
            long humidly = 0l;
            long CO2 = 0l;
            for (DeviceEntity deviceEntity : entityList) {
                List<TsKvLatestcfEntity> tsKvServerByList = tsKvServer.findByList(deviceEntity.getId());
                for (TsKvLatestcfEntity tsKvLatestcfEntity : tsKvServerByList) {
                    if (tsKvLatestcfEntity.getKey().equals("PM2.5")) {
                        pm25 = tsKvLatestcfEntity.getLongV() + pm25;
                    }
                    if (tsKvLatestcfEntity.getKey().equals("temperature")) {
                        template = tsKvLatestcfEntity.getLongV() + template;
                    }
                    if (tsKvLatestcfEntity.getKey().equals("humidity")) {
                        humidly = tsKvLatestcfEntity.getLongV() + humidly;
                    }
                    if ("CO2".equals(tsKvLatestcfEntity.getKey())) {
                        CO2 = tsKvLatestcfEntity.getLongV() + CO2;
                    }
                }
                pid.add(deviceEntity.getStrId());
                cid.add(deviceEntity.getId().toString());
            }
            collReponse.setCid(cid);
            collReponse.setPid(pid);
            collReponse.setHumidly(new BigDecimal(humidly / collReponse.getPid().size()).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
            collReponse.setPM25(new BigDecimal(pm25 / collReponse.getPid().size()).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
            collReponse.setTemplate(new BigDecimal(template / collReponse.getPid().size()).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
            collReponse.setCO2(new BigDecimal(CO2 / collReponse.getPid().size()).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
            reponseList.add(collReponse);
        }
        return reponseList;
    }

    /***
     * 楼层排序
     * @return
     */
    private List<CollReponse> order(List<CollReponse> list) {
        /**
         * 如果小于等于
         */
        Collections.sort(list, new Comparator<CollReponse>() {
            @Override
            public int compare(CollReponse o1, CollReponse o2) {
                if (o1.getName().length() > o2.getName().length()) {
                    return 1;
                } else {
                    if (o1.getName().compareTo(o2.getName()) < 0) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            }
        });
        return list;
    }


    /***
     * 703排序
     * @return
     */
    private List<CollReponse> order703(List<CollReponse> list) {
        /**
         * 如果小于等于
         */
        Collections.sort(list, new Comparator<CollReponse>() {
            @Override
            public int compare(CollReponse o1, CollReponse o2) {
                if (o1.getName().compareTo(o2.getName()) < 0) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        return list;
    }


    /**
     * 会中心排序
     *
     * @param list
     * @return
     */
    private List<CollReponse> orderHuiZhongXing(List<CollReponse> list) {
        /**
         * 如果小于等于
         */
        Collections.sort(list, new Comparator<CollReponse>() {
            @Override
            public int compare(CollReponse o1, CollReponse o2) {
                if (o1.getLabal().compareTo(o2.getLabal()) < 0) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        return list;
    }

    /**
     * 集合排序
     */
    private List<SelectBean> orderSelect(List<SelectBean> list) {
        /**
         * 如果小于等于
         */
        Collections.sort(list, new Comparator<SelectBean>() {
            @Override
            public int compare(SelectBean o1, SelectBean o2) {
                if (o1.getName().compareTo(o2.getName()) < 0) {
                    return -1;
                } else {
                    return 1;
                }
            }

        });
        return list;
    }


    /***
     * 获取 除市政院外的其他的数据封装
     * @param list
     * @return
     */
    private List<CollReponse> getList(List<DeviceEntity> list) {

        List<CollReponse> reponseList = new LinkedList<>();

        list.stream().forEach(p -> {
            CollReponse collReponse = new CollReponse();
            //cid的封装
            List<String> cids = new LinkedList<>();
            cids.add(p.getId().toString());
            collReponse.setCid(cids);
            //pid的封装
            List<String> pids = new LinkedList<>();
            pids.add(p.getStrId());
            collReponse.setPid(pids);

            collReponse.setName(p.getName());
            collReponse.setLabal(p.getLabel());
            List<TsKvLatestcfEntity> tsKvServerByList = tsKvServer.findByList(p.getId());
            for (TsKvLatestcfEntity tsKvLatestcfEntity : tsKvServerByList) {

                if ("PM2.5".equals(tsKvLatestcfEntity.getKey())) {
                    collReponse.setPM25(NumUtils.get1Double(tsKvLatestcfEntity.getLongV() == null ? tsKvLatestcfEntity.getDblV() : tsKvLatestcfEntity.getLongV()));
                }
                if ("temperature".equals(tsKvLatestcfEntity.getKey()) || "Temp".equals(tsKvLatestcfEntity.getKey())) {
                    collReponse.setTemplate(NumUtils.get1Double(tsKvLatestcfEntity.getLongV() == null ? tsKvLatestcfEntity.getDblV() : tsKvLatestcfEntity.getLongV()));
                }
                if ("humidity".equals(tsKvLatestcfEntity.getKey()) || "Humid".equals(tsKvLatestcfEntity.getKey())) {
                    collReponse.setHumidly(tsKvLatestcfEntity.getLongV() == null ? tsKvLatestcfEntity.getDblV() : tsKvLatestcfEntity.getLongV());
                }
                if ("CO2".equals(tsKvLatestcfEntity.getKey())) {
                    collReponse.setCO2(tsKvLatestcfEntity.getLongV() == null ? tsKvLatestcfEntity.getDblV() : tsKvLatestcfEntity.getLongV());
                }
            }
            reponseList.add(collReponse);
        });


        return reponseList;

    }

    ;


}

