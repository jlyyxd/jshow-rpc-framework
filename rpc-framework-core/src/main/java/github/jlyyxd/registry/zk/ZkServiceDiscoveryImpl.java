package github.jlyyxd.registry.zk;

import github.jlyyxd.enums.RpcErrorMessageEnum;
import github.jlyyxd.exception.RpcException;
import github.jlyyxd.extension.ExtensionLoader;
import github.jlyyxd.loadbalance.LoadBalance;
import github.jlyyxd.registry.ServiceDiscovery;
import github.jlyyxd.registry.zk.util.CuratorUtil;
import github.jlyyxd.remoting.dto.RpcRequest;
import github.jlyyxd.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    private final LoadBalance loadBalance;

    public ZkServiceDiscoveryImpl() {
        loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension("loadBalance");
    }

    @Override
    public InetSocketAddress lookUpService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        CuratorFramework zkClient = CuratorUtil.getZkClient();
        List<String> serviceUrlList = CuratorUtil.getChildrenNodes(zkClient, rpcServiceName);
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        // 负载均衡
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("Successfully found the service address:[{}]", targetServiceUrl);
        String[] UrlAndPort = targetServiceUrl.split(":");
        String host = UrlAndPort[0];
        int port = Integer.parseInt(UrlAndPort[1]);
        return new InetSocketAddress(host,port);
    }
}
