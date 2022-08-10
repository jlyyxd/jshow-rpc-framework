package github.jlyyxd.provider.impl;

import github.jlyyxd.config.RpcServiceConfig;
import github.jlyyxd.enums.RpcErrorMessageEnum;
import github.jlyyxd.exception.RpcException;
import github.jlyyxd.extension.ExtensionLoader;
import github.jlyyxd.provider.ServiceProvider;
import github.jlyyxd.registry.ServiceRegistry;
import github.jlyyxd.remoting.transport.netty.server.NettyRpcServer;
import github.jlyyxd.utils.IPUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {

    /**
     * key: rpc service name(interface name + version + group)
     * value: service object
     */
    private final Map<String, Object> serviceMap;
    private final ServiceRegistry serviceRegistry;

    public ZkServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension("zookeeper");
    }

    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        if (serviceMap.containsKey(rpcServiceName)) {
            return;
        }
        serviceMap.putIfAbsent(rpcServiceName, rpcServiceConfig.getService());
        log.info("Add service: {} and interfaces:{}", rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }

    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        try{
            String host = IPUtil.getLocalHostIPStr();
            this.addService(rpcServiceConfig);
            serviceRegistry.registerService(rpcServiceConfig.getServiceName(), new InetSocketAddress(host, NettyRpcServer.PORT));
        }catch (UnknownHostException e){
            log.error("occur exception when getHostAddress", e);
        }
    }
}
