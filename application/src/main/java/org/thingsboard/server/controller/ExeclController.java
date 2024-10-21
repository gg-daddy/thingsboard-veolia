package org.thingsboard.server.controller;/*
 * @Author:${zhangrui}
 * @Date:2020/9/25 9:51
 */

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.Request.ExportRequest;
import org.thingsboard.server.Response.ResponseResult;
import org.thingsboard.server.Response.Result;
import org.thingsboard.server.cassandra.TsKvCfEntity;
import org.thingsboard.server.cassandra.TsKvHourHistoryEntity;
import org.thingsboard.server.cassandra.TsKvServer;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.model.sql.ProjectEntity;
import org.thingsboard.server.dao.sql.device.DeviceRepository;
import org.thingsboard.server.dao.sql.projectList.ProjectRepository;
import org.thingsboard.server.service.execl.DemoExport;
import org.thingsboard.server.service.execl.ExcelServer;
import org.thingsboard.server.service.execl.ExeclExportUtils;
import org.thingsboard.server.service.execl.ExeclRequest;
import org.thingsboard.server.task.ZONGJob;
import org.thingsboard.server.utils.DateUtils;
//import org.thingsboard.server.utils.ExcelUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/noauth/export")
public class ExeclController {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TsKvServer tsKvServer;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ExcelServer excelServer;

    /****
     * 导出excel 天
     * @return
     */
    @GetMapping("/exportExcelForDay")
    public ResponseResult exportExcelForDay(String equipmentNumber,String time,String type,String tenantId,String equipmentName) throws Exception {
        return excelServer.exportExcelForDay(equipmentNumber,time,type,tenantId,equipmentName);
    }


    /****
     * 导出excel 天
     * @return
     */
    @GetMapping("/exportExcelForDay1")
    public ResponseResult exportExcelForDay1(String equipmentNumber,String time,String type,String tenantId,String equipmentName) throws Exception {
        return excelServer.exportExcelForDay1(equipmentNumber,time,type,tenantId,equipmentName);
    }


    /****
     * 导出excel 月
     * @return
     */
    @GetMapping("/exportExcelForMonth")
    public ResponseResult exportExcelForMonth(String equipmentNumber,String time,String type,String tenantId,String equipmentName) throws Exception {
        return excelServer.exportExcelForMonth(equipmentNumber,time,type,tenantId,equipmentName);
    }

    /***
     * 获取到下拉菜单
     * 死写法只对市政院管用
     * @param tenantId
     * @return
     */
    @GetMapping("/findDeviceList")
    public List<ExeclRequest> findList(String tenantId) {


        List<DeviceEntity> entityList = deviceRepository.findByTenantIdAndLabelNotNull(tenantId);
        /**
         * 集合对转
         */
        List<ExeclRequest> list = new LinkedList<>();
        entityList.stream().forEach(p -> {

            if (!"实时值".equals(p.getLabel()) && !"月均值".equals(p.getLabel())) {
                ExeclRequest execlRequest = new ExeclRequest();
                execlRequest.setLabal(p.getLabel());
                execlRequest.setValue(p.getStrId());
                list.add(execlRequest);
            }
        });


        /**
         * 死写法只对市政院管用
         */
        List<ExeclRequest> execlRequests = order(list);

        List<ExeclRequest> requestList = execlRequests.subList(16, execlRequests.size());
        requestList.addAll(execlRequests.subList(0, 16));
        return requestList;

    }


    /***
     * 楼层排序
     * @return
     */
    private List<ExeclRequest> order(List<ExeclRequest> execlRequests) {
        /**
         * 如果小于等于
         */
        Collections.sort(execlRequests, new Comparator<ExeclRequest>() {
            @Override
            public int compare(ExeclRequest o1, ExeclRequest o2) {
                if (o1.getLabal().compareTo(o2.getLabal()) < 0) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        return execlRequests;
    }


    /**
     * execl的导出
     */
    @GetMapping("/exportinfo")
    public void exportAuditInfo(HttpServletResponse response, HttpServletRequest request,
                                @RequestParam("project") String project
            , @RequestParam("tenantId") String tenantId) throws Exception {

        List<DeviceEntity> entityList = deviceRepository.findByTenantIdAndLabelNotNull(tenantId);

        List<DemoExport> exportList = tsKvServer.toExport(entityList);

        /***
         * 获取到是哪个项目的
         */
        ProjectEntity projectEntity = projectRepository.findById(project).get();


        String header = request.getHeader("Origin");
        response.setHeader("Access-Control-Allow-Origin", header);// 解决跨域

        /**
         * 后期再优化，先这样
         *
         * 循环查询先做
         */
        exportList.stream().forEach(p -> {
            try {

                System.out.println("我进行了抽取..........");
                List<TsKvCfEntity> cfEntityList = tsKvServer.findAll(p.getUuid(), p.getDate());
                System.out.println(cfEntityList.size());

                /**
                 * 封装起来
                 */
                Map<String, Double> map = cfEntityList.stream().collect(Collectors.groupingBy(TsKvCfEntity::getKey, Collectors.averagingLong(TsKvCfEntity::getDblV)));

                p.setPM25(String.valueOf(map.get("PM2.5")));
                p.setCo2(String.valueOf(map.get("C02")));
                p.setCH2o(String.valueOf(map.get("CH2O")));
                p.setPm100(String.valueOf(map.get("PM1.0")));
                p.setPm10(String.valueOf(map.get("PM10")));
                p.setTemperature(String.valueOf(map.get("temperature")));
                p.setHumidity(String.valueOf(map.get("humidity")));

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        List<DemoExport> orderExecl = orderExecl(exportList);
        List<DemoExport> subList = orderExecl.subList(DateUtils.getAllDay(DateUtils.getLastMonth()) * 16, orderExecl.size());
        subList.addAll(orderExecl.subList(0, DateUtils.getAllDay(DateUtils.getLastMonth()) * 16));
        ExeclExportUtils.createExcel(subList, response, projectEntity.getName());
    }


    /**
     * execl导出排序
     *
     * @return
     */
    private List<DemoExport> orderExecl(List<DemoExport> exportList) {
        /**
         * 如果小于等于
         */
        Collections.sort(exportList, new Comparator<DemoExport>() {

            @Override
            public int compare(DemoExport o1, DemoExport o2) {
                if (o1.getPointPosition().compareTo(o2.getPointPosition()) < 0) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        return exportList;

    }


    /***
     * 导出柱状图
     * @param response
     * @param request
     */
    @GetMapping("/exportChart")
    public void exportChart(HttpServletResponse response, HttpServletRequest request, String deviceId) throws Exception {

        DeviceEntity deviceEntity = deviceRepository.findById(deviceId).get();

        StringBuilder sb = new StringBuilder(DateUtils.getLastMonth());
        sb.append(deviceEntity.getLabel());

        String header = request.getHeader("Origin");
        response.setHeader("Access-Control-Allow-Origin", header);// 解决跨域

        XSSFWorkbook wb = new XSSFWorkbook();

        UUID uuid = UUIDConverter.fromString(deviceEntity.getStrId());

        List<TsKvHourHistoryEntity> list = tsKvServer.findForOne(uuid.toString());
        /**
         * 温度的
         */
        XSSFSheet template = wb.createSheet("温度");
        List<Double> doubleListTemperature = tsKvServer.getType(list, "temperature");
        ExeclExportUtils.exportTemplate(template, doubleListTemperature, sb.toString());


        /**
         * 湿度
         */
        XSSFSheet humidy = wb.createSheet("湿度");
        List<Double> doubleListHumidity = tsKvServer.getType(list, "humidity");
        ExeclExportUtils.exportHumidy(humidy, doubleListHumidity, sb.toString());


        XSSFSheet sheet = wb.createSheet("CO₂");
        List<Double> doubleListCo2 = tsKvServer.getType(list, "CO2");
        ExeclExportUtils.exportCO2(sheet, doubleListCo2, sb.toString());

        XSSFSheet pm25 = wb.createSheet("PM2.5");
        List<Double> doubleListPM25 = tsKvServer.getType(list, "PM2.5");
        ExeclExportUtils.exportPM25(pm25, doubleListPM25, sb.toString());

        XSSFSheet pm10 = wb.createSheet("PM10");
        List<Double> doubleListPM10 = tsKvServer.getType(list, "PM10");
        ExeclExportUtils.exportPM10(pm10, doubleListPM10, sb.toString());

        /***
         *
         */
        XSSFSheet ch20 = wb.createSheet("甲醛");
        List<Double> doubleListCH2O = tsKvServer.getType(list, "CH2O");
        ExeclExportUtils.exportCH2O(ch20, doubleListCH2O, sb.toString());


        ExeclExportUtils.export(wb, response, sb.toString());

    }


    @GetMapping("/exp")
    public Boolean exp(@RequestParam("id") String id, @RequestParam("type") String type) throws Exception {


        for (int j = 0; j < 7; j++) {
            if (j == 0) {
                add(id, "PM10");
            } else if (j == 1) {
                add(id, "CH2O");
            } else if (j == 2) {
                add(id, "PM1.0");
            } else if (j == 3) {
                add(id, "PM2.5");
            } else if (j == 4) {
                add(id, "CO2");
            } else if (j == 5) {
                add(id, "temperature");
            } else if (j == 6) {
                add(id, "humidity");
            }
        }

        return true;
    }



    @Autowired
    private ZONGJob zongJob;

    @GetMapping("/getMap")
    public Map getMap() throws Exception {
        zongJob.processingTasks();
        Map map = new HashMap();
        return map;
    }

    private void add(String id, String type) throws Exception {
        UUID uuid = UUID.fromString(id);
        List<TsKvCfEntity> list = tsKvServer.findAll1(uuid, type);

        Long start = 1588262400000l;
        Long end = 1604851200000l;
        int days = (int) ((end - start) / 3600000);

        for (long i = 0; i < days; i++) {

            DecimalFormat df = new DecimalFormat("######0.00");
            double shoot = (double) Math.random();    //  产生了一个随机数
            String d = df.format(shoot);

            TsKvHourHistoryEntity ts = new TsKvHourHistoryEntity();
            ts.setTs((start + 3600000 * i));
            ts.setEntityId(id);
            ts.setKey(list.get((int) (i * 30)).getKey());

            double key = (double) list.get((int) (30 * i)).getLongV() + Double.valueOf(d);

            ts.setValue(key);
            tsKvServer.add(ts);
        }


        /**
         * 存在一个id的装换
         *
         * @param args
         */




    }

    public static void main(String[] args) {


        System.out.println(111);


        UUID uuid = UUIDConverter.fromString("1eb678728e0a932b8da15e5146c05f0");
        System.out.println(uuid);
        UUID uuid1 = UUIDConverter.fromString("1eb7d535e12cca099a13ba675ae8da4");
        System.out.println(uuid1);


        UUID uuid2 = UUIDConverter.fromString("1eb7d7e7fcf379099a13ba675ae8da4");
        System.out.println(uuid2);
        UUID uuid3 = UUIDConverter.fromString("1eb2d2c5c9df9c0ad77273b56443f98");
        System.out.println(uuid3);


        UUID uuid4 = UUIDConverter.fromString("1eb2d2e0ddec1a087071323b5852caa");
        System.out.println(uuid4);
        UUID uuid5 = UUIDConverter.fromString("1eb2d2e5a4048c087071323b5852caa");
        System.out.println(uuid5);


        UUID uuid6 = UUIDConverter.fromString("1eb2d52cf91820083a815d8afa79994");
        System.out.println(uuid6);
        UUID uuid7 = UUIDConverter.fromString("1eb2d2ba01c3960ad77273b56443f98");
        System.out.println(uuid7);

        UUID uuid8 = UUIDConverter.fromString("1eb2d2d3e472400ad77273b56443f98");
        System.out.println(uuid8);
        UUID uuid9 = UUIDConverter.fromString("1eb2d2cdb7c27d0ad77273b56443f98");
        System.out.println(uuid9);




    }


}
