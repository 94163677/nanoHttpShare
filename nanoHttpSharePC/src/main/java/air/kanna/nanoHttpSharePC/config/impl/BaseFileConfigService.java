package air.kanna.nanoHttpSharePC.config.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import org.apache.log4j.Logger;

import air.kanna.nanoHttpSharePC.config.ConfigService;


public abstract class BaseFileConfigService<T> implements ConfigService<T> {
    private static final Logger logger = Logger.getLogger(SyncConfigServicePropertiesImpl.class);
    
    protected static final String CHARSET = "UTF-8";
    
    protected File propertiesConfigFile;
    
    public BaseFileConfigService(File propFile) {
        if(propFile == null) {
            throw new NullPointerException("PropertiesConfigFile is null");
        }
        if(propFile.isDirectory()) {
            throw new IllegalArgumentException("PropertiesConfigFile is not a file");
        }
        propertiesConfigFile = propFile;
    }
    
    protected abstract T prop2Config(Properties prop);
    protected abstract Properties config2Prop(T config);
    
    @Override
    public T getConfig() {
        if(propertiesConfigFile == null 
                || propertiesConfigFile.isDirectory() 
                || !propertiesConfigFile.exists()) {
            logger.warn("PropertiesConfigFile is null, is a directory or is not exists");
            return null;
        }
        Properties prop = new Properties();
        try {
            Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(propertiesConfigFile), CHARSET), 10240);
            prop.load(reader);
            reader.close();
        }catch(Exception e) {
            logger.error("Cannot load config from file: " + propertiesConfigFile.getAbsolutePath(), e);
        }
        return prop2Config(prop);
    }
    
    @Override
    public boolean saveConfig(T config) {
        if(config == null) {
            logger.warn("Config is null");
            return false;
        }
        Properties prop = config2Prop(config);
        if(prop == null || prop.size() <= 0) {
            logger.warn("Cannot get properties from config");
            return false;
        }
        try {
            if(propertiesConfigFile.exists()) {
                propertiesConfigFile.delete();
            }
            propertiesConfigFile.createNewFile();
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(propertiesConfigFile), CHARSET), 10240);
            prop.store(writer, "");
            writer.flush();
            writer.close();
        }catch(Exception e) {
            logger.error("Cannot save config", e);
            return false;
        }
        return true;
    }
}
