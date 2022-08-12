package github.jlyyxd.proxy;

import github.jlyyxd.config.RpcServiceConfig;
import github.jlyyxd.enums.RpcErrorMessageEnum;
import github.jlyyxd.enums.RpcResponseCodeEnum;
import github.jlyyxd.exception.RpcException;
import github.jlyyxd.remoting.dto.RpcRequest;
import github.jlyyxd.remoting.dto.RpcResponse;
import github.jlyyxd.remoting.transport.RpcRequestTransport;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RpcClientProxy implements InvocationHandler {

    private static final String INTERFACE_NAME = "interfaceName";
    // 用于发送rpc请求
    private final RpcRequestTransport rpcRequestTransport;
    // 用于构造RpcMessage
    private final RpcServiceConfig rpcServiceConfig;

    public RpcClientProxy(RpcRequestTransport rpcRequestTransport, RpcServiceConfig rpcServiceConfig) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcServiceConfig = rpcServiceConfig;
    }

    public RpcClientProxy(RpcRequestTransport rpcRequestTransport) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcServiceConfig = new RpcServiceConfig();
    }

    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> interfaceClazz) {
        return (T) Proxy.newProxyInstance(interfaceClazz.getClassLoader(),
                new Class[]{interfaceClazz},
                this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("invoked method: [{}]", method.getName());
        RpcRequest rpcRequest = RpcRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .paramTypes(method.getParameterTypes())
                .parameters(args)
                .group(rpcServiceConfig.getGroup())
                .version(rpcServiceConfig.getVersion()).build();
        CompletableFuture<RpcResponse<Object>> completableFuture = rpcRequestTransport.sendRpcRequest(rpcRequest);
        // 阻塞等待相应
        RpcResponse<Object> rpcResponse = completableFuture.get();
        check(rpcResponse, rpcRequest);
        return rpcResponse.getData();
    }

    private void check(RpcResponse<Object> rpcResponse, RpcRequest rpcRequest) {
        if (rpcResponse == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
        // 检查返回数据包的requestId是否对应
        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException(RpcErrorMessageEnum.REQUEST_NOT_MATCH_RESPONSE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
        // 检查是否成功完成调用
        if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
    }
}
