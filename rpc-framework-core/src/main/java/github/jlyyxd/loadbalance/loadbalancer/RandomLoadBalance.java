package github.jlyyxd.loadbalance.loadbalancer;

import github.jlyyxd.factory.SingletonFactory;
import github.jlyyxd.loadbalance.AbstractLoadBalance;
import github.jlyyxd.remoting.dto.RpcRequest;

import java.util.List;
import java.util.Random;

public class RandomLoadBalance extends AbstractLoadBalance {

    private final Random random = SingletonFactory.getInstance(Random.class);

    @Override
    protected String doSelect(List<String> serviceUrlList, RpcRequest rpcRequest) {
        return serviceUrlList.get(random.nextInt(serviceUrlList.size()));
    }
}
