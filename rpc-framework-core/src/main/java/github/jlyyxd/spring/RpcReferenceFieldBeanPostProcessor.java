package github.jlyyxd.spring;

import github.jlyyxd.annotation.RpcReference;
import github.jlyyxd.config.RpcServiceConfig;
import github.jlyyxd.extension.ExtensionLoader;
import github.jlyyxd.proxy.RpcClientProxy;
import github.jlyyxd.remoting.transport.RpcRequestTransport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;

public class RpcReferenceFieldBeanPostProcessor implements BeanPostProcessor {

    private final RpcRequestTransport rpcClient;

    public RpcReferenceFieldBeanPostProcessor(String clientExtensionName){
        rpcClient= ExtensionLoader.getExtensionLoader(RpcRequestTransport.class).getExtension(clientExtensionName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        /*
         * 1. 获取class对象
         * 2. 遍历所有字段，如果该字段被@RpcReference注解修饰，则生成代理对象填充到该字段上
         * */
        Class<?> targetClass = bean.getClass();
        Field[] declaredFields = targetClass.getDeclaredFields();
        for (Field field : declaredFields) {
            RpcReference rpcReference = field.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                        .group(rpcReference.group())
                        .version(rpcReference.version()).build();
                RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient, rpcServiceConfig);
                Object serviceProxy = rpcClientProxy.getProxy(field.getType());
                field.setAccessible(true);

                try{
                    field.set(bean, serviceProxy);
                }catch (IllegalAccessException e){
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }
}
