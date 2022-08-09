package github.jlyyxd.remoting.transport;

import github.jlyyxd.remoting.dto.RpcRequest;

public interface RpcRequestTransport {
    /**
     *
     * @param rpcRequest    向服务端发送的rpc请求
     * @return  服务端返回结果
     */
    Object sendRpcRequest(RpcRequest rpcRequest);
}
