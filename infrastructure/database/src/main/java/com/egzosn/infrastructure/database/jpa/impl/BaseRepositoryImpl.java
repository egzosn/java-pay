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


package com.egzosn.infrastructure.database.jpa.impl;

import com.egzosn.infrastructure.database.jpa.BaseRepository;
import org.hibernate.Session;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.metamodel.SingularAttribute;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Created by egan on 2016/9/12.
 */

public class BaseRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements BaseRepository<T, ID> {


    private final EntityManager entityManager;
    private final JpaEntityInformation entityInformation;
    /**
     * 覆盖父类的构造 截取entityManager
     * @param entityInformation
     * @param entityManager
     * @see SimpleJpaRepository
     * @see EntityManager
     */
    public BaseRepositoryImpl(JpaEntityInformation entityInformation,
                              EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;

    }


    /**
     * 获取session
     * @return session
     * @see Session
     */
    protected Session getSession() {
        return (Session) entityManager.getDelegate();
    }



    /**
     * 获取主键名
     * @return 获取对应的主键名
     *
     * @see  org.springframework.data.jpa.repository.support.JpaEntityInformation#getIdAttribute
     *
     */
    private String getPrimaryKey() {
        // TODO 2016/9/12 16:50 author: egan  #getIdAttribute()获取对应的ID属性
        return entityInformation.getIdAttribute().getName();
    }

}

