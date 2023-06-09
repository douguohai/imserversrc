package net.x52im.mobileimsdk.server.network.websocket;

/**
 * <p>
 * java类作用描述
 * </p>
 *
 * @author : tianwen
 * @version : 1.0
 * @date : 2023/6/9 17:12
 **/

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.ReadTimeoutException;
import net.x52im.mobileimsdk.server.ServerCoreHandler;
import net.x52im.mobileimsdk.server.network.tcp.MBTCPClientInboundHandler;
import net.x52im.mobileimsdk.server.protocal.Protocal;
import net.x52im.mobileimsdk.server.utils.ServerToolKits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author tianwen
 */
public class MBWebsocketClientInboundHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private static Logger logger = LoggerFactory.getLogger(MBTCPClientInboundHandler.class);
    private ServerCoreHandler serverCoreHandler = null;

    public MBWebsocketClientInboundHandler(ServerCoreHandler serverCoreHandler) {
        this.serverCoreHandler = serverCoreHandler;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        try {
            if (e instanceof ReadTimeoutException) {
                logger.info("[IMCORE-ws]客户端{}的会话已超时失效，很可能是对方非正常通出或网络故障" +
                        "，即将以会话异常的方式执行关闭流程 ...", ServerToolKits.clientInfoToString(ctx.channel()));
            }
            serverCoreHandler.exceptionCaught(ctx.channel(), e);
        } catch (Exception e2) {
            logger.warn(e2.getMessage(), e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        serverCoreHandler.sessionCreated(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        serverCoreHandler.sessionClosed(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            String frameContent = ((TextWebSocketFrame) frame).text();
            if (frameContent != null) {
                Protocal pFromClient = ServerToolKits.toProtocal(frameContent);
                serverCoreHandler.messageReceived(ctx.channel(), pFromClient);
            } else {
                throw new UnsupportedOperationException("不支持的 frame content (is null!!)");
            }
        } else {
            throw new UnsupportedOperationException("不支持的 frame type: " + frame.getClass().getName());
        }
    }
}
