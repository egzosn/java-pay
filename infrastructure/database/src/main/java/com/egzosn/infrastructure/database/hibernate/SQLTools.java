/*
 * Copyright 2002-2017 the original  egan.
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


package com.egzosn.infrastructure.database.hibernate;

import org.hibernate.transform.Transformers;
import org.hibernate.type.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @time 2016/9/21 10:36
 * @author: egan
 *  sql 操作的工具类
 */
public final class SQLTools {

    /**
     *  对sql查询结果集映射至对应的对象里
     * @param transferClass 映射的对象
     * @param query 查询记录集
     */
    public static void setQueryTransform(Class transferClass, org.hibernate.SQLQuery query) {

        if (transferClass != null) {
            List<Field> fieldList = new ArrayList<Field>();
            Field[] fields = transferClass.getDeclaredFields();
            for (Field field : fields) {
                fieldList.add(field);
            }
            // 暂不使用递归获取父类fields方法 (getSuperclassFields)
            Class superclass = transferClass.getSuperclass();
            if (superclass != null) {
                Field[] superFields = superclass.getDeclaredFields();
                for (Field superField : superFields) {
                    fieldList.add(superField);
                }
            }
            fields = fieldList.toArray(new Field[fieldList.size()]);
            // TODO 2016/9/21 10:16 author: egan  获取select的每一个字段
            String[] selectFieldSQLs = getSelectFieldSQL(query.getQueryString()).split("(,|\\s)");
            Arrays.sort(selectFieldSQLs);
            for (int i = 0; i < fields.length; i++) {
                // TODO 2016/9/21 10:16 author: egan 映射对象Field进行匹配
                if (Arrays.binarySearch(selectFieldSQLs, fields[i].getName()) < 0) {
                    continue;
                }
                if (fields[i].getType() == Short.class || fields[i].getType() == short.class) {
                    query.addScalar(fields[i].getName(), new ShortType());
                } else if (fields[i].getType() == Long.class || fields[i].getType() == long.class) {
                    query.addScalar(fields[i].getName(), new LongType());
                } else if (fields[i].getType() == Timestamp.class) {
                    query.addScalar(fields[i].getName(), new TimestampType());
                } else if (fields[i].getType() == Date.class || fields[i].getType() == java.util.Date.class) {
                    query.addScalar(fields[i].getName(), new DateType());
                } else if (fields[i].getType() == BigDecimal.class) {
                    query.addScalar(fields[i].getName(), new BigDecimalType());
                } else if (fields[i].getType() == Double.class || fields[i].getType() == double.class) {
                    query.addScalar(fields[i].getName(), new DoubleType());
                } else if (fields[i].getType() == Float.class || fields[i].getType() == float.class) {
                    query.addScalar(fields[i].getName(), new FloatType());
                } else if (fields[i].getType() == Integer.class || fields[i].getType() == int.class) {
                    query.addScalar(fields[i].getName(), new IntegerType());
                } else if (fields[i].getType() == Boolean.class || fields[i].getType() == boolean.class) {
                    query.addScalar(fields[i].getName(), new BooleanType());
                } else {
                    query.addScalar(fields[i].getName(), new StringType());
                }
                query.setResultTransformer(Transformers.aliasToBean(transferClass));
            }
        }
    }

    public   static String getSelectFieldSQL(final String sql) {
        String upperSql = sql.toUpperCase();
        return sql.trim().substring(7, upperSql.indexOf(" FROM "));
    }

    public static  String getCountSQL(final String sql) {

        String countSql = "SELECT COUNT(*) ";
        String upperSql = sql.toUpperCase();
        int start = upperSql.indexOf("FROM ");
        int end = upperSql.lastIndexOf("ORDER BY ");
        countSql += sql.substring(start, end == -1 ? sql.length() : end);

        // System.out.println(countSql);
        return countSql;
    }

    public static  String getCountSQL(final String sql, String countField) {

        String countSql = String.format("SELECT DISTINCT COUNT(%s) ", null == countField ? "*" : countField);
        String upperSql = sql.toUpperCase();
        int start = upperSql.indexOf("FROM ");
        int end = upperSql.lastIndexOf("ORDER BY ");
        countSql += sql.substring(start, end == -1 ? sql.length() : end);

        // System.out.println(countSql);
        return countSql;
    }

    public static  String removeGROUP(final String sql) {

        String upperSql = sql.toUpperCase();
        int end = upperSql.indexOf(" GROUP ");
        return -1 == end ? sql : sql.substring(0, end);
    }

}
