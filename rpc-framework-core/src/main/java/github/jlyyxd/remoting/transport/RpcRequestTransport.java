package github.jlyyxd.remoting.transport;

import github.jlyyxd.extension.SPI;
import github.jlyyxd.remoting.dto.RpcRequest;
import github.jlyyxd.remoting.dto.RpcResponse;

import java.util.concurrent.CompletableFuture;

@SPI
public interface RpcRequestTransport {
    /**
     *
     * @param rpcRequest    向服务端发送的rpc请求
     * @return  服务端返回结果
     */
    CompletableFuture<RpcResponse<Object>> sendRpcRequest(RpcRequest rpcRequest);
}
