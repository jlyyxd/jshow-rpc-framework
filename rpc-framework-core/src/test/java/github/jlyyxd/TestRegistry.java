package github.jlyyxd;

import github.jlyyxd.enums.RegistryCenterEnum;
import github.jlyyxd.extension.ExtensionLoader;
import github.jlyyxd.registry.ServiceRegistry;
import github.jlyyxd.registry.zk.util.CuratorUtil;
import github.jlyyxd.utils.IPUtil;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

public class TestRegistry {
    public static void main(String[] args) throws Exception {
        ServiceRegistry serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).
                getExtension(RegistryCenterEnum.zookeeper.getRegistryCenterName());
        String localHostIPStr = IPUtil.getLocalHostIPStr();

        InetSocketAddress inetSocketAddress = new InetSocketAddress(localHostIPStr, 9000);
        serviceRegistry.registerService("test",inetSocketAddress);

        CuratorFramework zkClient = CuratorUtil.getZkClient();

        inetSocketAddress=new InetSocketAddress(localHostIPStr,10000);
        serviceRegistry.registerService("test",inetSocketAddress);
        List<String> test = CuratorUtil.getChildrenNodes(zkClient, "test");
        test.forEach(System.out::println);
        CuratorUtil.clearRegistry(zkClient,inetSocketAddress);
        test = CuratorUtil.getChildrenNodes(zkClient, "test");
        test.forEach(System.out::println);
    }
}
