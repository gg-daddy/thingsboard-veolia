package org.thingsboard.server.service.execl;/*
 * @Author:${zhangrui}
 * @Date:2020/9/25 15:25
 */

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Chart;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.thingsboard.server.cassandra.TsKvHourHistoryEntity;
import org.thingsboard.server.utils.DateUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExeclExportUtils {


    public static void export(Workbook wb, HttpServletResponse response, String title) throws IOException {
        try {

            response.setCharacterEncoding("UTF-8");
//            response.setHeader("content-Type", "application/vnd.ms-excel");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            response.setHeader("Content-Disposition", "attachment;filename="
                    + URLEncoder.encode(title + "空气质量检测报表" + "." + "xlsx", "UTF-8"));
            wb.write(response.getOutputStream());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }


    /***
     * 导出Ch2o
     */
    public static void exportCH2O(XSSFSheet sheet, List<Double> doubleListCH2O,String title) throws IOException {


        Row row;
        Cell cell;
        row = sheet.createRow(1);
        cell = row.createCell(9);
        cell.setCellValue(title+"24时甲醛均值报表");




        row = sheet.createRow(2);
        row.createCell(0).setCellValue("时间");
        row.createCell(1).setCellValue("甲醛(μg/m³)");
//        row.createCell(2).setCellValue("Lines");

        for (int r = 0; r <= 23; r++) {
            Row row1 = sheet.createRow(r + 3);
            cell = row1.createCell(0);
            cell.setCellValue((r) + "时");

            //Co2的值
            cell = row1.createCell(1);
            cell.setCellValue(keepTwo(doubleListCH2O.get(r)));

        }

        /**
         * 画出图表
         */
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
//        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 4, 0, 11, 15);
        /**
         * 调整距离
         */
        XSSFClientAnchor anchor = (XSSFClientAnchor) drawing.createAnchor(0, 0, 0, 0, 3, 3, 21, 28);

        Chart chart = drawing.createChart(anchor);

        CTChart ctChart = ((XSSFChart) chart).getCTChart();
        CTPlotArea ctPlotArea = ctChart.getPlotArea();

        //the bar chart
        CTBarChart ctBarChart = ctPlotArea.addNewBarChart();
        CTBoolean ctBoolean = ctBarChart.addNewVaryColors();
        ctBoolean.setVal(true);
        ctBarChart.addNewBarDir().setVal(STBarDir.COL);
//        ctBarChart.add

        //the bar series
        CTBarSer ctBarSer = ctBarChart.addNewSer();
        CTSerTx ctSerTx = ctBarSer.addNewTx();
        CTStrRef ctStrRef = ctSerTx.addNewStrRef();
        ctStrRef.setF("甲醛!$B$1");
        ctBarSer.addNewIdx().setVal(0);
        CTAxDataSource cttAxDataSource = ctBarSer.addNewCat();
        ctStrRef = cttAxDataSource.addNewStrRef();
        ctStrRef.setF("甲醛!$A$4:$A$27");
        CTNumDataSource ctNumDataSource = ctBarSer.addNewVal();
        CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
        ctNumRef.setF("甲醛!$B$4:$B$27");

        //at least the border lines in Libreoffice Calc ;-)
//        ctBarSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[] {0,0,0});

        //telling the BarChart that it has axes and giving them Ids
        ctBarChart.addNewAxId().setVal(123455); //cat axis 1 (bars)
        ctBarChart.addNewAxId().setVal(123456); //val axis 1 (left)

        //the line chart
        CTLineChart ctLineChart = ctPlotArea.addNewLineChart();
        ctBoolean = ctLineChart.addNewVaryColors();
        ctBoolean.setVal(false);

        //the line series
        CTLineSer ctLineSer = ctLineChart.addNewSer();
        ctSerTx = ctLineSer.addNewTx();
        ctStrRef = ctSerTx.addNewStrRef();
        ctStrRef.setF("甲醛!$C$1");
        ctLineSer.addNewIdx().setVal(1);
        cttAxDataSource = ctLineSer.addNewCat();
        ctStrRef = cttAxDataSource.addNewStrRef();
        ctStrRef.setF("甲醛!$A$4:$A$27");
        ctNumDataSource = ctLineSer.addNewVal();
        ctNumRef = ctNumDataSource.addNewNumRef();
        ctNumRef.setF("甲醛!$C$4:$C$27");

        //at least the border lines in Libreoffice Calc ;-)
//        ctLineSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[] {0,0,0});

        //telling the LineChart that it has axes and giving them Ids
        ctLineChart.addNewAxId().setVal(123458); //cat axis 2 (lines)
        ctLineChart.addNewAxId().setVal(123459); //val axis 2 (right)

        //cat axis 1 (bars)
        CTCatAx ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(123455); //id of the cat axis
        CTScaling ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctCatAx.addNewDelete().setVal(false);
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(123456); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //val axis 1 (left)
        CTValAx ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(123456); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.L);
        ctValAx.addNewCrossAx().setVal(123455); //id of the cat axis
        ctValAx.addNewCrosses().setVal(STCrosses.AUTO_ZERO); //this val axis crosses the cat axis at zero
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //cat axis 2 (lines)
        ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(1234525); //id of the cat axis
        ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);

        ctCatAx.addNewDelete().setVal(true); //this cat axis is deleted
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(1234525); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);


        ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(1234525); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.R);
        ctValAx.addNewCrossAx().setVal(1234525); //id of the cat axis
        ctValAx.addNewCrosses().setVal(STCrosses.MAX); //this val axis crosses the cat axis at max value
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);
    }


    /***
     * 导出PM100
     */
    public static void exportPM100(XSSFSheet sheet) throws IOException {


        Row row;
        Cell cell;

        row = sheet.createRow(2);
        row.createCell(0).setCellValue("时间");
        row.createCell(1).setCellValue("PM1.0");
//        row.createCell(2).setCellValue("Lines");

        for (int r = 0; r <= 23; r++) {
            Row row1 = sheet.createRow(r + 2);
            cell = row1.createCell(0);
            cell.setCellValue((r) + "时");

            //Co2的值
            cell = row1.createCell(1);
            cell.setCellValue(new java.util.Random().nextDouble());

        }

        /**
         * 画出图表
         */
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
//        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 4, 0, 11, 15);
        /**
         * 调整距离
         */
        XSSFClientAnchor anchor = (XSSFClientAnchor) drawing.createAnchor(0, 0, 0, 0, 3, 2, 21, 28);

        Chart chart = drawing.createChart(anchor);

        CTChart ctChart = ((XSSFChart) chart).getCTChart();
        CTPlotArea ctPlotArea = ctChart.getPlotArea();

        //the bar chart
        CTBarChart ctBarChart = ctPlotArea.addNewBarChart();
        CTBoolean ctBoolean = ctBarChart.addNewVaryColors();
        ctBoolean.setVal(true);
        ctBarChart.addNewBarDir().setVal(STBarDir.COL);
//        ctBarChart.add

        //the bar series
        CTBarSer ctBarSer = ctBarChart.addNewSer();
        CTSerTx ctSerTx = ctBarSer.addNewTx();
        CTStrRef ctStrRef = ctSerTx.addNewStrRef();
        ctStrRef.setF("PM1.0!$B$1");
        ctBarSer.addNewIdx().setVal(0);
        CTAxDataSource cttAxDataSource = ctBarSer.addNewCat();
        ctStrRef = cttAxDataSource.addNewStrRef();
        ctStrRef.setF("PM1.0!$A$2:$A$27");
        CTNumDataSource ctNumDataSource = ctBarSer.addNewVal();
        CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
        ctNumRef.setF("PM1.0!$B$2:$B$27");

        //at least the border lines in Libreoffice Calc ;-)
//        ctBarSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[] {0,0,0});

        //telling the BarChart that it has axes and giving them Ids
        ctBarChart.addNewAxId().setVal(123455); //cat axis 1 (bars)
        ctBarChart.addNewAxId().setVal(123456); //val axis 1 (left)

        //the line chart
        CTLineChart ctLineChart = ctPlotArea.addNewLineChart();
        ctBoolean = ctLineChart.addNewVaryColors();
        ctBoolean.setVal(false);

        //the line series
        CTLineSer ctLineSer = ctLineChart.addNewSer();
        ctSerTx = ctLineSer.addNewTx();
        ctStrRef = ctSerTx.addNewStrRef();
        ctStrRef.setF("PM1.0!$C$1");
        ctLineSer.addNewIdx().setVal(1);
        cttAxDataSource = ctLineSer.addNewCat();
        ctStrRef = cttAxDataSource.addNewStrRef();
        ctStrRef.setF("PM1.0!$A$2:$A$27");
        ctNumDataSource = ctLineSer.addNewVal();
        ctNumRef = ctNumDataSource.addNewNumRef();
        ctNumRef.setF("PM1.0!$C$2:$C$27");

        //at least the border lines in Libreoffice Calc ;-)
//        ctLineSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[] {0,0,0});

        //telling the LineChart that it has axes and giving them Ids
        ctLineChart.addNewAxId().setVal(123458); //cat axis 2 (lines)
        ctLineChart.addNewAxId().setVal(123459); //val axis 2 (right)

        //cat axis 1 (bars)
        CTCatAx ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(123455); //id of the cat axis
        CTScaling ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctCatAx.addNewDelete().setVal(false);
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(123456); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //val axis 1 (left)
        CTValAx ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(123456); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.L);
        ctValAx.addNewCrossAx().setVal(123455); //id of the cat axis
        ctValAx.addNewCrosses().setVal(STCrosses.AUTO_ZERO); //this val axis crosses the cat axis at zero
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //cat axis 2 (lines)
        ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(1234525); //id of the cat axis
        ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);

        ctCatAx.addNewDelete().setVal(true); //this cat axis is deleted
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(1234525); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //val axis 2 (right)
        ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(1234525); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.R);
        ctValAx.addNewCrossAx().setVal(1234525); //id of the cat axis
        ctValAx.addNewCrosses().setVal(STCrosses.MAX); //this val axis crosses the cat axis at max value
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);
    }


    /***
     * 导出PM2.5
     */
    public static void exportPM25(XSSFSheet sheet, List<Double> doubleListPM25,String title) throws IOException {


        Row row;
        Cell cell;
        row = sheet.createRow(1);
        cell = row.createCell(9);
        cell.setCellValue(title+"24时PM2.5均值报表");


        row = sheet.createRow(2);
        row.createCell(0).setCellValue("时间");
        row.createCell(1).setCellValue("PM2.5(μg/m³)");
//        row.createCell(2).setCellValue("Lines");

        for (int r = 0; r <= 23; r++) {
            Row row1 = sheet.createRow(r + 3);
            cell = row1.createCell(0);
            cell.setCellValue((r) + "时");

            //Co2的值
            cell = row1.createCell(1);
            cell.setCellValue(keepTwo(doubleListPM25.get(r)));

        }

        /**
         * 画出图表
         */
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
//        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 4, 0, 11, 15);
        /**
         * 调整距离
         */
        XSSFClientAnchor anchor = (XSSFClientAnchor) drawing.createAnchor(0, 0, 0, 0, 3, 3, 21, 28);

        Chart chart = drawing.createChart(anchor);

        CTChart ctChart = ((XSSFChart) chart).getCTChart();
        CTPlotArea ctPlotArea = ctChart.getPlotArea();

        //the bar chart
        CTBarChart ctBarChart = ctPlotArea.addNewBarChart();
        CTBoolean ctBoolean = ctBarChart.addNewVaryColors();
        ctBoolean.setVal(true);
        ctBarChart.addNewBarDir().setVal(STBarDir.COL);
//        ctBarChart.add

        //the bar series
        CTBarSer ctBarSer = ctBarChart.addNewSer();
        CTSerTx ctSerTx = ctBarSer.addNewTx();
        CTStrRef ctStrRef = ctSerTx.addNewStrRef();
        ctStrRef.setF("PM2.5!$B$1");
        ctBarSer.addNewIdx().setVal(0);
        CTAxDataSource cttAxDataSource = ctBarSer.addNewCat();
        ctStrRef = cttAxDataSource.addNewStrRef();
        ctStrRef.setF("PM2.5!$A$4:$A$27");
        CTNumDataSource ctNumDataSource = ctBarSer.addNewVal();
        CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
        ctNumRef.setF("PM2.5!$B$4:$B$27");

        //at least the border lines in Libreoffice Calc ;-)
//        ctBarSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[] {0,0,0});

        //telling the BarChart that it has axes and giving them Ids
        ctBarChart.addNewAxId().setVal(123455); //cat axis 1 (bars)
        ctBarChart.addNewAxId().setVal(123456); //val axis 1 (left)

        //the line chart
        CTLineChart ctLineChart = ctPlotArea.addNewLineChart();
        ctBoolean = ctLineChart.addNewVaryColors();
        ctBoolean.setVal(false);

        //the line series
        CTLineSer ctLineSer = ctLineChart.addNewSer();
        ctSerTx = ctLineSer.addNewTx();
        ctStrRef = ctSerTx.addNewStrRef();
        ctStrRef.setF("PM2.5!$C$1");
        ctLineSer.addNewIdx().setVal(1);
        cttAxDataSource = ctLineSer.addNewCat();
        ctStrRef = cttAxDataSource.addNewStrRef();
        ctStrRef.setF("PM2.5!$A$4:$A$27");
        ctNumDataSource = ctLineSer.addNewVal();
        ctNumRef = ctNumDataSource.addNewNumRef();
        ctNumRef.setF("PM2.5!$C$4:$C$27");

        //at least the border lines in Libreoffice Calc ;-)
//        ctLineSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[] {0,0,0});

        //telling the LineChart that it has axes and giving them Ids
        ctLineChart.addNewAxId().setVal(123458); //cat axis 2 (lines)
        ctLineChart.addNewAxId().setVal(123459); //val axis 2 (right)

        //cat axis 1 (bars)
        CTCatAx ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(123455); //id of the cat axis
        CTScaling ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctCatAx.addNewDelete().setVal(false);
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(123456); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //val axis 1 (left)
        CTValAx ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(123456); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.L);
        ctValAx.addNewCrossAx().setVal(123455); //id of the cat axis
        ctValAx.addNewCrosses().setVal(STCrosses.AUTO_ZERO); //this val axis crosses the cat axis at zero
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //cat axis 2 (lines)
        ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(1234525); //id of the cat axis
        ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);

        ctCatAx.addNewDelete().setVal(true); //this cat axis is deleted
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(1234525); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //val axis 2 (right)
        ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(1234525); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.R);
        ctValAx.addNewCrossAx().setVal(1234525); //id of the cat axis
        ctValAx.addNewCrosses().setVal(STCrosses.MAX); //this val axis crosses the cat axis at max value
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

    }


    /***
     * 导出湿度
     * @param sheet
     * @throws IOException
     */
    public static void exportHumidy(XSSFSheet sheet, List<Double> doubleListHumidity,String title) throws IOException {

        Row row;
        Cell cell;
        row = sheet.createRow(1);
        cell = row.createCell(9);
        cell.setCellValue(title+"24时湿度均值报表");

        row = sheet.createRow(2);
        row.createCell(0).setCellValue("时间");
        row.createCell(1).setCellValue("湿度(%)");
//        row.createCell(2).setCellValue("Lines");

        for (int r = 0; r <= 23; r++) {
            Row row1 = sheet.createRow(r + 3);
            cell = row1.createCell(0);
            cell.setCellValue((r) + "时");

            //Co2的值
            cell = row1.createCell(1);
            cell.setCellValue(keepTwo(doubleListHumidity.get(r)));

        }

        /**
         * 画出图表
         */
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
//        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 4, 0, 11, 15);
        /**
         * 调整距离
         */
        XSSFClientAnchor anchor = (XSSFClientAnchor) drawing.createAnchor(0, 0, 0, 0, 3, 3, 21, 28);

        Chart chart = drawing.createChart(anchor);

        CTChart ctChart = ((XSSFChart) chart).getCTChart();
        CTPlotArea ctPlotArea = ctChart.getPlotArea();

        //the bar chart
        CTBarChart ctBarChart = ctPlotArea.addNewBarChart();
        CTBoolean ctBoolean = ctBarChart.addNewVaryColors();
        ctBoolean.setVal(true);
        ctBarChart.addNewBarDir().setVal(STBarDir.COL);
//        ctBarChart.add

        //the bar series
        CTBarSer ctBarSer = ctBarChart.addNewSer();
        CTSerTx ctSerTx = ctBarSer.addNewTx();
        CTStrRef ctStrRef = ctSerTx.addNewStrRef();
        ctStrRef.setF("湿度!$B$3");
        ctBarSer.addNewIdx().setVal(0);
        CTAxDataSource cttAxDataSource = ctBarSer.addNewCat();
        ctStrRef = cttAxDataSource.addNewStrRef();
        ctStrRef.setF("湿度!$A$4:$A$27");
        CTNumDataSource ctNumDataSource = ctBarSer.addNewVal();
        CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
        ctNumRef.setF("湿度!$B$4:$B$27");

        //at least the border lines in Libreoffice Calc ;-)
//        ctBarSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[] {0,0,0});

        //telling the BarChart that it has axes and giving them Ids
        ctBarChart.addNewAxId().setVal(123455); //cat axis 1 (bars)
        ctBarChart.addNewAxId().setVal(123456); //val axis 1 (left)

        //the line chart
        CTLineChart ctLineChart = ctPlotArea.addNewLineChart();
        ctBoolean = ctLineChart.addNewVaryColors();
        ctBoolean.setVal(false);

        //the line series
        CTLineSer ctLineSer = ctLineChart.addNewSer();
        ctSerTx = ctLineSer.addNewTx();
        ctStrRef = ctSerTx.addNewStrRef();
        ctStrRef.setF("湿度!$C$3");
        ctLineSer.addNewIdx().setVal(1);
        cttAxDataSource = ctLineSer.addNewCat();
        ctStrRef = cttAxDataSource.addNewStrRef();
        ctStrRef.setF("湿度!$A$4:$A$27");
        ctNumDataSource = ctLineSer.addNewVal();
        ctNumRef = ctNumDataSource.addNewNumRef();
        ctNumRef.setF("湿度!$C$4:$C$27");

        //at least the border lines in Libreoffice Calc ;-)
//        ctLineSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[] {0,0,0});

        //telling the LineChart that it has axes and giving them Ids
        ctLineChart.addNewAxId().setVal(123458); //cat axis 2 (lines)
        ctLineChart.addNewAxId().setVal(123459); //val axis 2 (right)

        //cat axis 1 (bars)
        CTCatAx ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(123455); //id of the cat axis
        CTScaling ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctCatAx.addNewDelete().setVal(false);
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(123456); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //val axis 1 (left)
        CTValAx ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(123456); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.L);
        ctValAx.addNewCrossAx().setVal(123455); //id of the cat axis
        ctValAx.addNewCrosses().setVal(STCrosses.AUTO_ZERO); //this val axis crosses the cat axis at zero
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //cat axis 2 (lines)
        ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(1234525); //id of the cat axis
        ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);

        ctCatAx.addNewDelete().setVal(true); //this cat axis is deleted
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(1234525); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //val axis 2 (right)
        ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(1234525); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.R);
        ctValAx.addNewCrossAx().setVal(1234525); //id of the cat axis
        ctValAx.addNewCrosses().setVal(STCrosses.MAX); //this val axis crosses the cat axis at max value
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);
    }

    /***
     * 导出温度
     * @param sheet
     * @throws IOException
     */
    public static void exportTemplate(XSSFSheet sheet, List<Double> doubleListTemperature, String title) throws IOException {
        Row row;
        Cell cell;
        row = sheet.createRow(1);
        cell = row.createCell(9);
        cell.setCellValue(title+"24时温度均值报表");



        row = sheet.createRow(2);
        row.createCell(0).setCellValue("时间");
        row.createCell(1).setCellValue("温度(℃)");

        for (int r = 0; r <= 23; r++) {
            Row row1 = sheet.createRow(r + 3);
            cell = row1.createCell(0);
            cell.setCellValue((r) + "时");
            cell = row1.createCell(1);
            cell.setCellValue(keepTwo(doubleListTemperature.get(r)));
        }

        /**
         * 画出图表
         */
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        /**
         * 调整距离
         */
        XSSFClientAnchor anchor = (XSSFClientAnchor) drawing.createAnchor(0, 0, 0, 0, 3, 3, 21, 29);

        Chart chart = drawing.createChart(anchor);

        CTChart ctChart = ((XSSFChart) chart).getCTChart();
        CTPlotArea ctPlotArea = ctChart.getPlotArea();

        //the bar chart
        CTBarChart ctBarChart = ctPlotArea.addNewBarChart();
        CTBoolean ctBoolean = ctBarChart.addNewVaryColors();
        ctBoolean.setVal(true);
        ctBarChart.addNewBarDir().setVal(STBarDir.COL);
//        ctBarChart.add

        //the bar series
        CTBarSer ctBarSer = ctBarChart.addNewSer();
        CTSerTx ctSerTx = ctBarSer.addNewTx();
        CTStrRef ctStrRef = ctSerTx.addNewStrRef();
        ctStrRef.setF("温度!$B$1");
        ctBarSer.addNewIdx().setVal(0);
        CTAxDataSource cttAxDataSource = ctBarSer.addNewCat();
        ctStrRef = cttAxDataSource.addNewStrRef();
        ctStrRef.setF("温度!$A$4:$A$27");
        CTNumDataSource ctNumDataSource = ctBarSer.addNewVal();
        CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
        ctNumRef.setF("温度!$B$4:$B$27");

        //at least the border lines in Libreoffice Calc ;-)
//        ctBarSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[] {0,0,0});

        //telling the BarChart that it has axes and giving them Ids
        ctBarChart.addNewAxId().setVal(123455); //cat axis 1 (bars)
        ctBarChart.addNewAxId().setVal(123456); //val axis 1 (left)

        //the line chart
        CTLineChart ctLineChart = ctPlotArea.addNewLineChart();
        ctBoolean = ctLineChart.addNewVaryColors();
        ctBoolean.setVal(true);

        //the line series
        CTLineSer ctLineSer = ctLineChart.addNewSer();
        ctSerTx = ctLineSer.addNewTx();
        ctStrRef = ctSerTx.addNewStrRef();
        ctStrRef.setF("温度!$C$3");
        ctLineSer.addNewIdx().setVal(4);
        cttAxDataSource = ctLineSer.addNewCat();
        ctStrRef = cttAxDataSource.addNewStrRef();
        ctStrRef.setF("温度!$A$4:$A$27");
        ctNumDataSource = ctLineSer.addNewVal();
        ctNumRef = ctNumDataSource.addNewNumRef();
        ctNumRef.setF("温度!$C$4:$C$27");


        ctLineChart.addNewAxId().setVal(123458); //cat axis 2 (lines)
        ctLineChart.addNewAxId().setVal(123459); //val axis 2 (right)

        //cat axis 1 (bars)
        CTCatAx ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(123455); //id of the cat axis
        CTScaling ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctCatAx.addNewDelete().setVal(false);
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(123456); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //val axis 1 (left)
        CTValAx ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(123456); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.L);
        ctValAx.addNewCrossAx().setVal(123455); //id of the cat axis
        ctValAx.addNewCrosses().setVal(STCrosses.AUTO_ZERO); //this val axis crosses the cat axis at zero
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //cat axis 2 (lines)
        ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(1234525); //id of the cat axis
        ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);

        ctCatAx.addNewDelete().setVal(true); //this cat axis is deleted
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(1234525); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //val axis 2 (right)
        ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(1234525); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(true);
        ctValAx.addNewAxPos().setVal(STAxPos.R);
        ctValAx.addNewCrossAx().setVal(1234525); //id of the cat axis
        ctValAx.addNewCrosses().setVal(STCrosses.MAX); //this val axis crosses the cat axis at max value
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

    }

    /***
     *导出co2
     */
    public static void exportCO2(XSSFSheet sheet, List<Double> doubleList,String title) throws IOException {


        Row row;
        Cell cell;
        row = sheet.createRow(1);
        cell = row.createCell(9);
        cell.setCellValue(title+"24时CO₂均值报表");

        row = sheet.createRow(2);
        row.createCell(0).setCellValue("时间");
        row.createCell(1).setCellValue("CO₂(ppm)");
//        row.createCell(2).setCellValue("Lines");

        for (int r = 0; r <= 23; r++) {
            Row row1 = sheet.createRow(r + 3);
            cell = row1.createCell(0);
            cell.setCellValue((r) + "时");

            //Co2的值
            cell = row1.createCell(1);
            cell.setCellValue(keepTwo(doubleList.get(r)));

        }

        /**
         * 画出图表
         */
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        /**
         * 调整距离
         */
        XSSFClientAnchor anchor = (XSSFClientAnchor) drawing.createAnchor(0, 0, 0, 0, 3, 3, 21, 28);

        Chart chart = drawing.createChart(anchor);

        CTChart ctChart = ((XSSFChart) chart).getCTChart();
        CTPlotArea ctPlotArea = ctChart.getPlotArea();

        //the bar chart
        CTBarChart ctBarChart = ctPlotArea.addNewBarChart();
        CTBoolean ctBoolean = ctBarChart.addNewVaryColors();
        ctBoolean.setVal(true);
        ctBarChart.addNewBarDir().setVal(STBarDir.COL);
//        ctBarChart.add

        //the bar series
        CTBarSer ctBarSer = ctBarChart.addNewSer();
        CTSerTx ctSerTx = ctBarSer.addNewTx();
        CTStrRef ctStrRef = ctSerTx.addNewStrRef();
        ctStrRef.setF("CO₂!$B$1");
        ctBarSer.addNewIdx().setVal(0);
        CTAxDataSource cttAxDataSource = ctBarSer.addNewCat();
        ctStrRef = cttAxDataSource.addNewStrRef();
        ctStrRef.setF("CO₂!$A$4:$A$27");
        CTNumDataSource ctNumDataSource = ctBarSer.addNewVal();
        CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
        ctNumRef.setF("CO₂!$B$4:$B$27");

        //at least the border lines in Libreoffice Calc ;-)
//        ctBarSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[] {0,0,0});

        //telling the BarChart that it has axes and giving them Ids
        ctBarChart.addNewAxId().setVal(123455); //cat axis 1 (bars)
        ctBarChart.addNewAxId().setVal(123456); //val axis 1 (left)

        //the line chart
        CTLineChart ctLineChart = ctPlotArea.addNewLineChart();
        ctBoolean = ctLineChart.addNewVaryColors();
        ctBoolean.setVal(false);

        //the line series
        CTLineSer ctLineSer = ctLineChart.addNewSer();
        ctSerTx = ctLineSer.addNewTx();
        ctStrRef = ctSerTx.addNewStrRef();
        ctStrRef.setF("CO₂!$C$1");
        ctLineSer.addNewIdx().setVal(1);
        cttAxDataSource = ctLineSer.addNewCat();
        ctStrRef = cttAxDataSource.addNewStrRef();
        ctStrRef.setF("CO₂!$A$4:$A$27");
        ctNumDataSource = ctLineSer.addNewVal();
        ctNumRef = ctNumDataSource.addNewNumRef();
        ctNumRef.setF("CO₂!$C$4:$C$27");

        //at least the border lines in Libreoffice Calc ;-)
//        ctLineSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[] {0,0,0});

        //telling the LineChart that it has axes and giving them Ids
        ctLineChart.addNewAxId().setVal(123458); //cat axis 2 (lines)
        ctLineChart.addNewAxId().setVal(123459); //val axis 2 (right)

        //cat axis 1 (bars)
        CTCatAx ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(123455); //id of the cat axis
        CTScaling ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctCatAx.addNewDelete().setVal(false);
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(123456); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //val axis 1 (left)
        CTValAx ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(123456); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.L);
        ctValAx.addNewCrossAx().setVal(123455); //id of the cat axis
        ctValAx.addNewCrosses().setVal(STCrosses.AUTO_ZERO); //this val axis crosses the cat axis at zero
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //cat axis 2 (lines)
        ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(1234525); //id of the cat axis
        ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);

        ctCatAx.addNewDelete().setVal(true); //this cat axis is deleted
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(1234525); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //val axis 2 (right)
        ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(1234525); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.R);
        ctValAx.addNewCrossAx().setVal(1234525); //id of the cat axis
        ctValAx.addNewCrosses().setVal(STCrosses.MAX); //this val axis crosses the cat axis at max value
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);


    }


    /**
     * 导出pm10
     *
     * @param
     * @throws IOException
     */
    public static void exportPM10(XSSFSheet sheet, List<Double> doubleListPM10,String title) throws IOException {


        Row row;
        Cell cell;
        row = sheet.createRow(1);
        cell = row.createCell(9);
        cell.setCellValue(title+"24时PM10均值报表");

        row = sheet.createRow(2);
        row.createCell(0).setCellValue("时间");
        row.createCell(1).setCellValue("PM10(μg/m³)");
//        row.createCell(2).setCellValue("Lines");

        for (int r = 0; r <= 23; r++) {
            Row row1 = sheet.createRow(r + 3);
            cell = row1.createCell(0);
            cell.setCellValue((r) + "时");
            //Co2的值
            cell = row1.createCell(1);
            cell.setCellValue(keepTwo(doubleListPM10.get(r)));
        }

        /**
         * 画出图表
         */
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
//        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 4, 0, 11, 15);
        /**
         * 调整距离
         */
        XSSFClientAnchor anchor = (XSSFClientAnchor) drawing.createAnchor(0, 0, 0, 0, 3, 3, 21, 28);

        Chart chart = drawing.createChart(anchor);

        CTChart ctChart = ((XSSFChart) chart).getCTChart();
        CTPlotArea ctPlotArea = ctChart.getPlotArea();

        //the bar chart
        CTBarChart ctBarChart = ctPlotArea.addNewBarChart();
        CTBoolean ctBoolean = ctBarChart.addNewVaryColors();
        ctBoolean.setVal(true);
        ctBarChart.addNewBarDir().setVal(STBarDir.COL);
//        ctBarChart.add

        //the bar series
        CTBarSer ctBarSer = ctBarChart.addNewSer();
        CTSerTx ctSerTx = ctBarSer.addNewTx();
        CTStrRef ctStrRef = ctSerTx.addNewStrRef();
        ctStrRef.setF("PM10!$B$1");
        ctBarSer.addNewIdx().setVal(0);
        CTAxDataSource cttAxDataSource = ctBarSer.addNewCat();
        ctStrRef = cttAxDataSource.addNewStrRef();
        ctStrRef.setF("PM10!$A$4:$A$27");
        CTNumDataSource ctNumDataSource = ctBarSer.addNewVal();
        CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
        ctNumRef.setF("PM10!$B$4:$B$27");

        //at least the border lines in Libreoffice Calc ;-)
//        ctBarSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[] {0,0,0});

        //telling the BarChart that it has axes and giving them Ids
        ctBarChart.addNewAxId().setVal(123455); //cat axis 1 (bars)
        ctBarChart.addNewAxId().setVal(123456); //val axis 1 (left)

        //the line chart
        CTLineChart ctLineChart = ctPlotArea.addNewLineChart();
        ctBoolean = ctLineChart.addNewVaryColors();
        ctBoolean.setVal(false);

        //the line series
        CTLineSer ctLineSer = ctLineChart.addNewSer();
        ctSerTx = ctLineSer.addNewTx();
        ctStrRef = ctSerTx.addNewStrRef();
        ctStrRef.setF("PM10!$C$1");
        ctLineSer.addNewIdx().setVal(1);
        cttAxDataSource = ctLineSer.addNewCat();
        ctStrRef = cttAxDataSource.addNewStrRef();
        ctStrRef.setF("PM10!$A$4:$A$27");
        ctNumDataSource = ctLineSer.addNewVal();
        ctNumRef = ctNumDataSource.addNewNumRef();
        ctNumRef.setF("PM10!$C$4:$C$27");

        //at least the border lines in Libreoffice Calc ;-)
//        ctLineSer.addNewSpPr().addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[] {0,0,0});

        //telling the LineChart that it has axes and giving them Ids
        ctLineChart.addNewAxId().setVal(123458); //cat axis 2 (lines)
        ctLineChart.addNewAxId().setVal(123459); //val axis 2 (right)

        //cat axis 1 (bars)
        CTCatAx ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(123455); //id of the cat axis
        CTScaling ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctCatAx.addNewDelete().setVal(false);
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(123456); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //val axis 1 (left)
        CTValAx ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(123456); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.L);
        ctValAx.addNewCrossAx().setVal(123455); //id of the cat axis
        ctValAx.addNewCrosses().setVal(STCrosses.AUTO_ZERO); //this val axis crosses the cat axis at zero
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //cat axis 2 (lines)
        ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(1234525); //id of the cat axis
        ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);

        ctCatAx.addNewDelete().setVal(true); //this cat axis is deleted
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(1234525); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //val axis 2 (right)
        ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(1234525); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.R);
        ctValAx.addNewCrossAx().setVal(1234525); //id of the cat axis
        ctValAx.addNewCrosses().setVal(STCrosses.MAX); //this val axis crosses the cat axis at max value
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);
    }


    /***
     * execl导出
     * @param list
     * @param response
     * @throws IOException
     * poi
     */
    public static void createExcel(List<DemoExport> list, HttpServletResponse response, String name) throws IOException {
        // 创建一个Excel文件
        HSSFWorkbook workbook = new HSSFWorkbook();
        // 创建一个工作表
        HSSFSheet sheet = workbook.createSheet(DateUtils.getLastMonth() + name + "空气质量检测报表");


        // 添加表头行
        HSSFRow hssfRow = sheet.createRow(0);
        hssfRow.setHeightInPoints(30);

        // 设置单元格格式居中
        HSSFCellStyle cellStyle = workbook.createCellStyle();

//        cellStyle.setAlignment(HSSFCellStyle);
//        //垂直居中
//        cellStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);


        // 添加表头内容
        HSSFCell headCell = hssfRow.createCell(0);
        headCell.setCellValue("序号");
        headCell.setCellStyle(cellStyle);


        headCell = hssfRow.createCell(1);
        headCell.setCellValue("点位");
        headCell.setCellStyle(cellStyle);

        headCell = hssfRow.createCell(2);
        headCell.setCellValue("日期");
        headCell.setCellStyle(cellStyle);


        headCell = hssfRow.createCell(3);
        headCell.setCellValue("温度(℃)");
        headCell.setCellStyle(cellStyle);

        headCell = hssfRow.createCell(4);
        headCell.setCellValue("湿度(%)");
        headCell.setCellStyle(cellStyle);


        headCell = hssfRow.createCell(5);
        headCell.setCellValue("PM2.5");
        headCell.setCellStyle(cellStyle);

        headCell = hssfRow.createCell(6);
        headCell.setCellValue("PM10");
        headCell.setCellStyle(cellStyle);

        headCell = hssfRow.createCell(7);
        headCell.setCellValue("PM1.0");
        headCell.setCellStyle(cellStyle);

        headCell = hssfRow.createCell(8);
        headCell.setCellValue("CO₂");
        headCell.setCellStyle(cellStyle);

        headCell = hssfRow.createCell(9);
        headCell.setCellValue("甲醛");
        headCell.setCellStyle(cellStyle);


        for (int i = 0; i < list.size(); i++) {
            hssfRow = sheet.createRow(i + 1);
            DemoExport demoExport = list.get(i);

            HSSFCell cell = hssfRow.createCell(0);
            cell.setCellValue(i + 1);
            cell.setCellStyle(cellStyle);

            cell = hssfRow.createCell(1);
            cell.setCellValue(demoExport.getPointPosition());
            cell.setCellStyle(cellStyle);

            cell = hssfRow.createCell(2);
            cell.setCellValue(demoExport.getDate());
            cell.setCellStyle(cellStyle);


            cell = hssfRow.createCell(3);
            cell.setCellValue(demoExport.getTemperature().equals("null") ? "" : demoExport.getTemperature());
            cell.setCellStyle(cellStyle);


            cell = hssfRow.createCell(4);
            cell.setCellValue(demoExport.getHumidity().equals("null") ? "" : demoExport.getHumidity());
            cell.setCellStyle(cellStyle);

            cell = hssfRow.createCell(5);
            cell.setCellValue(demoExport.getPM25().equals("null") ? "" : demoExport.getPM25());
            cell.setCellStyle(cellStyle);

            cell = hssfRow.createCell(6);
            cell.setCellValue(demoExport.getPm10().equals("null") ? "" : demoExport.getPm10());
            cell.setCellStyle(cellStyle);


            cell = hssfRow.createCell(7);
            cell.setCellValue(demoExport.getPm100().equals("null") ? "" : demoExport.getPm100());
            cell.setCellStyle(cellStyle);

            cell = hssfRow.createCell(8);
            cell.setCellValue(demoExport.getCo2().equals("null") ? "" : demoExport.getCo2());
            cell.setCellStyle(cellStyle);

            cell = hssfRow.createCell(9);
            cell.setCellValue(demoExport.getCH2o().equals("null") ? "" : demoExport.getCH2o());
            cell.setCellStyle(cellStyle);
        }


        // 添加数据内容
        String lastMonth = DateUtils.getLastMonth();
        ExeclExportUtils.export(workbook, response, lastMonth + name);


    }


    /**
     * 获取到需要导出的数据
     * Co2
     */
    public List<Double> getList(List<TsKvHourHistoryEntity> list, String type) {


        /**
         * 24时的平均数据
         */
        List<Double> doubleList = new LinkedList<>();

        Map<String, List<TsKvHourHistoryEntity>> listMap = list.stream().collect(Collectors.groupingBy(TsKvHourHistoryEntity::getKey));


        List<TsKvHourHistoryEntity> entityList = listMap.get(type);




        /**
         * 过去一个月的数据
         */
        int recond = entityList == null ? 0 : (entityList.size()) / DateUtils.getAllDay(DateUtils.getLastMonth());

        int num =0;
        // 得到24个小时的数据 点的数据
        for (int i = 0; i < 24; i++) {
            double sum = 0.0;
            sum = sum +entityList.get(recond*i+num).getValue();
            doubleList.add(sum);
            num++;
        }


        System.out.println(doubleList.size()+"我是导出的数据");

        return doubleList;
    }


    /**
     * 保留2位小数
     */
    public static Double keepTwo(Double d) {

        BigDecimal b = new BigDecimal(d);
        d = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        return d;

    }

    /***
     * 获取到平均值
     * @return
     */
    private Double getAvg(Integer num, Integer recond, List<TsKvHourHistoryEntity> entityList) {
        Integer dayAvg = recond / 24;


        Double sum = 0.0;

        if (entityList != null) {

            for (int i = 0; i < 24; i++) {

            }
        }


        return sum / 24;

    }


}



