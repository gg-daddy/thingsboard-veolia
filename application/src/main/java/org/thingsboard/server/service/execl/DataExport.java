package org.thingsboard.server.service.execl;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataExport implements Serializable {

    @Excel(name = "点位", orderNum = "1", width = 20, isWrap = false)
    private String pointName;

    @Excel(name = "传入时间", orderNum = "2", width = 20, isWrap = false)
    private String time;

    @Excel(name = "检测项", orderNum = "3", width = 20, isWrap = false)
    private String testItems;

    @Excel(name = "检测值", orderNum = "4", width = 20, isWrap = false)
    private String value;




}
