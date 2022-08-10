package github.jlyyxd.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Slf4j
public class PropertiesFileUtil {

    private PropertiesFileUtil(){}

    public static Properties readPropertiesFile(String fileName){
        URL url = Thread.currentThread().getContextClassLoader().getResource(fileName);
        String path = "";
        if (url != null) {
            path = url.getPath();
        }else {
            log.warn("Unable to find the file "+fileName);
            return null;
        }
        Properties properties = null;
        try(InputStreamReader inputStreamReader = new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8)){
            properties = new Properties();
            properties.load(inputStreamReader);
        } catch (IOException e){
            log.error("occur exception when read properties file [{}]", fileName);
        }
        return properties;
    }
}
