package github.jlyyxd.registry.zk;


import github.jlyyxd.registry.ServiceRegistry;
import github.jlyyxd.registry.zk.util.CuratorUtil;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;

public class ZkServiceRegistryImpl implements ServiceRegistry {
    @Override
    public void registerService(String serviceName, InetSocketAddress inetSocketAddress) {
        // 注册<服务->ip地址>映射关系 到zookeeper上
        String servicePath = CuratorUtil.ZK_REGISTER_ROOT_PATH + "/" + serviceName + inetSocketAddress.toString();
        CuratorFramework zkClient = CuratorUtil.getZkClient();
        CuratorUtil.createPersistentNode(zkClient, servicePath);
    }
}
