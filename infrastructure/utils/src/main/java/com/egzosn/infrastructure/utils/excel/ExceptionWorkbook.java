/*
 * Copyright 2002-2012 the original author egan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.egzosn.infrastructure.utils.excel;

import org.apache.poi.ss.usermodel.*;

import java.util.List;

/**
 * 主要用来写带样式的文档
 * Created by ZaoSheng on 2016/3/17.
 */
public class ExceptionWorkbook<T> extends ExcelTools<T> {

    private Font font = null;
    private CellStyle style = null;

    protected void writeRow(int rowNum, Row row, List<String> exceptionComm) {
        try {
            Row failureRow = getFailureSheet().createRow(rowNum);
            for (Integer p : propertys.keySet()) {
//                if (null != row.getCell(p)) {
                Cell cellTmp = row.getCell(p);
                Object value = null == cellTmp ? null : getValue(cellTmp);
                if (null == value) {
                    value = "";
                }
                Cell cell = failureRow.createCell(p);
                cell.setCellValue(value.toString());
                int idx;
                String nowComm = propertys.get(p);
                if (exceptionComm.contains(nowComm)) {
                    cell.setCellStyle(exceptionStyle());

                }

            }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public CellStyle exceptionStyle() {
        CellStyle cellStyle = createCellStyle();
        cellStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        Font font = createFont();
        font.setColor(IndexedColors.RED.getIndex());
        cellStyle.setFont(font);
        return cellStyle;
    }

    public CellStyle createCellStyle() {
        if (null == font) {
            style = getFailureWork().createCellStyle();
        }
        return style;
    }

    public Font createFont() {
        if (null == font) {
            font = getFailureWork().createFont();
        }
        return font;
    }
}
