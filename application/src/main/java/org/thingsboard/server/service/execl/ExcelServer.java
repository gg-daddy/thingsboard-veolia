package org.thingsboard.server.service.execl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.thingsboard.server.Request.ExportRequest;
import org.thingsboard.server.Response.ResponseResult;
import org.thingsboard.server.Response.Result;
import org.thingsboard.server.cassandra.TsKvCfEntity;
import org.thingsboard.server.cassandra.TsKvHourHistoryEntity;
import org.thingsboard.server.cassandra.TsKvServer;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.sql.device.DeviceRepository;
import org.thingsboard.server.utils.DateUtils;
import org.thingsboard.server.utils.ExcelUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExcelServer {

    @Autowired
    HttpServletResponse httpServletResponse;

    @Autowired
    private TsKvServer tsKvServer;

    /***
     * excel的天导出
     * @param equipmentNumber
     * @param time
     * @param type
     * @param tenantId
     * @param equipmentName
     * @return
     * @throws Exception
     */
    public ResponseResult exportExcelForDay(String equipmentNumber,String time,String type,String tenantId,String equipmentName) throws Exception {

        System.out.println("equipmentNumber:"+equipmentNumber);
        System.out.println("time:"+time);
        System.out.println("tenantId:"+tenantId);
        System.out.println("equipmentName:"+equipmentName);
        System.out.println("type:"+type);


        List<DataExport> list = new LinkedList<>();
        Map<String, Long> toDateLong = DateUtils.strToDateLong(time);
        String unit = "";
        List<TsKvCfEntity> entityList = tsKvServer.findAllBetweenTime(UUIDConverter.fromString(equipmentNumber), toDateLong.get("startTime"), toDateLong.get("endTime"), type);
        unit = getUnit(tenantId,type);
        String finalUnit = unit;

        String finalType = chanceType(type);
        entityList.stream().forEach(p->{
            DataExport dataExport = new DataExport();
            dataExport.setPointName(equipmentName);

            dataExport.setTestItems(finalType);
            dataExport.setTime(DateUtils.getDateAll(p.getTs()));
            String str = String.valueOf(p.getLongV() == null ? p.getStrV() :p.getLongV());
            if(StringUtils.isBlank(str) || "null".equals(str)){
                str = String.valueOf(p.getDblV());
            }
            dataExport.setValue(str+ finalUnit);
            list.add(dataExport);
        });
        ExcelUtils.exportExcel(list,equipmentName,equipmentName
                ,DataExport.class,equipmentName+"---"+time,true,httpServletResponse);
        return ResponseResult.SUCCESS();
    }

    @Autowired
    private DeviceRepository deviceRepository;

    public ResponseResult exportExcelForDay1(String equipmentNumber,String time,String type,String tenantId,String equipmentName) throws Exception {



        System.out.println("equipmentNumber:"+equipmentNumber);
        System.out.println("time:"+time);
        System.out.println("tenantId:"+tenantId);
        System.out.println("equipmentName:"+equipmentName);
        System.out.println("type:"+type);

        List<DeviceEntity> entities = deviceRepository.findByTenantIdAndType(tenantId, "energy");


        for (DeviceEntity entity : entities) {
            Map<String,String> stringMap = new HashMap<>();
//            stringMap.put("AQI","AQI");
//            stringMap.put("CH2O","CH2O");
//            stringMap.put("CO2","CO2");
//            stringMap.put("Humid","湿度");
//            stringMap.put("PM10","PM10");
//            stringMap.put("PM2.5","PM2.5");
//            stringMap.put("pollution","污染物");
//            stringMap.put("Temp","温度");
//            stringMap.put("TVOC","TVOC");
            stringMap.put("energy","电量");

            for (String s : stringMap.keySet()) {

                List<DataExport> list = new LinkedList<>();

                String unit = "";
                List<TsKvCfEntity> entityList = tsKvServer.findAllBetweenTime(UUIDConverter.fromString(entity.getStrId()), 1627747200000l, 1630339200000l, s);
                unit = getUnit(tenantId,type);
                String finalUnit = unit;

                String finalType = chanceType(s);
                entityList.stream().forEach(p->{
                    DataExport dataExport = new DataExport();
                    dataExport.setPointName(entity.getName());
                    dataExport.setTestItems(s);
                    dataExport.setTime(DateUtils.getDateAll(p.getTs()));
                    String str = String.valueOf(p.getLongV() == null ? p.getStrV() :p.getLongV());
                    if(StringUtils.isBlank(str) || "null".equals(str)){
                        str = String.valueOf(p.getDblV());
                    }
                    dataExport.setValue(str+ finalUnit);
                    list.add(dataExport);
                });
                ExcelUtils.exportExcel(list,equipmentName,entity.getName()
                        ,DataExport.class,stringMap.get(s)+"---"+time,true,httpServletResponse);
            }

        }




        return ResponseResult.SUCCESS();
    }


    /***
     * 数据转换
     * @param type
     * @return
     */
    private String chanceType(String type){

         switch (type){
             case "Humid":
                 return "湿度";
             case "Temp":
                 return "温度";
         }
         return type;
    }

    /****
     *  月数据导出 代码未细化
     * @param equipmentNumber
     * @param time
     * @param type
     * @param tenantId
     * @param equipmentName
     * @return
     * @throws Exception
     */
    public ResponseResult exportExcelForMonth(String equipmentNumber,String time,String type,String tenantId,String equipmentName) throws Exception {

        System.out.println("equipmentNumber:"+equipmentNumber);
        System.out.println("time:"+time);
        System.out.println("tenantId:"+tenantId);
        System.out.println("equipmentName:"+equipmentName);
        System.out.println("type:"+type);


        Map<String, Long> toDateLong = DateUtils.strToDateLongForMonth(time);
        String unit = getUnit(tenantId,type);;
        List<TsKvCfEntity> entityList = tsKvServer.findAllBetweenTime(UUIDConverter.fromString(equipmentNumber), toDateLong.get("startTimeForMonth"), toDateLong.get("endTimeForMonth"), type);
        entityList = entityList.stream().sorted(Comparator.comparing(TsKvCfEntity::getTs)).collect(Collectors.toList());

        /***
         * 市政院1号楼   1eb14336092c090a0fa793db9637587 目前是1分钟一笔的数据
         *   其他        5秒一笔
         * 粗粒度3分钟一笔需要导出
         */
        String finalType = chanceType(type);

        if("1eb14336092c090a0fa793db9637587".equals(tenantId)){
            ExcelUtils.exportExcel(this.getDataExports(entityList,equipmentName,finalType,unit,3),equipmentName,equipmentName
                    ,DataExport.class,equipmentName+"---"+time,true,httpServletResponse);
        }else {
            ExcelUtils.exportExcel(this.getDataExports(entityList,equipmentName,finalType,unit,60),equipmentName,equipmentName
                    ,DataExport.class,equipmentName+"---"+time,true,httpServletResponse);
        }
        return ResponseResult.SUCCESS();
    }

    /***
     * 数据导出 月
     * @param entityList
     * @return
     */
    private List<DataExport> getDataExports(List<TsKvCfEntity> entityList,String equipmentName,String type,String finalUnit,Integer number){
        List<DataExport> list = new LinkedList<>();
        for (int i = 0; i <entityList.size(); i++) {
            if(i%number == 0){
                TsKvCfEntity p =entityList.get(i);
                DataExport dataExport = new DataExport();
                dataExport.setPointName(equipmentName);
                dataExport.setTestItems(type);
                dataExport.setTime(DateUtils.getDateAllNoMiao(p.getTs()));
                String str = String.valueOf(p.getLongV() == null ? p.getStrV() :p.getLongV());
                if(StringUtils.isBlank(str) || "null".equals(str)){
                    str = String.valueOf(p.getDblV());
                }
                dataExport.setValue(str+ finalUnit);
                list.add(dataExport);
            }
        }
        return list;
    }



    /***
     * 这是一段死代码  目前是适合
     * 市政院1号楼   1eb14336092c090a0fa793db9637587
     *
     * 市政院2号楼   1eab9e802ba5aa092284b8ee4a402d9
     *
     * @return
     */
    private String getUnit(String tenantId,String testItems){

        /***
         * 市政院1号楼
         */
        if("1eb14336092c090a0fa793db9637587".equals(tenantId)){
                switch (testItems){
                    case "CO2":
                       return "(ppm)";
                    case "Humid":
                        return "(%)";
                    case "PM10":
                        return "(ug/m³)";
                    case "PM2.5":
                        return "(ug/m³)";
                    case "Temp":
                        return "℃";
                    case "TVOC":
                        return "ppb";
                    case "AQI":
                        return "";
                }
           } else if("1eab9e802ba5aa092284b8ee4a402d9".equals(tenantId)) {
            /***
             * 市政院2号楼
             */
            switch (testItems){
                case "CH2O":
                    return "(ug/m³)";
                case "CO2":
                    return "(ppm)";
                case "humidity":
                    return "(%)";
                case "PM1.0":
                    return "(ug/m³)";
                case "PM10":
                    return "(ug/m³)";
                case "PM2.5":
                    return "(ug/m³)";
                case "temperature":
                    return "℃";
            }
        }else {

        }
            switch (testItems){
                case "CH2O":
                    return "(ug/m³)";
                case "CO2":
                    return "(ppm)";
                case "humidity":
                    return "(%)";
                case "PM1.0":
                    return "(ug/m³)";
                case "PM10":
                    return "(ug/m³)";
                case "PM2.5":
                    return "(ug/m³)";
                case "temperature":
                    return "℃";

        }
        return "";
    }




}
