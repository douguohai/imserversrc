package net.x52im.mobileimsdk.server.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.x52im.mobileimsdk.server.ServerCoreHandler;
import net.x52im.mobileimsdk.server.network.tcp.MBTCPClientInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tianwen
 */
public class GatewayTCP extends Gateway {
    private static Logger logger = LoggerFactory.getLogger(GatewayTCP.class);

    /**
     * tcp 监听端口
     */
    public static int PORT = 8901;
    public static int SESION_RECYCLER_EXPIRE = 20;//10;
    public static int TCP_FRAME_FIXED_HEADER_LENGTH = 4;     // 4 bytes
    public static int TCP_FRAME_MAX_BODY_LENGTH = 6 * 1024; // 6K bytes

    protected final EventLoopGroup bossGroup4Netty = new NioEventLoopGroup(1);
    protected final EventLoopGroup workerGroup4Netty = new NioEventLoopGroup();
    protected Channel serverChannel4Netty = null;

    protected ServerBootstrap bootstrap = null;

    @Override
    public void init(ServerCoreHandler serverCoreHandler) {
        bootstrap = new ServerBootstrap()
                .group(bossGroup4Netty, workerGroup4Netty)
                .channel(NioServerSocketChannel.class)
                .childHandler(initChildChannelHandler(serverCoreHandler));

        bootstrap.option(ChannelOption.SO_BACKLOG, 4096);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
    }

    @Override
    public void bind() throws Exception {
        ChannelFuture cf = bootstrap.bind(PORT).sync();
        if (cf.isSuccess()) {
            logger.info("[IMCORE-tcp] 基于MobileIMSDK的TCP服务绑定端口成功 √");
        } else {
            logger.info("[IMCORE-tcp] 基于MobileIMSDK的TCP服务绑定端口失败 ×");
        }

        serverChannel4Netty = cf.channel();
        serverChannel4Netty.closeFuture().addListener((ChannelFutureListener) future -> {
            bossGroup4Netty.shutdownGracefully();
            workerGroup4Netty.shutdownGracefully();
        });

        logger.info("[IMCORE-tcp] .... continue ...");
        logger.info("[IMCORE-tcp] 基于MobileIMSDK的TCP服务正在端口" + PORT + "上监听中...");
    }

    @Override
    public void shutdown() {
        if (serverChannel4Netty != null)
            serverChannel4Netty.close();
    }

    protected ChannelHandler initChildChannelHandler(final ServerCoreHandler serverCoreHandler) {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {

                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                        TCP_FRAME_FIXED_HEADER_LENGTH + TCP_FRAME_MAX_BODY_LENGTH
                        , 0, TCP_FRAME_FIXED_HEADER_LENGTH, 0, TCP_FRAME_FIXED_HEADER_LENGTH));
                pipeline.addLast("frameEncoder", new LengthFieldPrepender(TCP_FRAME_FIXED_HEADER_LENGTH));
                pipeline.addLast(new ReadTimeoutHandler(SESION_RECYCLER_EXPIRE));
                pipeline.addLast(new MBTCPClientInboundHandler(serverCoreHandler));
            }
        };
    }
}
