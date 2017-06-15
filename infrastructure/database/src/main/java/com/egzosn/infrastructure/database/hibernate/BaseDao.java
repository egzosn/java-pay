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

import net.zz.dao.params.Params;
import net.zz.dao.params.Where;
import net.zz.dao.params.enums.Restriction;
import javax.persistence.Query;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by ZaoSheng on 2015/12/29.
 */
public class BaseDao<T, PK extends Serializable>  extends SupportHibernateDao<T, PK> {


    protected String getHQL(Params params) {
        return String.format(" from %s %s %s",entityClass.getName(), params.alias(),  params.builderAttrs().getSqlString());
    }

    /**
     * 通过orm实体属性名称查询全部
     *
     * @param propertyName
     *            orm实体属性名称
     * @param value
     *            值
     * @return List
     */
    public List<T> findByProperty(String propertyName, Object value) {
        return findByProperty(propertyName, value, null);
    }

    /**
     * 通过orm实体属性名称查询全部
     *
     * @param propertyName
     *            orm实体属性名称
     * @param value
     *            值
     * @param restriction
     *            约束名称 参考 {@link net.zz.dao.params.enums.Restriction}
     * @return List
     */
    @SuppressWarnings("unchecked")
    public List<T> findByProperty(String propertyName, Object value, Restriction restriction) {
        if (null == restriction) {
            restriction = Restriction.EQ;
        }

        Where where = new Where();
        where.setAlias("z");
        where.and(propertyName, value, restriction, "z");

        Query query = createHQLQuery(getHQL(where), where.getAttrs());
        return query.getResultList();

    }

    /**
     * 通过orm实体的属性名称查询单个orm实体
     *
     * @param propertyName
     *            属性名称
     * @param value
     *            值
     * @return Object
     */
    public T findUniqueByProperty(String propertyName, Object value) {
        return findUniqueByProperty(propertyName, value, null);
    }

    /**
     * 通过orm实体的属性名称查询单个orm实体
     *
     * @param propertyName
     *            属性名称
     * @param value
     *            值
     * @param restriction
     *            约束名称 参考 {@link net.zz.dao.params.enums.Restriction} 的所有实现类
     * @return Object
     */
    public T findUniqueByProperty(String propertyName, Object value, Restriction restriction) {

        if (null == restriction) {
            restriction = Restriction.EQ;
        }

        Where where = new Where();
        where.setAlias("z");
        where.and(propertyName, value, restriction, "z");
        Query query = createHQLQuery(getHQL(where), where.getAttrs());
        return (T) query.getSingleResult();

    }

    /**
     * @param hql
     * @param attrs
     * @return
     */
    private T findUniqueByQuerys(String hql, Map<String, Object> attrs) {

        return (T) createHQLQuery(hql, attrs).getSingleResult();
    }



    public T uniqueQueryUseHQL(Params params) {
        String hql = getHQL(params);
        T value = this.findUniqueByQuerys(hql, params.getAttrs());
        return value;
    }


    /**
     *  分页查找
     * @param params 查询参数集
     * @param isPage 是否分页
     * @return
     */
    public Page<T> queryPageUseHQL(Params params, boolean isPage) {
        String hql = getHQL(params);
       return  isPage ? pageQueryHQL(hql, params.getAttrs(), params.getPage().getPageIndex(), params.getPage().getPageSize()) : pageQueryHQL(hql, params.getAttrs());
    }

    /**
     *  分页查找
     * @param params 查询参数集
     * @param isPage 是否分页
     * @return
     */
    public List<T> queryListUseHQL(Params params, boolean isPage) {
        String hql = getHQL(params);
        return  isPage ? listMoreByHQL(hql, params.getAttrs(), params.getPage().getPageIndex(), params.getPage().getPageSize()) : listByHQL(hql, params.getAttrs());
    }

    /**
     * @param updateField 需要更新的字段
     * @param params      条件参数
     */
    public int update(Map<String, Object> updateField, Params params) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("UPDATE %s {alias} set ", entityClass.getName()));
        for (String colName : updateField.keySet()) {
            sb.append(String.format("{alias}.%s=:%s, ", colName, colName));
        }
        sb.deleteCharAt(sb.length() - 2);
        String hql = sb.toString().replace("{alias}", params.alias()) + params.builderAttrs().getSqlString();
        updateField.putAll(params.getAttrs());
        return execute(hql, updateField);
    }



    /**
     * @param isHQL 是否为HQL
     * @param updateField 需要更新的字段
     * @param params      条件参数
     */
    public int update(boolean isHQL, Map<String, Object> updateField, Params params) {
   
        List<Object> paras = new ArrayList<>();
        StringBuilder sql = getUpdateField(isHQL, updateField, params.alias(), paras);

        if (isHQL){
            sql.append(params.builderParas().getSqlString().replace( params.alias() + ".", ""));
        }else {
            sql.append(params.builderParas().getSqlString());
        }
        paras.addAll(params.getParas());

        return executeSql(sql.toString(), paras.toArray());
    }


}
