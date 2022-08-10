package github.jlyyxd;

import github.jlyyxd.enums.CompressTypeEnum;
import github.jlyyxd.enums.SerializationTypeEnum;
import github.jlyyxd.remoting.constants.RpcConstants;
import github.jlyyxd.remoting.dto.RpcMessage;
import github.jlyyxd.remoting.dto.RpcRequest;
import github.jlyyxd.remoting.transport.netty.codec.RpcMessageDecoder;
import github.jlyyxd.remoting.transport.netty.codec.RpcMessageEncoder;
import github.jlyyxd.remoting.transport.netty.server.NettyRpcServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;

import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class TestClientAndServer {
    @Test
    public void startServer() {
        NettyRpcServer nettyRpcServer = new NettyRpcServer();
        nettyRpcServer.start();
    }

    @Test
    public void startClient() {
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(worker)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new LoggingHandler(LogLevel.DEBUG))
                                    .addLast(new RpcMessageDecoder())
                                    .addLast(new RpcMessageEncoder())
                                    .addLast(new ChannelInboundHandlerAdapter() {
                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                            log.info("tcp连接成功");
                                            RpcMessage rpcMessage = RpcMessage.builder()
                                                    .messageType(RpcConstants.REQUEST_TYPE)
                                                    .compress(CompressTypeEnum.GZIP.getCode())
                                                    .codec(SerializationTypeEnum.HESSIAN.getCode())
                                                    .data(new RpcRequest())
                                                    .requestId(1)
                                                    .build();
                                            ctx.writeAndFlush(rpcMessage);
                                        }

                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                            log.info("服务器返回信息：{}", msg);
                                        }
                                    });
                        }
                    });
            ChannelFuture channelFuture = bootstrap.connect("localhost", 8081).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            worker.shutdownGracefully();
        }
    }
}
