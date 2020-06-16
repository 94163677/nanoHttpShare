package air.kanna.nanoHttpSharePC.config.impl;

import java.io.File;
import java.util.Properties;

import org.apache.log4j.Logger;

import air.kanna.nanoHttpShare.util.StringTool;
import air.kanna.nanoHttpSharePC.config.NanoSharePCConfig;
import air.kanna.nanoHttpSharePC.config.NanoSharePCConfigService;

public class SyncConfigServicePropertiesImpl 
        extends BaseFileConfigService<NanoSharePCConfig>
        implements NanoSharePCConfigService{
    private static final Logger logger = Logger.getLogger(SyncConfigServicePropertiesImpl.class);

    public SyncConfigServicePropertiesImpl(File propFile) {
        super(propFile);
    }

    @Override
    protected NanoSharePCConfig prop2Config(Properties prop) {
        if(prop == null || prop.size() <= 0) {
            return null;
        }
        NanoSharePCConfig config = new NanoSharePCConfig();
        
        String temp = prop.getProperty("basePath");
        if(!StringTool.isNullString(temp)) {
            config.setBasePath(temp);
        }
        
        return config;
    }
    
    @Override
    protected Properties config2Prop(NanoSharePCConfig config) {
        if(config == null) {
            return null;
        }
        Properties prop = new Properties();
        
        prop.put("basePath", config.getBasePath());

        return prop;
    }
}
