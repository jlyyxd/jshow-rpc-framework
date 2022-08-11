package github.jlyyxd.registry;

import github.jlyyxd.extension.SPI;
import github.jlyyxd.remoting.dto.RpcRequest;

import java.net.InetSocketAddress;

@SPI
public interface ServiceDisocovery {
    InetSocketAddress lookUpService(RpcRequest rpcRequest);
}
