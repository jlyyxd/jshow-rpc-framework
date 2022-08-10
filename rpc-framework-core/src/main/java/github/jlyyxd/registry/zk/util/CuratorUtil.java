package github.jlyyxd.registry.zk.util;

import github.jlyyxd.enums.RpcConfigEnum;
import github.jlyyxd.utils.PropertiesFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class CuratorUtil {
    private static final int BASE_SLEEP_TIME = 1000;
    // 最大重试次数
    private static final int MAX_RETRIES = 3;
    // 服务注册根目录
    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";
    // 服务地址map：key -> rpc服务名，value -> 服务器地址列表
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    // zk节点路径集合
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();

    private static CuratorFramework zkClient;
    // 默认zookeeper地址
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";
    // 控制并发的锁对象
    private static final Object lock = new Object();

    private CuratorUtil() {
    }

    /**
     * Create persistent nodes. Unlike temporary nodes, persistent nodes are not removed when the client disconnects
     * 创建持久节点。与临时节点不同，持久节点在客户端断开连接时不会被移除
     *
     * @param path node path
     */
    public static void createPersistentNode(CuratorFramework zkClient, String path) {
        try {
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                // 节点已经存在
                log.info("The node {} already exists.", path);
            } else {
                // 节点不存在
                //eg: /my-rpc/github.codeReaper2001.HelloService/127.0.0.1:9999
                zkClient.create().creatingParentContainersIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("The node [{}] was created successfully.", path);
            }
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            log.error("create persistent node for path [{}] fail", path);
        }
    }

    /**
     * Gets the children under a node
     *
     * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version1
     * @return All child nodes under the specified node
     * 即获取某rpc服务的所有服务器ip地址列表
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
        // 查找缓存
        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }

        // 查找zk
        List<String> result = null;
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        try {
            result = zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
            // 监听节点变化
            registerWatcher(rpcServiceName, zkClient);
        } catch (Exception e) {
            log.error("get children nodes for path [{}] fail", servicePath);
        }
        return result;
    }

    /**
     * Empty the registry of data
     * 取消注册，即将某ip地址主机的所有服务下线
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        List<String> toDelete = new ArrayList<>();
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            // 遍历所有路径，删除目标ip地址所在节点
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    toDelete.add(p);
                    zkClient.delete().forPath(p);
                }
            } catch (Exception e) {
                log.error("clear registry for path [{}] fail", p);
            }
        });
        for (String s : toDelete) {
            REGISTERED_PATH_SET.remove(s);
            String serviceName = getServiceNameFromNodePath(s);
            List<String> addressList = SERVICE_ADDRESS_MAP.get(serviceName);
            List<String> result = addressList.stream().filter(
                    it -> !(inetSocketAddress.toString().endsWith(it))).collect(Collectors.toList());
            SERVICE_ADDRESS_MAP.put(serviceName,result);
        }
        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET.toString());
    }

    public static CuratorFramework getZkClient() {
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }
        synchronized (lock) {
            if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
                return zkClient;
            }
            // 优先使用配置文件中的zk地址
            Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
            String zookeeperAddress = properties != null && properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null
                    ? properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue())
                    : DEFAULT_ZOOKEEPER_ADDRESS;

            // 重试策略。重试 3 次，会增加重试之间的休眠时间
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
            zkClient = CuratorFrameworkFactory.builder()
                    .connectString(zookeeperAddress)
                    .retryPolicy(retryPolicy)
                    .build();
            zkClient.start();

            try {
                // 等待直到连上zk，最大等待时间为30s
                if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Time out waiting to connect to ZK!");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return zkClient;
        }
    }

    /**
     * Registers to listen for changes to the specified node
     * 注册监听指定节点的变化，当变化时更新SERVICE_ADDRESS_MAP中key为rpcServiceName键值对的value
     *
     * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version
     */
    private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception {
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);
        PathChildrenCacheListener pathChildrenCacheListener = ((curatorFramework, pathChildrenCacheEvent) -> {
            // 每次/my-rpc/{rpcServiceName}路径节点发生变化，就会更新SERVICE_ADDRESS_MAP
            // 例如某主机服务下线，就不会再访问对应服务
            List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);

            // 更新REGISTERED_PATH_SET
            List<String> toDelete = new ArrayList<>();
            REGISTERED_PATH_SET.forEach(it->{
                String serviceName = getServiceNameFromNodePath(it);
                // 判断当前元素的服务名是否与发生改变的服务名相等
                if(serviceName.equals(rpcServiceName)){
                    boolean isDelete = true;
                    for (String serviceAddress : serviceAddresses) {
                        if (serviceAddress.equals(getServiceAddressFromNodePath(it))) {
                            isDelete=false;
                            break;
                        }
                    }
                    if(isDelete){
                        toDelete.add(it);
                    }
                }
            });
            toDelete.forEach(REGISTERED_PATH_SET::remove);

            // 更新SERVICE_ADDRESS_MAP
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
        });
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();
    }

    public static String getServiceNameFromNodePath(String path){
        String[] split = path.split("/");
        return split[2];
    }

    public static String getServiceAddressFromNodePath(String path){
        String[] split = path.split("/");
        return split[3];
    }
}

