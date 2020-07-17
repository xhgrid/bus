/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2020 aoju.org and other contributors.                      *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 ********************************************************************************/
package org.aoju.bus.office.support.excel;

import org.aoju.bus.core.lang.Normal;
import org.aoju.bus.core.lang.Symbol;
import org.aoju.bus.core.toolkit.DateKit;
import org.aoju.bus.office.support.excel.cell.CellEditor;
import org.aoju.bus.office.support.excel.cell.CellLocation;
import org.aoju.bus.office.support.excel.cell.FormulaCellValue;
import org.aoju.bus.office.support.excel.editors.TrimEditor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.ss.util.SheetUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Excel表格中单元格工具类
 *
 * @author Kimi Liu
 * @version 6.0.3
 * @since JDK 1.8+
 */
public class CellKit {

    /**
     * 获取单元格值
     *
     * @param cell {@link Cell}单元格
     * @return 值，类型可能为：Date、Double、Boolean、String
     */
    public static Object getCellValue(Cell cell) {
        return getCellValue(cell, false);
    }

    /**
     * 获取单元格值
     *
     * @param cell            {@link Cell}单元格
     * @param isTrimCellValue 如果单元格类型为字符串,是否去掉两边空白符
     * @return 值, 类型可能为：Date、Double、Boolean、String
     */
    public static Object getCellValue(Cell cell, boolean isTrimCellValue) {
        if (null == cell) {
            return null;
        }
        return getCellValue(cell, cell.getCellType(), isTrimCellValue);
    }

    /**
     * 获取单元格值
     *
     * @param cell       {@link Cell}单元格
     * @param cellEditor 单元格值编辑器 可以通过此编辑器对单元格值做自定义操作
     * @return 值, 类型可能为：Date、Double、Boolean、String
     */
    public static Object getCellValue(Cell cell, CellEditor cellEditor) {
        if (null == cell) {
            return null;
        }
        return getCellValue(cell, cell.getCellType(), cellEditor);
    }

    /**
     * 获取单元格值
     *
     * @param cell            {@link Cell}单元格
     * @param cellType        单元格值类型{@link CellType}枚举
     * @param isTrimCellValue 如果单元格类型为字符串,是否去掉两边空白符
     * @return 值, 类型可能为：Date、Double、Boolean、String
     */
    public static Object getCellValue(Cell cell, CellType cellType, final boolean isTrimCellValue) {
        return getCellValue(cell, cellType, isTrimCellValue ? new TrimEditor() : null);
    }

    /**
     * 获取单元格值
     * 如果单元格值为数字格式,则判断其格式中是否有小数部分,无则返回Long类型,否则返回Double类型
     *
     * @param cell       {@link Cell}单元格
     * @param cellType   单元格值类型{@link CellType}枚举,如果为{@code null}默认使用cell的类型
     * @param cellEditor 单元格值编辑器 可以通过此编辑器对单元格值做自定义操作
     * @return 值, 类型可能为：Date、Double、Boolean、String
     */
    public static Object getCellValue(Cell cell, CellType cellType, CellEditor cellEditor) {
        if (null == cell) {
            return null;
        }
        if (null == cellType) {
            cellType = cell.getCellType();
        }

        if (CellType.BLANK == cellType) {
            // 空白单元格可能为合并单元格
            cell = getMergedRegionCell(cell);
            cellType = cell.getCellType();
        }

        Object value;
        switch (cellType) {
            case NUMERIC:
                value = getNumericValue(cell);
                break;
            case BOOLEAN:
                value = cell.getBooleanCellValue();
                break;
            case FORMULA:
                // 遇到公式时查找公式结果类型
                value = getCellValue(cell, cell.getCachedFormulaResultType(), cellEditor);
                break;
            case BLANK:
                value = Normal.EMPTY;
                break;
            case ERROR:
                final FormulaError error = FormulaError.forInt(cell.getErrorCellValue());
                value = (null == error) ? Normal.EMPTY : error.getString();
                break;
            default:
                value = cell.getStringCellValue();
        }
        return null == cellEditor ? value : cellEditor.edit(cell, value);
    }

    /**
     * 设置单元格值
     * 根据传入的styleSet自动匹配样式
     * 当为头部样式时默认赋值头部样式,但是头部中如果有数字、日期等类型,将按照数字、日期样式设置
     *
     * @param cell     单元格
     * @param value    值
     * @param styleSet 单元格样式集,包括日期等样式
     * @param isHeader 是否为标题单元格
     */
    public static void setCellValue(Cell cell, Object value, StyleSet styleSet, boolean isHeader) {
        if (null == cell) {
            return;
        }

        if (null != styleSet) {
            final CellStyle headCellStyle = styleSet.getHeadCellStyle();
            final CellStyle cellStyle = styleSet.getCellStyle();
            if (isHeader && null != headCellStyle) {
                cell.setCellStyle(headCellStyle);
            } else if (null != cellStyle) {
                cell.setCellStyle(cellStyle);
            }
        }

        if (value instanceof Date) {
            if (null != styleSet && null != styleSet.getCellStyleForDate()) {
                cell.setCellStyle(styleSet.getCellStyleForDate());
            }
        } else if (value instanceof TemporalAccessor) {
            if (null != styleSet && null != styleSet.getCellStyleForDate()) {
                cell.setCellStyle(styleSet.getCellStyleForDate());
            }
        } else if (value instanceof Calendar) {
            if (null != styleSet && null != styleSet.getCellStyleForDate()) {
                cell.setCellStyle(styleSet.getCellStyleForDate());
            }
        } else if (value instanceof Number) {
            if ((value instanceof Double || value instanceof Float || value instanceof BigDecimal) && null != styleSet && null != styleSet.getCellStyleForNumber()) {
                cell.setCellStyle(styleSet.getCellStyleForNumber());
            }
        }
        setCellValue(cell, value, null);
    }

    /**
     * 设置单元格值
     * 根据传入的styleSet自动匹配样式
     * 当为头部样式时默认赋值头部样式，但是头部中如果有数字、日期等类型，将按照数字、日期样式设置
     *
     * @param cell  单元格
     * @param value 值
     * @param style 自定义样式，null表示无样式
     */
    public static void setCellValue(Cell cell, Object value, CellStyle style) {
        if (null == cell) {
            return;
        }

        if (null != style) {
            cell.setCellStyle(style);
        }

        if (null == value) {
            cell.setCellValue(Normal.EMPTY);
        } else if (value instanceof FormulaCellValue) {
            // 公式
            cell.setCellFormula(((FormulaCellValue) value).getValue());
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else if (value instanceof TemporalAccessor) {
            if (value instanceof Instant) {
                cell.setCellValue(Date.from((Instant) value));
            } else if (value instanceof LocalDateTime) {
                cell.setCellValue((LocalDateTime) value);
            } else if (value instanceof LocalDate) {
                cell.setCellValue((LocalDate) value);
            }
        } else if (value instanceof Calendar) {
            cell.setCellValue((Calendar) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof RichTextString) {
            cell.setCellValue((RichTextString) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 获取已有行或创建新行
     *
     * @param row       Excel表的行
     * @param cellIndex 列号
     * @return {@link Row}
     */
    public static Cell getOrCreateCell(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (null == cell) {
            cell = row.createCell(cellIndex);
        }
        return cell;
    }

    /**
     * 判断指定的单元格是否是合并单元格
     *
     * @param sheet       {@link Sheet}
     * @param locationRef 单元格地址标识符，例如A11，B5
     * @return 是否是合并单元格
     */
    public static boolean isMergedRegion(Sheet sheet, String locationRef) {
        final CellLocation cellLocation = ExcelKit.toLocation(locationRef);
        return isMergedRegion(sheet, cellLocation.getX(), cellLocation.getY());
    }

    /**
     * 判断指定的单元格是否是合并单元格
     *
     * @param cell {@link Cell}
     * @return 是否是合并单元格
     */
    public static boolean isMergedRegion(Cell cell) {
        return isMergedRegion(cell.getSheet(), cell.getColumnIndex(), cell.getRowIndex());
    }

    /**
     * 判断指定的单元格是否是合并单元格
     *
     * @param sheet {@link Sheet}
     * @param x     列号，从0开始
     * @param y     行号，从0开始
     * @return 是否是合并单元格
     */
    public static boolean isMergedRegion(Sheet sheet, int x, int y) {
        final int sheetMergeCount = sheet.getNumMergedRegions();
        CellRangeAddress ca;
        for (int i = 0; i < sheetMergeCount; i++) {
            ca = sheet.getMergedRegion(i);
            if (y >= ca.getFirstRow() && y <= ca.getLastRow()
                    && x >= ca.getFirstColumn() && x <= ca.getLastColumn()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 合并单元格,可以根据设置的值来合并行和列
     *
     * @param sheet       表对象
     * @param firstRow    起始行,0开始
     * @param lastRow     结束行,0开始
     * @param firstColumn 起始列,0开始
     * @param lastColumn  结束列,0开始
     * @param cellStyle   单元格样式,只提取边框样式
     * @return 合并后的单元格号
     */
    public static int mergingCells(Sheet sheet, int firstRow, int lastRow, int firstColumn, int lastColumn, CellStyle cellStyle) {
        final CellRangeAddress cellRangeAddress = new CellRangeAddress(
                firstRow, // first row (0-based)
                lastRow, // last row (0-based)
                firstColumn, // first column (0-based)
                lastColumn // last column (0-based)
        );

        if (null != cellStyle) {
            RegionUtil.setBorderTop(cellStyle.getBorderTop(), cellRangeAddress, sheet);
            RegionUtil.setBorderRight(cellStyle.getBorderRight(), cellRangeAddress, sheet);
            RegionUtil.setBorderBottom(cellStyle.getBorderBottom(), cellRangeAddress, sheet);
            RegionUtil.setBorderLeft(cellStyle.getBorderLeft(), cellRangeAddress, sheet);
        }
        return sheet.addMergedRegion(cellRangeAddress);
    }

    /**
     * 获取合并单元格的值
     * 传入的x,y坐标(列行数)可以是合并单元格范围内的任意一个单元格
     *
     * @param sheet       {@link Sheet}
     * @param locationRef 单元格地址标识符，例如A11，B5
     * @return 合并单元格的值
     */
    public static Object getMergedRegionValue(Sheet sheet, String locationRef) {
        final CellLocation cellLocation = ExcelKit.toLocation(locationRef);
        return getMergedRegionValue(sheet, cellLocation.getX(), cellLocation.getY());
    }

    /**
     * 获取合并单元格的值
     * 传入的x,y坐标(列行数)可以是合并单元格范围内的任意一个单元格
     *
     * @param sheet {@link Sheet}
     * @param x     列号，从0开始，可以是合并单元格范围中的任意一列
     * @param y     行号，从0开始，可以是合并单元格范围中的任意一行
     * @return 合并单元格的值
     */
    public static Object getMergedRegionValue(Sheet sheet, int x, int y) {
        return getCellValue(getMergedRegionCell(sheet, x, y));
    }

    /**
     * 获取合并单元格
     * 传入的x,y坐标(列行数)可以是合并单元格范围内的任意一个单元格
     *
     * @param cell {@link Cell}
     * @return 合并单元格
     */
    public static Cell getMergedRegionCell(Cell cell) {
        return getMergedRegionCell(cell.getSheet(), cell.getColumnIndex(), cell.getRowIndex());
    }

    /**
     * 获取合并单元格
     * 传入的x,y坐标(列行数)可以是合并单元格范围内的任意一个单元格
     *
     * @param sheet {@link Sheet}
     * @param x     列号，从0开始，可以是合并单元格范围中的任意一列
     * @param y     行号，从0开始，可以是合并单元格范围中的任意一行
     * @return 合并单元格，如果非合并单元格，返回坐标对应的单元格
     */
    public static Cell getMergedRegionCell(Sheet sheet, int x, int y) {
        final List<CellRangeAddress> addrs = sheet.getMergedRegions();

        int firstColumn;
        int lastColumn;
        int firstRow;
        int lastRow;
        for (CellRangeAddress ca : addrs) {
            firstColumn = ca.getFirstColumn();
            lastColumn = ca.getLastColumn();
            firstRow = ca.getFirstRow();
            lastRow = ca.getLastRow();

            if (y >= firstRow && y <= lastRow) {
                if (x >= firstColumn && x <= lastColumn) {
                    return SheetUtil.getCell(sheet, firstRow, firstColumn);
                }
            }
        }

        return SheetUtil.getCell(sheet, y, x);
    }

    /**
     * 获取数字类型的单元格值
     *
     * @param cell 单元格
     * @return 单元格值, 可能为Long、Double、Date
     */
    private static Object getNumericValue(Cell cell) {
        final double value = cell.getNumericCellValue();

        final CellStyle style = cell.getCellStyle();
        if (null != style) {
            final short formatIndex = style.getDataFormat();
            // 判断是否为日期
            if (isDateType(cell, formatIndex)) {
                return DateKit.date(cell.getDateCellValue());
            }

            final String format = style.getDataFormatString();
            // 普通数字
            if (null != format && format.indexOf(Symbol.C_DOT) < 0) {
                final long longPart = (long) value;
                if (((double) longPart) == value) {
                    // 对于无小数部分的数字类型，转为Long
                    return longPart;
                }
            }
        }

        // 某些Excel单元格值为double计算结果，可能导致精度问题，通过转换解决精度问题。
        return Double.parseDouble(NumberToTextConverter.toText(value));
    }

    /**
     * 是否为日期格式
     * 判断方式：
     *
     * <pre>
     * 1、指定序号
     * 2、org.apache.poi.ss.usermodel.DateUtil.isADateFormat方法判定
     * </pre>
     *
     * @param cell        单元格
     * @param formatIndex 格式序号
     * @return 是否为日期格式
     */
    private static boolean isDateType(Cell cell, int formatIndex) {
        // yyyy-MM-dd----- 14
        // yyyy年m月d日---- 31
        // yyyy年m月------- 57
        // m月d日 --------- 58
        // HH:mm---------- 20
        // h时mm分 -------- 32
        if (formatIndex == 14 || formatIndex == 31 || formatIndex == 57 || formatIndex == 58 || formatIndex == 20 || formatIndex == 32) {
            return true;
        }

        return DateUtil.isCellDateFormatted(cell);
    }

}
