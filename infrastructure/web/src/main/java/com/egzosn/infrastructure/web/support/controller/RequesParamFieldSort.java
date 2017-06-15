package com.egzosn.infrastructure.web.support.controller;

import com.egzosn.infrastructure.web.support.annotation.MsgCode;
import com.egzosn.infrastructure.web.support.exception.MultipartSameValidateException;

import javax.validation.Constraint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by egan on 2014/10/24.
 *
 */
public class RequesParamFieldSort {

//    private static final Logger LOG = LoggerFactory.getLogger(RequesParamFieldSort.class);

    private static volatile RequesParamFieldSort newInstance;

    public final Map<Class<?>, ParamFieldMapping[]> paramMapping = new HashMap<Class<?>, ParamFieldMapping[]>();
    private final int defaultMsgCode = -1;

    private RequesParamFieldSort() {
    }

    public static RequesParamFieldSort newInstance() {
        if (newInstance == null) {
            newInstance = new RequesParamFieldSort();
        }
        return newInstance;
    }

    public final ParamFieldMapping[] getParamField(Class<?> clazz) {
        return paramMapping.get(clazz);
    }

    private String firstStringLowerCase(String str) {
        return str.replaceFirst(str.substring(0, 1), str.substring(0, 1).toLowerCase());
    }

    public void addParamField(Class<?> clazz) {
        Map<String, String> map = new HashMap<String, String>();
        List<ParamFieldMapping> paramFields = new ArrayList<ParamFieldMapping>();
        Field[] fields = clazz.getDeclaredFields();

        Method[] declaredMethods = clazz.getDeclaredMethods();



        clazz.getDeclaredMethods();
        Annotation[] annotations;
        Constraint constraint;
        Class<? extends Annotation> annotationType;
        MsgCode msgCode;
        for (Field field : fields) {
            annotations = field.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {

                annotationType = annotation.annotationType();
                constraint = annotationType.getAnnotation(Constraint.class);
                if (constraint != null) {
                    String fieldAndvalidateName = field.getName() + annotation.annotationType().getSimpleName();
                    if (map.get(fieldAndvalidateName) != null) {
                        throw new MultipartSameValidateException("有重名的Annotation 校验器，请检查！ ");
                    }
                    map.put(fieldAndvalidateName, fieldAndvalidateName);
                    msgCode = getMsgCode(annotation, field);
                    if (msgCode != null) {
                        paramFields.add(new ParamFieldMapping(fieldAndvalidateName, msgCode.value()));
                    } else {
                        paramFields.add(new ParamFieldMapping(fieldAndvalidateName, defaultMsgCode));
                    }
                }
            }
        }


        if (paramFields.size() > 0) {
            paramMapping.put(clazz, paramFields.toArray(new ParamFieldMapping[0]));
        }
    }

    public MsgCode getMsgCode(Annotation annotation, Field field) {
        MsgCode msgCode = null;
        Class<?> clazz = annotation.annotationType();
        try {
            //      Method[] methods = clazz.getDeclaredMethods();
            Method method = clazz.getDeclaredMethod("code");
            if (method == null) {
                return null;
            }
            if (method.getReturnType() != MsgCode.class) {
                return null;
            }
            msgCode = (MsgCode) method.invoke(annotation);

        } catch (NoSuchMethodException e) {
         //   e.printStackTrace();
        } catch (InvocationTargetException e) {
         //   e.printStackTrace();;
        } catch (IllegalAccessException e) {
         //   e.printStackTrace();;
        }
        if (msgCode == null || msgCode.value() == -1) {
            if (field.getAnnotation(MsgCode.class) != null) {
                msgCode = field.getAnnotation(MsgCode.class);
            }
        }

        return msgCode;

    }

    public class ParamFieldMapping {
        private final String fieldNameAndValidateName;
        private final Integer code;

        public ParamFieldMapping(String fieldNameAndValidateName, Integer code) {
            this.fieldNameAndValidateName = fieldNameAndValidateName;
            this.code = code;
        }

        public String getFieldNameAndValidateName() {
            return fieldNameAndValidateName;
        }

        public Integer getCode() {
            return code;
        }
    }
}
