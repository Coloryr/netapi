package netapi.obj;

public class ConfigObj {
    public String level;
    public String encodeType;

    public static ConfigObj make() {
        ConfigObj obj = new ConfigObj();
        obj.level = "exhigh";
        obj.encodeType = "aac";
        return obj;
    }
}
