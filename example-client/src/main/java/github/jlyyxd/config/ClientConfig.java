package github.jlyyxd.config;

import github.jlyyxd.spring.RpcReferenceFieldBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {
    @Bean
    public RpcReferenceFieldBeanPostProcessor getRpcReferenceFieldBeanPostProcessor(){
        return new RpcReferenceFieldBeanPostProcessor("netty");
    }
}
