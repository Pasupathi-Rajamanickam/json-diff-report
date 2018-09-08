package com.nexttimespace.utilities.diffbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class UtilityFunction {
    
    public static Properties appProperties;
    
    static {
        appProperties = new Properties();
        if(new File("app.conf").exists()) {
            try {
                appProperties.load(new FileInputStream(new File("app.conf")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
