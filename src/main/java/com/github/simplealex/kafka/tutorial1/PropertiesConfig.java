package com.github.simplealex.kafka.tutorial1;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesConfig {

    public Properties readPropertiesFile(String fileName) throws IOException {
        InputStream fis = null;
        Properties prop = null;
        try {
            fis = getClass().getClassLoader().getResourceAsStream(fileName);
            prop = new Properties();
            prop.load(fis);
        } catch(FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        } finally {

            if  (fis != null) {
                fis.close();
            }
        }
        return prop;
    }

}
