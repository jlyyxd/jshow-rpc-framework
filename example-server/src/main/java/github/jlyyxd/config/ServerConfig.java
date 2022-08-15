package github.jlyyxd.config;

import github.jlyyxd.annotation.RpcScan;
import github.jlyyxd.remoting.transport.netty.server.NettyRpcServer;
import github.jlyyxd.spring.RpcServiceRegisterPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RpcScan(basePackage = "github.jlyyxd.ServiceImpl")
@Configuration
public class ServerConfig {
    @Bean
    public RpcServiceRegisterPostProcessor getRpcSericeBeanPostProcessor(){
        return new RpcServiceRegisterPostProcessor();
    }

    @Bean
    public NettyRpcServer getNettyRpcServer(){
        return new NettyRpcServer();
    }

}
