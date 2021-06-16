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
 * LogicProcessor.java at 2020-8-22 16:00:59, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.processor;

import io.netty.channel.Channel;
import net.x52im.mobileimsdk.server.ServerCoreHandler;
import net.x52im.mobileimsdk.server.network.Gateway;
import net.x52im.mobileimsdk.server.network.GatewayUDP;
import net.x52im.mobileimsdk.server.network.MBObserver;
import net.x52im.mobileimsdk.server.protocal.Protocal;
import net.x52im.mobileimsdk.server.protocal.ProtocalFactory;
import net.x52im.mobileimsdk.server.protocal.c.PLoginInfo;
import net.x52im.mobileimsdk.server.qos.QoS4ReciveDaemonC2S;
import net.x52im.mobileimsdk.server.qos.QoS4SendDaemonS2C;
import net.x52im.mobileimsdk.server.utils.GlobalSendHelper;
import net.x52im.mobileimsdk.server.utils.LocalSendHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicProcessor {
    private static Logger logger = LoggerFactory.getLogger(LogicProcessor.class);

    private ServerCoreHandler serverCoreHandler = null;

    public LogicProcessor(ServerCoreHandler serverCoreHandler) {
        this.serverCoreHandler = serverCoreHandler;
    }

    /**
     * 处理c2c发送消息
     *
     * @param bridgeProcessor
     * @param session
     * @param pFromClient
     * @param remoteAddress
     * @throws Exception
     */
    public void processC2CMessage(BridgeProcessor bridgeProcessor, Channel session, Protocal pFromClient, String remoteAddress) throws Exception {
        GlobalSendHelper.sendDataC2C(bridgeProcessor, session, pFromClient, remoteAddress, this.serverCoreHandler);
    }

    /**
     * 处理客户端发送消息到服务端的逻辑
     *
     * @param session
     * @param pFromClient
     * @param remoteAddress
     * @throws Exception
     */
    public void processC2SMessage(Channel session, final Protocal pFromClient, String remoteAddress) throws Exception {
        // && processedOK)
        if (pFromClient.isQoS()) {
            boolean hasRecieved = QoS4ReciveDaemonC2S.getInstance().hasRecieved(pFromClient.getFp());
            QoS4ReciveDaemonC2S.getInstance().addRecieved(pFromClient);
            LocalSendHelper.replyRecievedBack(session
                    , pFromClient
                    , (receivedBackSendSucess, extraObj) -> {
                        if (receivedBackSendSucess) {
                            logger.debug("[IMCORE-本机QoS！]【QoS_应答_C2S】向" + pFromClient.getFrom() + "发送" + pFromClient.getFp()
                                    + "的应答包成功了,from=" + pFromClient.getTo() + ".");
                        }
                    }
            );

            if (hasRecieved) {
                if (QoS4ReciveDaemonC2S.getInstance().isDebugable()) {
                    logger.debug("[IMCORE-本机QoS！]【QoS机制】" + pFromClient.getFp() + "因已经存在于发送列表中，这是重复包，本次忽略通知业务处理层（只需要回复ACK就行了）！");
                }

                return;
            }
        }

        boolean processedOK = this.serverCoreHandler.getServerEventListener().onTransferMessage4C2S(pFromClient, session);
    }

    /**
     * 处理消息收到确认流程
     *
     * @param pFromClient   消息的uuid
     * @param remoteAddress 客户端远程的地址
     * @throws Exception
     */
    public void processACK(final Protocal pFromClient, final String remoteAddress) throws Exception {
        String theFingerPrint = pFromClient.getDataContent();
        logger.debug("[IMCORE-本机QoS！]【QoS机制_S2C】收到接收者" + pFromClient.getFrom() + "回过来的指纹为" + theFingerPrint + "的应答包.");

        //调用自定义的回调接口
        if (this.serverCoreHandler.getServerMessageQoSEventListener() != null) {
            this.serverCoreHandler.getServerMessageQoSEventListener().messagesBeReceived(theFingerPrint);
        }
        //删除消息集合中的对应数据
        QoS4SendDaemonS2C.getInstance().remove(theFingerPrint);
    }

    /**
     * 处理登陆逻辑相关
     *
     * @param session       netty 链接对象
     * @param pFromClient   协议信息
     * @param remoteAddress 远程地址
     * @throws Exception
     */
    public void processLogin(final Channel session, final Protocal pFromClient, final String remoteAddress) throws Exception {
        final PLoginInfo loginInfo = ProtocalFactory.parsePLoginInfo(pFromClient.getDataContent());
        logger.info("[IMCORE-{}]>> 客户端" + remoteAddress + "发过来的登陆信息内容是：loginInfo={}|getToken={}", Gateway.$(session), loginInfo.getLoginUserId(), loginInfo.getLoginToken());

        //## Bug FIX: 20170603 by Jack Jiang
        //##          解决在某些极端情况下由于Java PC客户端程序的不合法数据提交而导致登陆数据处理流程发生异常。
        if (loginInfo.getLoginUserId() == null) {
            logger.warn("[IMCORE-{}]>> 收到客户端{}登陆信息，但loginInfo或loginInfo.getLoginUserId()是null，登陆无法继续[loginInfo={},loginInfo.getLoginUserId()={}]！", Gateway.$(session), remoteAddress, loginInfo, loginInfo.getLoginUserId());

            if (!GatewayUDP.isUDPChannel(session)) {
                session.close();
            }
            return;
        }

        if (serverCoreHandler.getServerEventListener() != null) {
            boolean alreadyLogined = OnlineProcessor.isLogined(session);
            //(_try_user_id != -1);
            if (alreadyLogined) {
                logger.debug("[IMCORE-{}]>> 【注意】客户端{}的会话正常且已经登陆过，而此时又重新登陆：getLoginName={}|getLoginPsw={}", Gateway.$(session), remoteAddress, loginInfo.getLoginUserId(), loginInfo.getLoginToken());

                MBObserver retObserver = (_sendOK, extraObj) -> {
                    if (_sendOK) {
                        session.attr(OnlineProcessor.USER_ID_IN_SESSION_ATTRIBUTE_ATTR).set(loginInfo.getLoginUserId());
                        OnlineProcessor.getInstance().putUser(loginInfo.getLoginUserId(), session);
                        serverCoreHandler.getServerEventListener().onUserLoginSucess(loginInfo.getLoginUserId(), loginInfo.getExtra(), session);
                    } else {
                        logger.warn("[IMCORE-{}]>> 发给客户端{}的登陆成功信息发送失败了！", Gateway.$(session), remoteAddress);
                    }
                };

                LocalSendHelper.sendData(session, ProtocalFactory.createPLoginInfoResponse(0, loginInfo.getLoginUserId()), retObserver);
            } else {
                //判断是否允许当前用户登陆
                int code = serverCoreHandler.getServerEventListener().onUserLoginVerify(loginInfo.getLoginUserId(), loginInfo.getLoginToken(), loginInfo.getExtra(), session);
                if (code == 0) {
                    MBObserver sendResultObserver = (__sendOK, extraObj) -> {
                        if (__sendOK) {
                            session.attr(OnlineProcessor.USER_ID_IN_SESSION_ATTRIBUTE_ATTR).set(loginInfo.getLoginUserId());
                            OnlineProcessor.getInstance().putUser(loginInfo.getLoginUserId(), session);
                            serverCoreHandler.getServerEventListener().onUserLoginSucess(loginInfo.getLoginUserId(), loginInfo.getExtra(), session);
                        } else {
                            logger.warn("[IMCORE-{}]>> 发给客户端{}的登陆成功信息发送失败了【no】！", Gateway.$(session), remoteAddress);
                        }

                    };
                    LocalSendHelper.sendData(session, ProtocalFactory.createPLoginInfoResponse(code, loginInfo.getLoginUserId()), sendResultObserver);
                } else {
                    logger.warn("[IMCORE-{}]>> 客户端{}登陆失败【no】，马上返回失败信息，并关闭其会话。。。", Gateway.$(session), remoteAddress);
                    MBObserver sendResultObserver = (sendOK, extraObj) -> {
                        logger.warn("[IMCORE-{}]>> 客户端{}登陆失败信息返回成功？{}（会话即将关闭）", Gateway.$(session), remoteAddress, sendOK);
                        session.close();
                    };
                    LocalSendHelper.sendData(session, ProtocalFactory.createPLoginInfoResponse(code, "-1"), GatewayUDP.isUDPChannel(session) ? null : sendResultObserver);
                }
            }
        } else {
            logger.warn("[IMCORE-{}]>> 收到客户端{}登陆信息，但回调对象是null，没有进行回调.", Gateway.$(session), remoteAddress);
        }
    }

    /**
     * 处理心跳包逻辑
     *
     * @param session
     * @param pFromClient
     * @param remoteAddress
     * @throws Exception
     */
    public void processKeepAlive(Channel session, Protocal pFromClient, String remoteAddress) throws Exception {
        String userId = OnlineProcessor.getUserIdFromSession(session);
        if (userId != null) {
            LocalSendHelper.sendData(ProtocalFactory.createPKeepAliveResponse(userId), null);
        } else {
            logger.warn("[IMCORE-{}]>> Server在回客户端{}的响应包时，调用getUserIdFromSession返回null，用户在这一瞬间掉线了？！", Gateway.$(session), remoteAddress);
        }
    }
}
