package github.jlyyxd.spring;

import github.jlyyxd.annotation.RpcService;
import github.jlyyxd.config.RpcServiceConfig;
import github.jlyyxd.factory.SingletonFactory;
import github.jlyyxd.provider.ServiceProvider;
import github.jlyyxd.provider.impl.ZkServiceProviderImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

@Slf4j
public class RpcServiceRegisterPostProcessor implements BeanPostProcessor {
    private final ServiceProvider serviceProvider;

    public RpcServiceRegisterPostProcessor(ServiceProvider serviceProvider) {
        this.serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(bean.getClass().isAnnotationPresent(RpcService.class)){
            log.info("[{}] is annotated with  [{}]", bean.getClass().getName(), RpcService.class.getCanonicalName());
            RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
            RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                    .group(rpcService.group())
                    .version(rpcService.version())
                    .service(bean).build();
            serviceProvider.publishService(rpcServiceConfig);
        }
        return bean;
    }
}
