package github.jlyyxd.loadbalance;

import github.jlyyxd.remoting.dto.RpcRequest;
import github.jlyyxd.utils.CollectionUtil;

import java.util.List;

public abstract class AbstractLoadBalance implements LoadBalance {
    @Override
    public String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest) {
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            return null;
        }
        if (serviceUrlList.size() == 1) {
            return serviceUrlList.get(0);
        }
        return doSelect(serviceUrlList, rpcRequest);
    }

    protected abstract String doSelect(List<String> serviceUrlList, RpcRequest rpcRequest);
}
