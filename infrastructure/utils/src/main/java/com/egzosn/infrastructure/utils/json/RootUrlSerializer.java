package com.egzosn.infrastructure.utils.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by linjie on 15-1-21.
 */
public class RootUrlSerializer extends JsonSerializer<String> {

    private static String root_file_url = "";

    static {
        try {
            Configuration config = new PropertiesConfiguration("web-config.properties");
            Iterator<String> keys = config.getKeys();
            String key = "";
            System.out.println("------------web-config.properties-----------");

            while(keys.hasNext()) {
                key = keys.next();
                if (key.equals("root_file_url")) {
                    root_file_url = config.getString(key);
                }
            }
            if (root_file_url == null || "".equals(root_file_url)) {
                System.out.println("没有配置对应的：root_image_url 属性");
            }
        } catch (ConfigurationException e) {
            System.out.println("需要记录下来：");
            System.out.println("没有找到web-config配置文件");
            System.out.println("没有对应的：root_image_url 属性");
        }
    }

    @Override
    public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        if (value != null) {
            jgen.writeString(root_file_url+value);
        }
    }
}
