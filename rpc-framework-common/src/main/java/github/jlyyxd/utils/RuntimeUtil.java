package github.jlyyxd.utils;

public class RuntimeUtil {
    /**
     * 获取cpu核心数
     */
    public static int cpuNumber() {
        return Runtime.getRuntime().availableProcessors();
    }
}
