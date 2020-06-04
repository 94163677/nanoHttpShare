package air.kanna.nanoHttpSharePC.logger.impl;

import air.kanna.nanoHttpShare.logger.Logger;
import air.kanna.nanoHttpShare.logger.LoggerFactory;

public class Log4jLoggerFactory implements LoggerFactory {

	@Override
	public Logger getLogger(Class cls) {
		Log4jLogger logger = new Log4jLogger(org.apache.log4j.Logger.getLogger(cls));
		return logger; 
	}

	@Override
	public Logger getLogger(String name) {
		Log4jLogger logger = new Log4jLogger(org.apache.log4j.Logger.getLogger(name));
		return logger; 
	}

}
