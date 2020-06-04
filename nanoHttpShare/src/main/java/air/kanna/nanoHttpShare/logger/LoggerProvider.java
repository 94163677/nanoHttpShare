package air.kanna.nanoHttpShare.logger;

public class LoggerProvider {
	private static LoggerFactory factory = null;
	
	public static void resetLoggerFactory(LoggerFactory fact){
		if(fact == null){
			throw new NullPointerException("Input LoggerFactory is null");
		}
		factory = fact;
	}
	
	private static void checkLoggerFactory(){
		if(factory == null){
			throw new NullPointerException("LoggerFactory is null");
		}
	}
	
	public static Logger getLogger(Class cls){
		checkLoggerFactory();
		return factory.getLogger(cls);
	}
	
	public static Logger getLogger(String name){
		checkLoggerFactory();
		return factory.getLogger(name);
	}
}
