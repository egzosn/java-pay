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

import org.hibernate.SQLQuery;
import org.hibernate.jpa.internal.QueryImpl;
import org.hibernate.transform.Transformers;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Map;

/**
 * 查询类型，获取Query {@link javax.persistence.Query}
 *  @time 2016/9/21 9:30
 *  @author: egan
 *
 */
public abstract class QueryType {

    protected Class resultClass;
    protected boolean isEntity = false;

    /**
     *  SQl
     */
    public static class SQL extends QueryType{
        @Override
        public Query create(EntityManager manager, String sql) {
            Query query = null;
            if ( null == resultClass || resultClass.isAssignableFrom(Map.class)){
                query = manager.createNativeQuery(sql);
                QueryImpl queryIml = (QueryImpl) query;
                queryIml.getHibernateQuery().setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
                return query;
            }
            if (isEntity){
                return manager.createNativeQuery(sql, resultClass);
            }
            query = manager.createNativeQuery(sql);
            QueryImpl queryIml = (QueryImpl) query;
            // TODO 2016/9/21 10:38 author: egan 查询结果级对象映射
            SQLTools.setQueryTransform(resultClass, (SQLQuery) queryIml.getHibernateQuery());
            return query;
        }
    }
  /**
     *  HQL
     */
    public static class HQL extends QueryType{
        @Override
        public Query create(EntityManager manager, String hql) {
            return manager.createQuery(hql);
        }
    }

    /**
     * Hibernate的NamedQuery
     */
    public static class Named extends QueryType{
        @Override
        public Query create(EntityManager manager, String named) {
            if (isEntity){
                return manager.createNamedQuery(named, resultClass);
            }
            Query query = manager.createNamedQuery(named);
            QueryImpl queryIml = (QueryImpl) query;
            org.hibernate.Query hibernateQuery = queryIml.getHibernateQuery();
            if (isNativeSqlQuery(hibernateQuery)){
                if ( null == resultClass || resultClass.isAssignableFrom(Map.class)){
                    hibernateQuery.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
                }else {
                    SQLTools.setQueryTransform(resultClass, (SQLQuery) hibernateQuery);
                }
            }
            return query;
        }
    }


    /**
     * 创建Query
     * @param manager 实体管理器 类似hibernate Session
     * @param ql 传入的sql，hql或者named
     * @return
     */
    public abstract Query create(EntityManager manager, String ql);

    public QueryType setResultClass(Class resultClass, boolean isEntity){
        this.resultClass = resultClass;
        this.isEntity = isEntity;
        return this;
    }
    public static boolean isNativeSqlQuery(org.hibernate.Query query) {
        return SQLQuery.class.isInstance(query);
    }

    public static boolean isSelectQuery(org.hibernate.Query query) {
        if(isNativeSqlQuery(query)) {
            throw new IllegalStateException("Cannot tell if native SQL query is SELECT query");
        } else {
            return (org.hibernate.internal.QueryImpl.class.cast(query)).isSelect();
        }
    }

}
