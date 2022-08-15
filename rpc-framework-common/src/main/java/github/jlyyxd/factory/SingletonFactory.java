package github.jlyyxd.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获得单例对象的工厂类
 */
public class SingletonFactory {
    private static final Map<Class<?>, Object> OBJECT_MAP = new ConcurrentHashMap<>();

    private SingletonFactory() {
    }

    public static <T> T getInstance(Class<T> c) {
        if (c == null) {
            throw new IllegalArgumentException("The argument must not be nll");
        }
        if (OBJECT_MAP.containsKey(c)) {
            return c.cast(OBJECT_MAP.get(c));
        } else {
            // 缓存中不存在则使用反射创建，放入缓存再返回
            // 这里使用了computeIfAbsent方法保证了线程安全（只有一个线程可以执行创建对象的逻辑）
            return c.cast(OBJECT_MAP.computeIfAbsent(c, clazz -> {
                try {
                    return c.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }));
        }
    }
}
