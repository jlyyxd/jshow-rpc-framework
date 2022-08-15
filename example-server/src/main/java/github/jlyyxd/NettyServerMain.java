package github.jlyyxd;

import github.jlyyxd.ServiceImpl.MyServiceImpl;
import github.jlyyxd.config.RpcServiceConfig;
import github.jlyyxd.config.ServerConfig;
import github.jlyyxd.remoting.transport.netty.server.NettyRpcServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class NettyServerMain {
    public static void main(String[] args) {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "Info");

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ServerConfig.class);
        NettyRpcServer nettyRpcServer = applicationContext.getBean(NettyRpcServer.class);
        // 手动创建并发布实例
        MyServiceImpl myService = new MyServiceImpl();
        RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder().group("test").version("1.0").service(myService).build();
        nettyRpcServer.registerService(rpcServiceConfig);

        nettyRpcServer.start();

    }
}
