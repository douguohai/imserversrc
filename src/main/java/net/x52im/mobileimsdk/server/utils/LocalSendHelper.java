package net.x52im.mobileimsdk.server.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import net.x52im.mobileimsdk.server.ServerCoreHandler;
import net.x52im.mobileimsdk.server.network.Gateway;
import net.x52im.mobileimsdk.server.network.GatewayUDP;
import net.x52im.mobileimsdk.server.network.MBObserver;
import net.x52im.mobileimsdk.server.processor.OnlineProcessor;
import net.x52im.mobileimsdk.server.protocal.ErrorCode;
import net.x52im.mobileimsdk.server.protocal.Protocal;
import net.x52im.mobileimsdk.server.protocal.ProtocalFactory;
import net.x52im.mobileimsdk.server.qos.QoS4SendDaemonS2C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalSendHelper {
    private static Logger logger = LoggerFactory.getLogger(ServerCoreHandler.class);

    public static void sendData(String to_user_id, String dataContent, MBObserver resultObserver) throws Exception {
        sendData(to_user_id, dataContent, true, null, -1, resultObserver);
    }

    public static void sendData(String to_user_id, String dataContent
            , int typeu, MBObserver resultObserver) throws Exception {
        sendData(to_user_id, dataContent, true, null, typeu, resultObserver);
    }

    public static void sendData(String to_user_id, String dataContent, boolean QoS, int typeu, MBObserver resultObserver) throws Exception {
        sendData(to_user_id, dataContent, QoS, null, typeu, resultObserver);
    }

    public static void sendData(String to_user_id, String dataContent, boolean QoS, String fingerPrint, MBObserver resultObserver) throws Exception {
        sendData(to_user_id, dataContent, QoS, fingerPrint, -1, resultObserver);
    }

    public static void sendData(String to_user_id, String dataContent, boolean QoS, String fingerPrint, int typeu, MBObserver resultObserver) throws Exception {
        sendData(ProtocalFactory.createCommonData(dataContent, "0", to_user_id, QoS, fingerPrint, typeu), resultObserver);
    }

    public static void sendData(Protocal p, MBObserver resultObserver) throws Exception {
        if (p != null) {
            //???????????????????????????????????????
            if (!"0".equals(p.getTo())) {
                sendData(OnlineProcessor.getInstance().getOnlineSession(p.getTo()), p, resultObserver);
            } else {
                logger.warn("[IMCORE]???????????????Protocal?????????????????????????????????(user_id==0)??????????????????????????????Server?????????????????????????????????????????????????????????????????????" + p.toGsonString());
                if (resultObserver != null) {
                    resultObserver.update(false, null);
                }
            }
        } else {
            if (resultObserver != null) {
                resultObserver.update(false, null);
            }
        }
    }

    public static void sendData(final Channel session, final Protocal p, final MBObserver resultObserver) throws Exception {
        if (session == null) {
            logger.info("[IMCORE-{}]toSession==null >> id={}??????????????????????????????{}????????????str={}???????????????id?????????????????????????????????????????????(????????????????????????????????????).", Gateway.$(session), p.getFrom(), p.getTo(), p.getDataContent());
        } else {
            if (session.isActive()) {
                if (p != null) {
                    final byte[] res = p.toBytes();
                    //????????????????????????
                    ByteBuf to = Unpooled.copiedBuffer(res);
                    ChannelFuture cf = session.writeAndFlush(to);//.sync();

                    cf.addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            if (p.isQoS() && !QoS4SendDaemonS2C.getInstance().exist(p.getFp())) {
                                QoS4SendDaemonS2C.getInstance().put(p);
                            }
                        } else {
                            logger.warn("[IMCORE-{}]???????????????{}?????????->{},???????????????[{}](????????????????????????????????????).", Gateway.$(session), ServerToolKits.clientInfoToString(session), p.toGsonString(), res.length);
                        }
                        if (resultObserver != null) {
                            resultObserver.update(future.isSuccess(), null);
                        }
                    });

                    // ## Bug FIX: 20171226 by JS, ???????????????????????????????????????ChannelFutureListener???????????????
                    //            ???????????????return????????????????????????resultObserver.update(false, null);?????????
                    //            ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????listener?????????
                    return;
                    // ## Bug FIX: 20171226 by JS END
                } else {
                    logger.warn("[IMCORE-{}]?????????id={}??????????????????{}??????????????????str={}????????????(????????????????????????????????????).", Gateway.$(session), p.getFrom(), p.getTo(), p.getDataContent());
                }
            }
        }

        if (resultObserver != null) {
            resultObserver.update(false, null);
        }
    }

    public static void replyDataForUnlogined(final Channel session, Protocal p, MBObserver resultObserver) throws Exception {
        logger.warn("[IMCORE-{}]>> ?????????{}???????????????{}???????????????."
                , Gateway.$(session), ServerToolKits.clientInfoToString(session), p.getDataContent());

        if (resultObserver == null) {
            resultObserver = (sendOK, extraObj) -> {
                logger.warn("[IMCORE-{}]>> ?????????{}??????????????????????????????????????????{}????????????????????????", Gateway.$(session), ServerToolKits.clientInfoToString(session), sendOK);

                if (!GatewayUDP.isUDPChannel(session)) {
                    session.close();
                }
            };
        }

        Protocal perror = ProtocalFactory.createPErrorResponse(ErrorCode.ForS.RESPONSE_FOR_UNLOGIN, p.toGsonString(), "-1"); // ???????????????user_id???????????????,???-1????????????????????????????????????????????????
        sendData(session, perror, resultObserver);
    }

    public static void replyRecievedBack(Channel session, Protocal pFromClient, MBObserver resultObserver) throws Exception {
        if (pFromClient.isQoS() && pFromClient.getFp() != null) {
            Protocal receivedBackP = ProtocalFactory.createRecivedBack(pFromClient.getTo(), pFromClient.getFrom(), pFromClient.getFp());
            sendData(session, receivedBackP, resultObserver);
        } else {
            logger.warn("[IMCORE-{}]??????{}???????????????QoS?????????????????????????????????null??????????????????????????????", Gateway.$(session), pFromClient.getFrom());

            if (resultObserver != null)
                resultObserver.update(false, null);
        }
    }
}
