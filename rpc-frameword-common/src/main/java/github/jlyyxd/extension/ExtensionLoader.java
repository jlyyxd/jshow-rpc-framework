package github.jlyyxd.extension;

import github.jlyyxd.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ExtensionLoader<T> {

    // SERVICE搜索路径
    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";
    // 一级map：接口Class对象 -> ExtensionLoader实例
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();
    // 存放所有服务实例：具体类Class对象 -> 创建出来的实例
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    // 接口Class对象，表示当前ExtensionLoader管理的是什么接口下的服务
    private final Class<?> type;
    // 存放的映射：服务名称 -> 服务实例
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();
    // value存放的映射：服务名称 -> 服务类对象
    private final Holder<Map<String, Class<?>>> cachedClassesHolder = new Holder<>();

    // 构造器私有化,使用类方法getExtensionLoader进行获取
    private ExtensionLoader(Class<?> type){
        this.type=type;
    }

    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type){
        // 入参要求：需要是被@SPI修饰的接口
        if(type==null){
            throw new IllegalArgumentException("Extension type must not be null!");
        }
        if (!type.isInterface()){
            throw new IllegalArgumentException("Extension type must be an interface!");
        }
        if(type.getAnnotation(SPI.class)==null){
            throw new IllegalArgumentException("Extension type must be annotated by @SPI");
        }

        //检查该接口对应的ExtensionLoader是否已经存在
        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>)EXTENSION_LOADERS.get(type);
        if (extensionLoader==null){
            // 不存在则创建一个ExtensionLoader放进去，putIfAbsent保证线程安全
            EXTENSION_LOADERS.putIfAbsent(type,new ExtensionLoader<>(type));
            extensionLoader= (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }
        return extensionLoader;
    }
    
    // 根据服务名获取服务对象
    public T getExtension(String name){
        if(StringUtils.isBlank(name)){
            throw new IllegalArgumentException("Extension name should not be null or empty");
        }
        
        // 首先从cache中检查服务是否存在
        Holder<Object> holder = cachedInstances.get(name);
        if (holder==null){
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder=cachedInstances.get(name);
        }

        Object serviceInstance = holder.getValue();
        if(serviceInstance==null){
            synchronized (holder){
                serviceInstance=holder.getValue();
                if(serviceInstance==null){
                    serviceInstance=createExtension(name);
                    holder.setValue(serviceInstance);
                }
            }
        }
        return (T) serviceInstance;
    }

    private T createExtension(String name) {
        Class<?> clazz = getCacheClass().get(name);
        if(clazz==null){
            throw new RuntimeException("No such extension of name "+name);
        }

        // 从静态服务缓存EXTENSION_INSTANCES中寻找服务,没有则创建并放入EXTENSION_INSTANCES中
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if(instance==null){
            try {
                EXTENSION_INSTANCES.putIfAbsent(clazz,clazz.getDeclaredConstructor().newInstance());
                instance=(T) EXTENSION_INSTANCES.get(clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    private Map<String, Class<?>> getCacheClass() {
        Map<String, Class<?>> classMap = cachedClassesHolder.getValue();

        if(classMap==null){
            synchronized (cachedClassesHolder){
                classMap= cachedClassesHolder.getValue();
                if(classMap==null){
                    classMap=new HashMap<>();
                    loadDirectory(classMap);
                    cachedClassesHolder.setValue(classMap);
                }
            }
        }
        return classMap;
    }

    private void loadDirectory(Map<String, Class<?>> extensionClassMap) {
        // 文件名为 SERVICE_DIRECTORY 目录下的当前ExtensionLoader负责的 接口类型全限定名
        String fileName=ExtensionLoader.SERVICE_DIRECTORY+type.getName();

        try{
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            Enumeration<URL> urls = classLoader.getResources(fileName);
            if (urls != null) {
                while (urls.hasMoreElements()){
                    URL url = urls.nextElement();
                    loadResource(extensionClassMap,classLoader,url);
                }
            }
        }catch (IOException e){
            log.error(e.getMessage());
        }


    }

    private void loadResource(Map<String, Class<?>> extensionClassMap, ClassLoader classLoader, URL resourceUrl) {
        // 读取文件
        try(BufferedReader reader =new BufferedReader(new InputStreamReader(resourceUrl.openStream(), StandardCharsets.UTF_8))){
            String line;
            while((line=reader.readLine())!=null){
                // 获取注释开始的位置
                final int ci = line.indexOf('#');
                if(ci>=0){
                    line = line.substring(0,ci);
                }
                // 去掉字符串两端多余的空格
                line=line.trim();
                if(line.length()>0){
                    try{
                        final int ei = line.indexOf('=');
                        String name =line.substring(0,ei).trim();
                        String clazzName=line.substring(ei+1).trim();

                        if(name.length()>0&&clazzName.length()>0){
                            Class<?> clazz=classLoader.loadClass(clazzName);
                            extensionClassMap.putIfAbsent(name,clazz);
                        }
                    }catch (ClassNotFoundException e){
                        log.error(e.getMessage());
                    }
                }
            }
        }catch (IOException e){
            log.error(e.getMessage());
        }
    }

}
