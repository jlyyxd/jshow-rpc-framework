package github.jlyyxd.remoting.transport.netty.server;

import github.jlyyxd.enums.CompressTypeEnum;
import github.jlyyxd.enums.RpcResponseCodeEnum;
import github.jlyyxd.enums.SerializationTypeEnum;
import github.jlyyxd.factory.SingletonFactory;
import github.jlyyxd.handler.RpcRequestHandler;
import github.jlyyxd.remoting.constants.RpcConstants;
import github.jlyyxd.remoting.dto.RpcMessage;
import github.jlyyxd.remoting.dto.RpcRequest;
import github.jlyyxd.remoting.dto.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Customize the ChannelHandler of the server to process the data sent by the client.
 * <p>
 * 如果继承自 SimpleChannelInboundHandler 的话就不要考虑 ByteBuf 的释放 ，{@link SimpleChannelInboundHandler} 内部的
 * channelRead 方法会替你释放 ByteBuf ，避免可能导致的内存泄露问题。详见《Netty进阶之路 跟着案例学 Netty》
 */
@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {

    private final RpcRequestHandler rpcRequestHandler;

    public NettyRpcServerHandler() {
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            // 只处理RpcMessage类型的消息
            if (msg instanceof RpcMessage) {
                log.info("server receive msg: [{}] ", msg);
                RpcMessage inMessage = (RpcMessage) msg;
                byte messageType = inMessage.getMessageType();
                RpcMessage outMessage = new RpcMessage();
                outMessage.setCodec(SerializationTypeEnum.HESSIAN.getCode());
                outMessage.setCompress(CompressTypeEnum.GZIP.getCode());

                // 判断是否心跳包
                if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                    outMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                    outMessage.setData(RpcConstants.PONG);
                } else {
                    outMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                    RpcRequest rpcRequest = (RpcRequest) inMessage.getData();
                    // 执行目标方法并得到结果
                    Object result = rpcRequestHandler.handle(rpcRequest);
                    log.info(String.format("server get result: %s", result.toString()));

                    // 查看通道是否可用、连接是否正常
                    if(ctx.channel().isActive()&&ctx.channel().isWritable()){
                        RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                        outMessage.setData(rpcResponse);
                    }else{
                        RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                        outMessage.setData(rpcResponse);
                        log.error("not writable now, message dropped");
                    }
                }
                ctx.writeAndFlush(outMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } finally{
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
            IdleState state = ((IdleStateEvent) evt).state();
            if(state==IdleState.READER_IDLE){
                // 对应时间为30s无输入断开连接
                log.info("idle check happen, so close the connection");
                ctx.close();
            }else{
                super.userEventTriggered(ctx, evt);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }
}
