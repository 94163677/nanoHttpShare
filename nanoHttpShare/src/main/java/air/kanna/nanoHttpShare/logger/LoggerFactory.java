package air.kanna.nanoHttpShare.logger;

public interface LoggerFactory {
	public Logger getLogger(Class cls);
	public Logger getLogger(String name);
}
