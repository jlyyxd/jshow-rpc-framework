package github.jlyyxd.remoting.transport.netty.server;

import github.jlyyxd.config.RpcServiceConfig;
import github.jlyyxd.factory.SingletonFactory;
import github.jlyyxd.provider.ServiceProvider;
import github.jlyyxd.provider.impl.ZkServiceProviderImpl;
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
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyRpcServer {

    public static final int PORT = 8081;

    // 从单例工厂中获取ServiceProvider用于发布服务
    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);

    public void registerService(RpcServiceConfig rpcServiceConfig){
        // 发布服务
        // ① 将服务对象保存到内存中，便于调用
        // ② 服务名/{ip}:{port} 信息添加到注册中心，方便rpc客户端寻找服务
        serviceProvider.publishService(rpcServiceConfig);
    }
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
                    // 开启tcp心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .childOption(ChannelOption.SO_BACKLOG,128)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new LoggingHandler(LogLevel.INFO))
                                    // 30s没有接收到数据则触发时间(处理事件会进行关闭连接)
                                    .addLast(new IdleStateHandler(30,0,0, TimeUnit.SECONDS))
                                    .addLast(new RpcMessageDecoder())
                                    .addLast(new RpcMessageEncoder())
                                    .addLast(serviceHandlerGroup, new NettyRpcServerHandler());
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
