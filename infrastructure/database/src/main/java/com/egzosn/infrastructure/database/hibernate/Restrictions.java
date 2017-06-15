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


import java.io.Serializable;
import java.util.*;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import javax.persistence.criteria.CriteriaBuilder.In;


/**
 * Query基类<br>
 *
 * @describe：封装JPA CriteriaBuilder查询条件
 * @author：yangjian1004
 * @since：2011-11-23
 */
public class Restrictions implements Serializable {

    private static final long serialVersionUID = 5064932771068929342L;

    private EntityManager entityManager;

    /**
     * 要查询的模型对象
     */
    private Class clazz;

    /**
     * 查询条件列表
     */
    private Root from;

    private List<Predicate> predicates;

    private CriteriaQuery criteriaQuery;

    private CriteriaBuilder criteriaBuilder;

    /**
     * 排序方式列表
     */
    private List<Order> orders;

    /**
     * 关联模式
     */
    private Map<String, Restrictions> subQuery;

    private Map<String, Restrictions> linkQuery;

    private String projection;

    /**
     * 或条件
     */
    private List<Restrictions> orQuery;

    private String groupBy;

    private Restrictions() {
    }

    private Restrictions(Class clazz, EntityManager entityManager) {
        this.clazz = clazz;
        this.entityManager = entityManager;
        this.criteriaBuilder = this.entityManager.getCriteriaBuilder();
        this.criteriaQuery = criteriaBuilder.createQuery(this.clazz);
        this.from = criteriaQuery.from(this.clazz);
        this.predicates = new ArrayList();
        this.orders = new ArrayList();
    }

    /**
     * 通过类创建查询条件
     */
    public static Restrictions forClass(Class clazz, EntityManager entityManager) {
        return new Restrictions(clazz, entityManager);
    }

    /**
     * 增加子查询
     */
    private void addSubQuery(String propertyName, Restrictions query) {
        if (this.subQuery == null)
            this.subQuery = new HashMap();

        if (query.projection == null)
            throw new RuntimeException("子查询字段未设置");

        this.subQuery.put(propertyName, query);
    }

    private void addSubQuery(Restrictions query) {
        addSubQuery(query.projection, query);
    }

    /**
     * 增关联查询
     */
    public void addLinkQuery(String propertyName, Restrictions query) {
        if (this.linkQuery == null)
            this.linkQuery = new HashMap();

        this.linkQuery.put(propertyName, query);
    }

    /**
     * 相等
     */
    public void eq(String propertyName, Object value) {
        if (isNullOrEmpty(value))
            return;
        this.predicates.add(criteriaBuilder.equal(from.get(propertyName), value));
    }

    private boolean isNullOrEmpty(Object value) {
        if (value instanceof String) {
            return value == null || "".equals(value);
        }
        return value == null;
    }

    public void or(List<String> propertyName, Object value) {
        if (isNullOrEmpty(value))
            return;
        if ((propertyName == null) || (propertyName.size() == 0))
            return;
        Predicate predicate = criteriaBuilder.or(criteriaBuilder.equal(from.get(propertyName.get(0)), value));
        for (int i = 1; i < propertyName.size(); ++i)
            predicate = criteriaBuilder.or(predicate, criteriaBuilder.equal(from.get(propertyName.get(i)), value));
        this.predicates.add(predicate);
    }

    public void orLike(List<String> propertyName, String value) {
        if (isNullOrEmpty(value) || (propertyName.size() == 0))
            return;
        if (value.indexOf("%") < 0)
            value = "%" + value + "%";
        Predicate predicate = criteriaBuilder.or(criteriaBuilder.like(from.get(propertyName.get(0)), value.toString()));
        for (int i = 1; i < propertyName.size(); ++i)
            predicate = criteriaBuilder.or(predicate, criteriaBuilder.like(from.get(propertyName.get(i)), value));
        this.predicates.add(predicate);
    }

    /**
     * 空
     */
    public void isNull(String propertyName) {
        this.predicates.add(criteriaBuilder.isNull(from.get(propertyName)));
    }

    /**
     * 非空
     */
    public void isNotNull(String propertyName) {
        this.predicates.add(criteriaBuilder.isNotNull(from.get(propertyName)));
    }

    /**
     * 不相等
     */
    public void notEq(String propertyName, Object value) {
        if (isNullOrEmpty(value)) {
            return;
        }
        this.predicates.add(criteriaBuilder.notEqual(from.get(propertyName), value));
    }

    /**
     * not in
     *
     * @param propertyName 属性名称
     * @param value        值集合
     */
    public void notIn(String propertyName, Collection value) {
        if ((value == null) || (value.size() == 0)) {
            return;
        }
        Iterator iterator = value.iterator();
        In in = criteriaBuilder.in(from.get(propertyName));
        while (iterator.hasNext()) {
            in.value(iterator.next());
        }
        this.predicates.add(criteriaBuilder.not(in));
    }

    /**
     * 模糊匹配
     *
     * @param propertyName 属性名称
     * @param value        属性值
     */
    public void like(String propertyName, String value) {
        if (isNullOrEmpty(value))
            return;
        if (value.indexOf("%") < 0)
            value = "%" + value + "%";
        this.predicates.add(criteriaBuilder.like(from.get(propertyName), value));
    }

    /**
     * 时间区间查询
     *
     * @param propertyName 属性名称
     * @param lo           属性起始值
     * @param go           属性结束值
     */
    public void between(String propertyName, Date lo, Date go) {
        if (!isNullOrEmpty(lo) && !isNullOrEmpty(go)) {
            this.predicates.add(criteriaBuilder.between(from.get(propertyName), lo, go));
        }

        // if (!isNullOrEmpty(lo) && !isNullOrEmpty(go)) {
        // this.predicates.add(criteriaBuilder.lessThan(from.get(propertyName),
        // new DateTime(lo).toString()));
        // }
        // if (!isNullOrEmpty(go)) {
        // this.predicates.add(criteriaBuilder.greaterThan(from.get(propertyName),
        // new DateTime(go).toString()));
        // }

    }

    public void between(String propertyName, Byte lo, Byte go) {
        this.predicates.add(criteriaBuilder.between(from.get(propertyName), lo, go));

    }
    public void between(String propertyName, Short lo, Short go) {
        this.predicates.add(criteriaBuilder.between(from.get(propertyName), lo, go));

    }
    public void between(String propertyName, Integer lo, Integer go) {
        this.predicates.add(criteriaBuilder.between(from.get(propertyName), lo, go));

    }
    public void between(String propertyName, Long lo, Long go) {
        this.predicates.add(criteriaBuilder.between(from.get(propertyName), lo, go));

    }
    public void between(String propertyName, Float lo, Float go) {
        this.predicates.add(criteriaBuilder.between(from.get(propertyName), lo, go));
    }

    public void between(String propertyName, Double lo, Double go) {
        this.predicates.add(criteriaBuilder.between(from.get(propertyName), lo, go));
    }

    /**
     * 小于等于
     *  @param propertyName 属性名称
     * @param value        属性值
     */
    public Restrictions le(String propertyName, Number value) {
        if (isNullOrEmpty(value)) {
            return this;
        }
        this.predicates.add(criteriaBuilder.le(from.get(propertyName), value));
        return this;
    }

    /**
     * 小于
     *  @param propertyName 属性名称
     * @param value        属性值
     */
    public Restrictions lt(String propertyName, Number value) {
        if (isNullOrEmpty(value)) {
            return this;
        }
        this.predicates.add(criteriaBuilder.lt(from.get(propertyName), value));
        return this;
    }

    /**
     * 大于等于
     *  @param propertyName 属性名称
     * @param value        属性值
     */
    public Restrictions ge(String propertyName, Number value) {
        if (isNullOrEmpty(value)) {
            return this;
        }
        this.predicates.add(criteriaBuilder.ge(from.get(propertyName), value));

        return this;
    }

    /**
     * 大于
     *  @param propertyName 属性名称
     * @param value        属性值
     */
    public Restrictions gt(String propertyName, Number value) {
        if (isNullOrEmpty(value)) {
            return this;
        }
        this.predicates.add(criteriaBuilder.gt(from.get(propertyName), value));

        return this;
    }

    /**
     * in
     *  @param propertyName 属性名称
     * @param value        值集合
     */
    public Restrictions in(String propertyName, Collection value) {
        if ((value == null) || (value.size() == 0)) {
            return this;
        }
        Iterator iterator = value.iterator();
        In in = criteriaBuilder.in(from.get(propertyName));
        while (iterator.hasNext()) {
            in.value(iterator.next());
        }
        this.predicates.add(in);
        return this;
    }
   /**
     * in
     *  @param propertyName 属性名称
     * @param value        值集合
    */
    public Restrictions in(String propertyName, Object[] value) {
        if (value == null) {
            return this;
        }

        In in = criteriaBuilder.in(from.get(propertyName));
        int len = value.length;
        if (0 == len){
            return this;
        }
        for (int i = 0; i< len; i++){
            in.value(value[i]);
        }
        this.predicates.add(in);
        return this;
    }

    /**
     * 直接添加JPA内部的查询条件,用于应付一些复杂查询的情况,例如或
     */
    public Restrictions addCriterions(Predicate predicate) {
        this.predicates.add(predicate);
        return this;
    }

    /**
     * 创建查询条件
     *
     * @return JPA离线查询
     */
    public CriteriaQuery newCriteriaQuery() {
        criteriaQuery.where(predicates.toArray(new Predicate[0]));
        if (!isNullOrEmpty(groupBy)) {
            criteriaQuery.groupBy(from.get(groupBy));
        }
        if (this.orders != null) {
            criteriaQuery.orderBy(orders);
        }
        addLinkCondition(this);
        return criteriaQuery;
    }

    private void addLinkCondition(Restrictions query) {

        Map subQuery = query.linkQuery;
        if (subQuery == null)
            return;

        for (Iterator queryIterator = subQuery.keySet().iterator(); queryIterator.hasNext(); ) {
            String key = (String) queryIterator.next();
            Restrictions sub = (Restrictions) subQuery.get(key);
            from.join(key);
            criteriaQuery.where(sub.predicates.toArray(new Predicate[0]));
            addLinkCondition(sub);
        }
    }

    public Restrictions addOrder(String propertyName, String order) {
        if (order == null || propertyName == null)
            return this;

        if (this.orders == null)
            this.orders = new ArrayList();

        if (order.equalsIgnoreCase("asc"))
            this.orders.add(criteriaBuilder.asc(from.get(propertyName)));
        else if (order.equalsIgnoreCase("desc"))
            this.orders.add(criteriaBuilder.desc(from.get(propertyName)));

        return this;
    }

    public Restrictions setOrder(String propertyName, String order) {
        this.orders = null;
        addOrder(propertyName, order);
        return this;

    }

    public Class getModleClass() {
        return this.clazz;
    }

    public String getProjection() {
        return this.projection;
    }

    public void setProjection(String projection) {
        this.projection = projection;
    }

    public Class getClazz() {
        return this.clazz;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public EntityManager getEntityManager() {
        return this.entityManager;
    }

    public void setEntityManager(EntityManager em) {
        this.entityManager = em;
    }

    public Root getFrom() {
        return from;
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    public void setPredicates(List<Predicate> predicates) {
        this.predicates = predicates;
    }

    public CriteriaQuery getCriteriaQuery() {
        return criteriaQuery;
    }

    public CriteriaBuilder getCriteriaBuilder() {
        return criteriaBuilder;
    }

    public void setFetchModes(List<String> fetchField, List<String> fetchMode) {

    }

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

}