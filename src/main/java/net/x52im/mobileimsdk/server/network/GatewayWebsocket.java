/*
 * Copyright (C) 2020  即时通讯网(52im.net) & Jack Jiang.
 * The MobileIMSDK v5.x Project.
 * All rights reserved.
 *
 * > Github地址：https://github.com/JackJiang2011/MobileIMSDK
 * > 文档地址：  http://www.52im.net/forum-89-1.html
 * > 技术社区：  http://www.52im.net/
 * > 技术交流群：320837163 (http://www.52im.net/topic-qqgroup.html)
 * > 作者公众号：“【即时通讯技术圈】”，欢迎关注！
 * > 联系作者：  http://www.52im.net/thread-2792-1-1.html
 *
 * "即时通讯网(52im.net) - 即时通讯开发者社区!" 推荐开源工程。
 *
 * GatewayWebsocket.java at 2020-8-22 16:00:58, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import net.x52im.mobileimsdk.server.ServerCoreHandler;
import net.x52im.mobileimsdk.server.network.websocket.MBWebsocketClientInboundHandler;

/**
 * @author tianwen
 */
@Slf4j
public class GatewayWebsocket extends Gateway {

    /**
     * 设置端口
     */
    public static int PORT = 9901;

    private static final String WEBSOCKET_PATH = "/websocket";

    protected final EventLoopGroup bossGroup = new NioEventLoopGroup();

    protected final EventLoopGroup workerGroup = new NioEventLoopGroup();

    protected Channel __serverChannel4Netty = null;

    protected ServerBootstrap bootstrap = null;

    public static int SESSION_RECYCLER_EXPIRE = 20;

    @Override
    public void init(ServerCoreHandler serverCoreHandler) {
        bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(initChildChannelHandler(serverCoreHandler));
    }

    @Override
    public void bind() throws Exception {
        ChannelFuture cf = bootstrap.bind(PORT).sync();
        if (cf.isSuccess()) {
            log.info("[IMCORE-ws] 基于MobileIMSDK的WebSocket服务绑定端口" + PORT + "成功 √ ");
        } else {
            log.info("[IMCORE-ws] 基于MobileIMSDK的WebSocket服务绑定端口" + PORT + "失败 ×");
        }

        __serverChannel4Netty = cf.channel();
        __serverChannel4Netty.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        });

        log.info("[IMCORE-ws] .... continue ...");
        log.info("[IMCORE-ws] 基于MobileIMSDK的WebSocket服务正在端口" + PORT + "上监听中...");
    }

    @Override
    public void shutdown() {
        if (__serverChannel4Netty != null) {
            __serverChannel4Netty.close();
        }
    }

    protected ChannelHandler initChildChannelHandler(final ServerCoreHandler serverCoreHandler) {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();

                pipeline.addLast(new HttpServerCodec());
                pipeline.addLast(new HttpObjectAggregator(65536));
                pipeline.addLast(new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true));
                pipeline.addLast(new ReadTimeoutHandler(SESSION_RECYCLER_EXPIRE));
                pipeline.addLast(new MBWebsocketClientInboundHandler(serverCoreHandler));
            }
        };
    }
}
