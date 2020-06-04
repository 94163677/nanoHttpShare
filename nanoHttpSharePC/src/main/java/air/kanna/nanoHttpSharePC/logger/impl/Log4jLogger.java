package air.kanna.nanoHttpSharePC.logger.impl;

import air.kanna.nanoHttpShare.logger.Logger;

public class Log4jLogger implements Logger {
	private org.apache.log4j.Logger logger;
	
	Log4jLogger(org.apache.log4j.Logger logger){
		if(logger == null){
			throw new NullPointerException("org.apache.log4j.Logger is null");
		}
		this.logger = logger;
	}
	@Override
	public void debug(String msg) {
		logger.debug(msg);
	}

	@Override
	public void info(String msg) {
		logger.info(msg);
	}

	@Override
	public void warn(String msg) {
		logger.warn(msg);
	}
	
	@Override
	public void warn(String msg, Throwable tro) {
		logger.warn(msg, tro);
	}

	@Override
	public void error(String msg) {
		logger.error(msg);
	}

	@Override
	public void error(String msg, Throwable tro) {
		logger.error(msg, tro);
	}

}
