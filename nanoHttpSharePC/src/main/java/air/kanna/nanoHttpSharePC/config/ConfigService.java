package air.kanna.nanoHttpSharePC.config;

public interface ConfigService<T> {
    T getConfig();
    boolean saveConfig(T config);
}
