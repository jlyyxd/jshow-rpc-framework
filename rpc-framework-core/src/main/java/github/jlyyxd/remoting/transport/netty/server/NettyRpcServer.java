package github.jlyyxd.remoting.transport.netty.server;

import github.jlyyxd.utils.RuntimeUtil;
import github.jlyyxd.utils.concurrent.ThreadFactoryBuilder.ThreadPoolFactoryUtil;
import github.jlyyxd.remoting.transport.netty.codec.RpcMessageDecoder;
import github.jlyyxd.remoting.transport.netty.codec.RpcMessageEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;

@Slf4j
public class NettyRpcServer {

    public static final int PORT = 8081;

    /**
     * 启动rpc服务器方法
     */
    @SneakyThrows
    public void start() {
        String host = InetAddress.getLocalHost().getHostAddress();
        // boss EventGroup只有一个线程,负责连接,生成NioSocketChannel,并分发给workerGroup中的线程处理
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // worker EventGroup，这个线程池的线程数默认为cpu核心数的两倍，负责网络字节数据读写
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        // 可以理解为完成实际业务处理的线程池，线程数为cpu核心数的两倍
        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                RuntimeUtil.cpuNumber() * 2,
                ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false)
        );
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。
                    // TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法，这里设置为关闭。
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new LoggingHandler(LogLevel.INFO))
                                    .addLast(new RpcMessageDecoder())
                                    .addLast(new RpcMessageEncoder())
                                    .addLast(new ChannelInboundHandlerAdapter() {
                                        @Override
                                        public void channelRead(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
                                            log.info("接收到消息：\n{}\n{}", msg,msg.getClass().getName());
                                            channelHandlerContext.writeAndFlush(msg);
                                        }
                                    });
                        }
                    });

            // 绑定端口，并阻塞等待至绑定成功
            ChannelFuture future = b.bind(PORT).sync();
            log.info("端口绑定成功");
            // 阻塞等待服务端关闭
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Occur exception when start server:", e);
        } finally {
            // 关闭事件循环对象，释放其中的线程
            log.error("shutdown bossGroup and workerGroup");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }
}
