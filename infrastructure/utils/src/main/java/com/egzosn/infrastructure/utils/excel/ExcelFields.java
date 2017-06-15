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

/**
 * Created by ZaoSheng on 2016/1/12.
 */
public class ExcelFields {
    private String fieldName;
    private String field;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

	public ExcelFields() {
		super();
	}

	public ExcelFields(String fieldName, String field) {
		super();
		this.fieldName = fieldName;
		this.field = field;
	}
    
}