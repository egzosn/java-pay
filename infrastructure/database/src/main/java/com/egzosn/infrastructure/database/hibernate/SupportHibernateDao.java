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

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.*;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.Getter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.persistence.*;
import javax.persistence.Query;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import static com.egzosn.infrastructure.database.utils.ReflectionUtils.getSuperClassGenricType;

/**
 * Hibernate基础类,包含对Hibernate的CURD和其他Hibernate操作
 *
 * @param <T>  ROM对象
 * @param <PK> ORM主键ID类型
 * @author egan
 * @date  2016/9/22 10:36
 */
public class SupportHibernateDao<T, PK extends Serializable> {

    @PersistenceContext
    protected EntityManager manager;
    @Resource
    protected JdbcTemplate jdbcTemplate;

    @Resource(name = "transactionManager")
    protected JpaTransactionManager tm;

    protected TransactionStatus transactionStatus;


    protected Class<T> entityClass;
    protected String tableName;
    protected String primaryKey;

    /**
     * 构造方法
     *
     * @param entityClass orm实体类型class
     */
    public SupportHibernateDao(Class<T> entityClass) {
        this.entityClass = entityClass;
    }
    @PostConstruct
    public void init(){
        tableName = entityClass.getAnnotation(Table.class).name();
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.getAnnotation(Id.class) != null) {
                primaryKey =  field.getName();
                break;
            }
        }
    }

    /**
     * 构造方法
     */
    public SupportHibernateDao() {
        this.entityClass = getSuperClassGenricType(getClass());
    }

    protected String getPrimaryKey() {
        return primaryKey;
    }
    protected String getTableName() {
        return  tableName ;
    }


    /**
     * 手动开启事物并获得事物的对应的状态
     * @return
     * <pre>
    User user = //用户
    getTransactionStatus();
    try {
    //此处写持久层逻辑
    save(user)
    commit();
    } catch (Exception e) {
    rollback();
    }
    </pre>
     *
     */
    public TransactionStatus getTransactionStatus(){

        //事务开始
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        def.setTimeout(30);
        //事务状态
        transactionStatus = tm.getTransaction(def);
        return transactionStatus;
    }


    /**
     * 手动开启事物后主动提交事务
     * @see #getTransactionStatus()
     */
    public void commit(){
        tm.commit(transactionStatus);
    }

    /**
     * 手开启事务后进行异常事物回滚
     *  @see #getTransactionStatus()
     */
    public void rollback(){
        if(!transactionStatus.isCompleted()){
            tm.rollback(transactionStatus);
        }
    }

    /**
     *
     *  执行手动提交的事务操作
     * @param callback 执行回调
     * @param <R> 返回对应的领域对象
     * @return
     *
     */
    public <R>R performTransaction(Callback<R> callback){

        getTransactionStatus();
        try{
            R d = callback.performCallback();
            commit();
            return d;
        }catch (Exception e){
            rollback();
            throw  e;
        }
    }

    /**
     * 获取实体管理器
     * @return
     */
    protected EntityManager getManager() {
        return manager;
    }

    public T get(PK id){
        return  manager.find(entityClass, id);
    }

    public T load(PK id){
        return  manager.getReference(entityClass, id);
    }


    /**
     * 按PK列表获取对象列表.
     *
     * @param ids 主键ID集合
     * @return List
     */
    public List<T> get(Collection<PK> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        Restrictions restrictions = Restrictions.forClass(entityClass, manager);
        restrictions.in(getPrimaryKey(), ids);

        return manager.createQuery(restrictions.newCriteriaQuery()).getResultList();
    }


    /**
     * 获取全部对象.
     *
     * @return List
     */
    public List<T> getAll() {
        String ql = "from " + tableName + "  ";
        return manager.createQuery(ql).getResultList();
    }


    /**
     * 按PK列表获取对象列表.
     *
     * @param ids 主键ID数据
     * @return List
     */
    public List<T> get(PK[] ids) {
        if (null == ids){
            return Collections.emptyList();

        }
        Restrictions restrictions = Restrictions.forClass(entityClass, manager);
        restrictions.in(getPrimaryKey(), ids);
        return manager.createQuery(restrictions.newCriteriaQuery()).getResultList();
    }


    /**
     * 新增对象
     *
     * @param entity orm实体
     */
    public void save(T entity) {
        manager.persist(entity);
    }

    /**
     * 将实体合并当前持久化的上下文中 <b>新增或修改对象</b>
     *
     * @param entity orm实体
     */
    public T merge(T entity) {
        return manager.merge(entity);

    }

    /**
     * 保存或更新全部对象
     *
     * @param list orm实体集合
     */
    public void saveAll(List<T> list) {

        if (CollectionUtils.isEmpty(list)) {
            System.out.println("isEmpty");
            return;
        }

        for (int i = 0; i < list.size(); i ++){
            list.set(i, merge(list.get(i)));
        }


    }


    /**
     * 保存或更新全部对象
     *
     * @param ts orm实体集合
     */
    public void saveAll(T[] ts) {
        if (null == ts){
            return;
        }
        for (T t : ts){
            merge(t);
        }
    }


    /**
     * 按PK删除对象.
     *
     * @param id 主键ID
     */
    private Integer delete(PK id) {
        String ql = String.format("delete from %s o where o.%s =:id", tableName, primaryKey);
        Query query = manager.createQuery(ql);
        query.setParameter("id", id);
        Integer result = query.executeUpdate();
        return result;
    }

    public void delete(T entity) {
        manager.remove(entity);
    }

    /**
     * 按PK批量删除对象
     *
     * @param ids 主键ID集合
     */
    public void deleteAll(List<PK> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        for (Iterator<PK> it = ids.iterator(); it.hasNext(); ) {
            delete(it.next());
        }

    }

    /**
     * 按orm实体集合删除对象
     *
     * @param list
     */
    public void deleteAllByEntities(List<T> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        for (Iterator<T> it = list.iterator(); it.hasNext(); ) {
            delete(it.next());
        }
    }

    /**
     * 更新对象
     *
     * @param entity orm实体
     */
    public void update(T entity) {
        manager.merge(entity);
    }

    /**
     * 批量更新对象
     *
     * @param list orm实体集合
     */
    public void updateAll(List<T> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        for (Iterator<T> it = list.iterator(); it.hasNext(); ) {
            update(it.next());
        }
    }

    /**
     *  <code>Query</code>设置参数 parameters
     * @param query 查询
     * @param map 参数值
     * @return
     */
    private Query setProperties(Query query, Map<String, Object> map){
        Set<Parameter<?>> parameters = query.getParameters();
        for(Parameter<?> params: parameters) {
            String namedParam = params.getName();
            Object object = map.get(namedParam);
            if(object != null) {
                query.setParameter(namedParam, object);
            }
        }

        return query;
    }

    /**
     * <code>Query</code>设置参数 bean
     * @param query
     * @param bean 参数对象
     * @return
     */
    private Query setProperties(Query query, Object bean){
        Class clazz = bean.getClass();
        Set<Parameter<?>> parameters = query.getParameters();
        for(Parameter<?> params: parameters) {
            String namedParam = params.getName();
            try {
                Getter pnfe = ReflectHelper.getGetter(clazz, namedParam);
                Object object = pnfe.get(bean);
                if(object != null) {
                    query.setParameter(namedParam, object);
                }
            } catch (PropertyNotFoundException var9) {
                ;
            }
        }

        return query;
    }
    /**
     * 设置参数值到query的hql中
     *
     * @param query  Query
     * @param values 参数值可变数组
     */
    protected void setQueryValues(Query query, Object... values) {
        if (ArrayUtils.isEmpty(values)) {
            return;
        }
        for (int len = values.length, i = 0; i < len; i ++) {
            query.setParameter(i, values[i]);
        }
    }


    /**
     * 根据hql,sql或 NamedQuery 创建Query对象
     * @param queryType 查询类型{@link QueryType}
     * @param queryOrNamedQuery sql或ql 或者Hibernate的NamedQuery
     * @param values            数量可变的参数,按顺序绑定.
     * @return {@link javax.persistence.Query}
     */
    protected Query createQuery(QueryType queryType, String queryOrNamedQuery, Object... values) {
        Assert.hasText(queryOrNamedQuery, "queryOrNamedQuery不能为空");
        Query query =queryType.create(manager, queryOrNamedQuery);
        setQueryValues(query, values);
        return query;
    }

    /**
     * 根据hql,sql或 NamedQuery 创建Query对象
     * @param resultClass 需要返回的对象--><b>结果集</b>
     * @param queryOrNamedQuery sql或ql 或者Hibernate的NamedQuery
     * @param values            数量可变的参数,按顺序绑定.
     * @return {@link javax.persistence.Query}
     */
    protected Query createNamedQuery(Class resultClass, String queryOrNamedQuery, Object... values) {
        Assert.hasText(queryOrNamedQuery, "queryOrNamedQuery不能为空");
        Query query = createQuery(new QueryType.Named().setResultClass(resultClass, resultClass == entityClass), queryOrNamedQuery, values);
        return query;
    }

    /**
     * 根据hql或者sql创建Query对象
     * @param queryOrNamedQuery sql或ql 或者Hibernate的NamedQuery
     * @param values            数量可变的参数,按顺序绑定.
     * @return {@link javax.persistence.Query}
     */
    protected Query createNamedQuery(String queryOrNamedQuery, Object... values) {
        return createNamedQuery(null, queryOrNamedQuery, values);
    }
    /**
     * 根据查询hql或者sql与参数列表创建Query对象
     *
     * @param resultClass 需要返回的对象--><b>结果集</b>
     * @param queryOrNamedQuery sql或ql  或者Hibernate的NamedQuery
     * @param values            命名参数,按名称绑定.
     * @return {@link javax.persistence.Query}
     */
    protected Query createNamedQuery(Class resultClass, String queryOrNamedQuery, Map<String, Object> values) {
        Query query = createNamedQuery(resultClass, queryOrNamedQuery);
        if (values == null || values.isEmpty()) {
            return query;
        }
        return  setProperties(query, values);
    }

    /**
     * 根据查询hql参数列表创建Query对象
     *
     * @param queryOrNamedQuery sql或ql  或者Hibernate的NamedQuery
     * @param values            命名参数,按名称绑定.
     * @return {@link javax.persistence.Query}
     */
    protected Query createNamedQuery(String queryOrNamedQuery, Map<String, Object> values) {
       return createNamedQuery(null, queryOrNamedQuery, values);
    }


    /**
     * 根据hql创建Query对象
     * @param hql
     * @param values            数量可变的参数,按顺序绑定.
     * @return {@link javax.persistence.Query}
     */
    protected Query createHQLQuery(String hql, Object... values) {
        Assert.hasText(hql, "hql不能为空");
        Query query = createQuery(new QueryType.HQL(), hql, values);
        return query;
    }


    /**
     * 根据查询hql与参数列表创建Query对象
     *
     * @param hql hql
     * @param values            命名参数,按名称绑定.
     * @return {@link javax.persistence.Query}
     */
    protected Query createHQLQuery(String hql, Map<String, Object> values) {
        Query query = createHQLQuery( hql);
        if (values == null || values.isEmpty()) {
            return query;
        }
        return  setProperties(query, values);
    }

    /**
     * 根据sql创建Query对象
     * @param resultClass 需要返回的对象--><b>结果集</b>
     * @param sql sql或ql 或者Hibernate的NamedQuery
     * @param values            数量可变的参数,按顺序绑定.
     * @return {@link javax.persistence.Query}
     */
    protected Query createSQLQuery(Class resultClass, String sql, Object... values) {
        Assert.hasText(sql, "sql不能为空");
        Query query = createQuery(new QueryType.SQL().setResultClass(resultClass, resultClass == entityClass), sql, values);
        return query;
    }

    /**
     * 根据sql创建Query对象
     * @param queryOrNamedQuery sql或ql 或者Hibernate的NamedQuery
     * @param values            数量可变的参数,按顺序绑定.
     * @return {@link javax.persistence.Query}
     */
    protected Query createSQLQuery(String queryOrNamedQuery, Object... values) {
        return createSQLQuery(null, queryOrNamedQuery, values);
    }
    /**
     * 根据查询sql与参数列表创建Query对象
     *
     * @param resultClass 需要返回的对象--><b>结果集</b>
     * @param queryOrNamedQuery sql或ql  或者Hibernate的NamedQuery
     * @param values            命名参数,按名称绑定.
     * @return {@link javax.persistence.Query}
     */
    protected Query createSQLQuery(Class resultClass, String queryOrNamedQuery, Map<String, Object> values) {
        Query query = createSQLQuery(resultClass, queryOrNamedQuery);
        if (values == null || values.isEmpty()) {
            return query;
        }
        return  setProperties(query, values);
    }

    /**
     * 根据查询sql与参数列表创建Query对象
     *
     * @param queryOrNamedQuery sql或ql  或者Hibernate的NamedQuery
     * @param values            命名参数,按名称绑定.
     * @return {@link javax.persistence.Query}
     */
    protected Query createSQLQuery(String queryOrNamedQuery, Map<String, Object> values) {
       return createSQLQuery(null, queryOrNamedQuery, values);
    }

    /**
     * 执行带参数的hql
     * @param hql
     * @param params 参数
     * @return 执行操作后影响的条数
     */
    public int execute(String hql, Map<String, Object> params) {
        return createHQLQuery(hql, params).executeUpdate();
    }
    /**
     * 执行带参数的hql
     * @param hql
     * @param params 参数
     * @return 执行操作后影响的条数
     */
    public int execute(String hql,  Object ... params) {
        return createHQLQuery(hql, params).executeUpdate();
    }

    /**
     * 执行带参数的sql
     * @param sql
     * @param params 参数
     * @return  执行操作后影响的条数
     */
    public int executeSql(String sql, Map<String, Object> params) {
        return createSQLQuery(sql, params).executeUpdate();
    }

    /**
     * 执行带参数的sql
     * @param sql
     * @param params 参数
     * @return  执行操作后影响的条数
     */
    public int executeSql(String sql, Object... params) {
        return createSQLQuery(sql, params).executeUpdate();
    }

    /**
     * 创建查询记录集
     */
    class CreateQuery<X>{
        private Map<String, Object> attr;
        private Object[] paras;
        private Class<X> transferClass; //主要用于sql查询结果集映射的对象
        private boolean isHQL = false; //是否为hql 默认否

        public Query createSQLQuery(String ql){
             return  (null == attr || attr.isEmpty()) ?  SupportHibernateDao.this.createSQLQuery(transferClass, ql, paras) :  SupportHibernateDao.this.createSQLQuery(transferClass, ql, attr);
        }

        public Query createHQLQuery(String ql){
            return (null == attr || attr.isEmpty()) ? SupportHibernateDao.this.createHQLQuery(ql, paras): SupportHibernateDao.this.createHQLQuery(ql, attr);
        }

        public Query createQuery(String ql){
            return  isHQL ? createHQLQuery(ql) : createSQLQuery(ql);
        }

        public CreateQuery(Map<String, Object> attr) {
            this.attr = attr;
        }

        public CreateQuery(Object... paras) {
            this.paras = paras;
        }

        public CreateQuery(Class transferClass, Object... paras) {
            this.transferClass = transferClass;
            this.paras = paras;
        }

        public CreateQuery( Class transferClass, Map<String, Object> attr) {
            this.transferClass = transferClass;
            this.attr = attr;
        }

        public CreateQuery(Map<String, Object> attr, boolean isHQL) {
            this.attr = attr;
            this.isHQL = isHQL;
        }

        public CreateQuery(Object[] paras, boolean isHQL) {
            this.paras = paras;
            this.isHQL = isHQL;
        }
    }

    /**
     * 查询记录数
     * @param createQuery
     * @param ql
     * @return
     */
    public long count(CreateQuery createQuery, String ql) {
        Query queryCount = createQuery.createQuery(ql);
        Object object = queryCount.getSingleResult();
        if (!(object instanceof Number)) {
            return 0;
        }
        return ((Number) object).longValue();
    }

    /**
     * 查询记录数
     * @param hql
     * @param params
     * @return
     */
    public long count(String hql,  Map<String, Object> params){
        return count(new CreateQuery(params, true), hql);
    }

    /**
     * 查询记录数
     * @param hql
     * @param params
     * @return
     */
    public long count(String hql, Object... params){
        return count(new CreateQuery(params, true), hql);
    }
    /**
     * 查询记录数
     * @param sql
     * @param params
     * @return
     */
    public long countSQL(String sql,  Map<String, Object> params){
        return count(new CreateQuery(params), sql);
    }

    /**
     * 查询记录数
     * @param sql
     * @param params
     * @return
     */
    public long countSQL(String sql, Object... params){
        return count(new CreateQuery(params), sql);
    }


    /**
     * 查找出单一对应的结果集
     * @param queryCallback 创建查询记录对象
     * @param ql
     * @param <X> 返回的对象类型
     * @return List
     */
    protected <X> X queryUniqueByQl(CreateQuery queryCallback, final String ql) {
        // 取得查询接口
        Query query = queryCallback.createQuery(ql);
        return (X) query.getSingleResult();
    }

    /**
     * 查找出单一对应的结果集
     * @param sql
     * @param transferClass 对象映射
     * @param params 参数
     * @param <X> 返回的对象类型
     * @return
     */
    public <X> X queryUniqueBySql(final String sql, Class<X> transferClass, Map<String, Object> params) {
        return queryUniqueByQl(new CreateQuery(transferClass, params), sql);
    }



    /**
     * 查找出单一对应的结果集
     * @param  hql
     * @param params 参数
     * @return 返回的对象类型
     */
    public T queryUniqueByHQL(final String hql, Map<String, Object> params) {
        return queryUniqueByQl(new CreateQuery(params, true), hql);
    }
    /**
     * 分页查找出对应的集合
     * @param ql
     * @param <X> 返回的对象类型
     * @return List
     */
    protected <X> List<X> listByQl(CreateQuery<X> queryCallback, final String ql) {
        // 取得查询接口
        return queryCallback.createQuery(ql).getResultList();
    }
    /**
     * 分页查找出对应的集合
     * @param sql
     * @param transferClass 映射的对应类
     * @param params 参数
     * @param <X> 返回的对象类型
     * @return List
     */
    protected <X> List<X> listBySql(final String sql, Class<X> transferClass, Map<String, Object> params) {
        return listByQl(new CreateQuery(transferClass, params), sql);
    }


    /**
     * 分页查找出对应的集合
     * @param sql
     * @param transferClass 映射的对应类
     * @param params 参数
     * @param <X> 返回的对象类型
     * @return List
     */
    protected <X> List<X> listBySql(final String sql, Class<X> transferClass, Object... params) {
        return listByQl(new CreateQuery(transferClass, params), sql);
    }

    /**
     * 分页查找出对应的集合
     * @param hql
     * @param params 参数
     * @return List 返回的对象类型
     */
    protected  List<T> listByHQL(final String hql, Map<String, Object> params) {

        return listByQl(new CreateQuery(params, true), hql);
    }


    /**
     * 分页查找出对应的集合
     * @param hql
     * @param params 参数
     * @param <X> 返回的对象类型
     * @return List
     */
    protected <X> List<X> listByHQL(final String hql, Object... params) {
        return listByQl(new CreateQuery(params, true), hql);
    }

    /**
     * 分页查找出对应的集合
     * @param ql
     * @param page 第几页
     * @param rows 几行
     * @param <X> 返回的对象类型
     * @return List
     */
    protected <X> List<X> listMoreByQl(CreateQuery<X> queryCallback, final String ql, int page, int rows) {
        int startIndex = Page.getStartOfPage(page, rows);
        List<X> list = null;
        // 取得查询接口
        Query query = queryCallback.createQuery(ql);
        // 分页
        query.setFirstResult(startIndex);
        query.setMaxResults(rows);
        return query.getResultList();
    }

    /**
     * 分页查找出对应的集合
     * @param sql
     * @param transferClass 映射的对应类
     * @param params 参数
     * @param page 第几页
     * @param rows 几行
     * @param <X> 返回的对象类型
     * @return List
     */
    protected <X> List<X> listMoreBySql(final String sql, Class<X> transferClass, Map<String, Object> params, int page, int rows) {

        return listMoreByQl(new CreateQuery(transferClass, params), sql, page, rows);
    }


    /**
     * 分页查找出对应的集合
     * @param sql
     * @param transferClass 映射的对应类
     * @param params 参数
     * @param page 第几页
     * @param rows 几行
     * @param <X> 返回的对象类型
     * @return List
     */
    protected <X> List<X> listMoreBySql(final String sql, Class<X> transferClass, int page, int rows, Object... params) {
        return listMoreByQl(new CreateQuery(transferClass, params), sql, page, rows);
    }


    /**
     * 分页查找出对应的集合
     * @param hql
     * @param params 参数
     * @param page 第几页
     * @param rows 几行
     * @return List
     */
    protected  List<T> listMoreByHQL(final String hql, Map<String, Object> params, int page, int rows) {
        return listMoreByQl(new CreateQuery(params, true), hql, page, rows);
    }

    /**
     * 分页查找出对应的集合
     * @param hql
     * @param params 参数
     * @param page 第几页
     * @param rows 几行
     * @return List 返回的对象类型
     */
    protected  List<T>  listMoreByHQL(final String hql, int page, int rows, Object... params) {
        return listMoreByQl(new CreateQuery(params, true), hql, page, rows);
    }



    /**
     * 分页查询
     * @param sql
     * @param transferClass 映射的对应类
     * @param params 参数
     * @param page 第几页
     * @param rows 几行
     * @param <X> 返回的对象类型
     * @return
     */
    protected <X> Page<X> pageQuerySQL(final String sql, Class<X> transferClass, Map<String, Object> params, int page, int rows) {
        long lo = countSQL(sql, params);
        if (0 == lo){ return new Page<>();}

        // TODO 2016/9/21 14:25 author: egan 获取对应结果集
        List<X> list = listMoreBySql(sql, transferClass, params, page, rows);
        return new Page<X>(page, list.size(), lo, list);
    }


    /**
     * 分页查询
     * @param sql
     * @param transferClass 映射的对应类
     * @param params 参数
     * @param page 第几页
     * @param rows 几行
     * @param <X> 返回的对象类型
     * @return
     */
    protected <X> Page<X> pageQuerySQL(final String sql, Class<X> transferClass, int page, int rows,  Object ... params) {
        long lo = countSQL(sql, params);
        if (0 == lo){ return new Page<>();}

        // TODO 2016/9/21 14:25 author: egan 获取对应结果集
        List<X> list = listMoreBySql(sql, transferClass, page, rows, params);
        return new Page<X>(page, list.size(), lo, list);
    }


    /**
     * 分页查询
     * @param sql
     * @param transferClass 映射的对应类
     * @param params 参数
     * @param <X> 返回的对象类型
     * @return
     */
    protected <X> Page<X> pageQuerySQL(final String sql, Class<X> transferClass, Map<String, Object> params) {
        long lo = countSQL(sql, params);
        if (0 == lo){ return new Page<>();}

        // TODO 2016/9/21 14:25 author: egan 获取对应结果集
        List<X> list = listBySql(sql, transferClass, params);
        return new Page<X>(1, list.size(), lo, list);
    }


    /**
     * 分页查询
     * @param sql
     * @param transferClass 映射的对应类
     * @param params 参数
     * @param <X> 返回的对象类型
     * @return
     */
    protected <X> Page<X> pageQuerySQL(final String sql, Class<X> transferClass, Object ... params) {
        long lo = countSQL(sql, params);
        if (0 == lo){ return new Page<>();}

        // TODO 2016/9/21 14:25 author: egan 获取对应结果集
        List<X> list = listBySql(sql, transferClass, params);
        return new Page<X>(1, list.size(), lo, list);
    }


   /**
     * 分页查询
     * @param hql
     * @param params 参数
     * @param page 第几页
     * @param rows 几行
     * @return 返回的对象类型
     */
    protected  Page<T> pageQueryHQL(final String hql, Map<String, Object> params, int page, int rows) {
        long lo = count(hql, params);
        if (0 == lo){ return new Page<>();}

        // TODO 2016/9/21 14:25 author: egan 获取对应结果集
        List<T> list = listMoreByHQL(hql, params, page, rows);
        return new Page<T>(page, list.size(), lo, list);
    }


    /**
     * 分页查询
     * @param hql
     * @param params 参数
     * @param page 第几页
     * @param rows 几行
     * @return 返回的对象类型
     */
    protected Page<T> pageQueryHQL(final String hql, int page, int rows,  Object ... params) {
        long lo = count(hql, params);
        if (0 == lo){ return new Page<>();}

        // TODO 2016/9/21 14:25 author: egan 获取对应结果集
        List<T> list = listMoreByHQL(hql, page, rows, params);
        return new Page<T>(page, list.size(), lo, list);
    }

   /**
     * 分页查询
     * @param hql
     * @param params 参数
     * @return 返回的对象类型
     */
    protected  Page<T> pageQueryHQL(final String hql, Map<String, Object> params) {
        long lo = count(hql, params);
        if (0 == lo){ return new Page<>();}

        // TODO 2016/9/21 14:25 author: egan 获取对应结果集
        List<T> list = listByHQL(hql, params);
        return new Page<T>(1, list.size(), lo, list);
    }


    /**
     * 分页查询
     * @param hql
     * @param params 参数
     * @return 返回的对象类型
     */
    protected Page<T> pageQueryHQL(final String hql,  Object ... params) {
        long lo = count(hql, params);
        if (0 == lo){ return new Page<>();}

        // TODO 2016/9/21 14:25 author: egan 获取对应结果集
        List<T> list = listByHQL(hql, params);
        return new Page<T>(1, list.size(), lo, list);
    }


    protected StringBuilder getUpdateField(boolean isHQL, Map<String, Object> updateField, String alias, List<Object> paras){
        StringBuilder sql = new StringBuilder();
        sql.append("update `").append(isHQL ? entityClass.getName() + " " + alias: getTableName()).append("` set ");
        for (String colName : updateField.keySet()) {
            if (paras.size() > 0){
                sql.append(", ");
            }
            sql.append("`").append(colName).append("` = ? ");
            paras.add(updateField.get(colName));
        }
        return sql;
    }

    /**
     *  更新对象
     * @param isHQL 是否为HQL
     * @param updateField 需要更新的字段
     * @param conditionValue 条件部分
     * @return
     */
    public int update(boolean isHQL, Map<String, Object> updateField, Map<String,String> conditionValue) {
         List<Object> paras = new ArrayList<>();
        StringBuilder sql = getUpdateField(isHQL, updateField, "", paras);

        if (null == conditionValue || conditionValue.isEmpty()) {
            return 0;
        }
        sql.append(" where `");
        int i = 0;
        for (String colName : conditionValue.keySet()) {
            if (i > 0){
                sql.append(", ");
            }
            sql.append("`").append(colName).append("` = ? ");
            paras.add(conditionValue.get(colName));
            i++;
        }
        return executeSql(sql.toString(), paras.toArray());
    }



}
