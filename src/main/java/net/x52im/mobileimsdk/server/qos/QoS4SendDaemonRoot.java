package net.x52im.mobileimsdk.server.qos;

import net.x52im.mobileimsdk.server.ServerLauncher;
import net.x52im.mobileimsdk.server.network.MBObserver;
import net.x52im.mobileimsdk.server.protocal.Protocal;
import net.x52im.mobileimsdk.server.utils.LocalSendHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * S2C模式中QoS数据包质量包证机制之发送队列保证实现类.
 * 本类是QoS机制的核心，目的是加强保证TCP协议在应用层的可靠性和送达率。
 */
public class QoS4SendDaemonRoot {
    private static Logger logger = LoggerFactory.getLogger(QoS4SendDaemonRoot.class);

    private boolean DEBUG = false;

    private ServerLauncher serverLauncher = null;

    /**
     * 待发送的消息集合
     */
    private ConcurrentSkipListMap<String, Protocal> sentMessages = new ConcurrentSkipListMap<String, Protocal>();

    /**
     * 消息的唯一id和服务端接收到当前消息的时间映射
     */
    private ConcurrentMap<String, Long> sendMessagesTimestamp = new ConcurrentHashMap<String, Long>();

    /**
     * 检查间隔
     */
    private int CHECH_INTERVAL = 5000;

    /**
     * 设置重复发送时间，防止这边发送消息后，对方回调还没过来，服务端这边进行了再次发送
     */
    private int MESSAGES_JUST$NOW_TIME = 2 * 1000;

    /**
     * 默认发送消息重试次数
     */
    private int QOS_TRY_COUNT = 1;

    /**
     * 服务的助兴状态 true 在执行 false 不在执行
     */
    private boolean _excuting = false;
    private Timer timer = null;
    private String debugTag = "";

    /**
     * 构造函数
     *
     * @param CHECH_INTERVAL         daemon线程的检查间隔时间
     * @param MESSAGES_JUST$NOW_TIME
     * @param QOS_TRY_COUNT          消息发送的重试次数
     * @param DEBUG                  是否开启debug日志级别
     * @param debugTag               日志标签
     */
    public QoS4SendDaemonRoot(int CHECH_INTERVAL, int MESSAGES_JUST$NOW_TIME, int QOS_TRY_COUNT, boolean DEBUG, String debugTag) {
        if (CHECH_INTERVAL > 0) {
            this.CHECH_INTERVAL = CHECH_INTERVAL;
        }
        if (MESSAGES_JUST$NOW_TIME > 0) {
            this.MESSAGES_JUST$NOW_TIME = MESSAGES_JUST$NOW_TIME;
        }
        if (QOS_TRY_COUNT >= 0) {
            this.QOS_TRY_COUNT = QOS_TRY_COUNT;
        }
        this.DEBUG = DEBUG;
        this.debugTag = debugTag;
    }

    private void doTaskOnece() {
        if (!_excuting) {
            ArrayList<Protocal> lostMessages = new ArrayList<Protocal>();
            _excuting = true;
            try {
                if (DEBUG && sentMessages.size() > 0) {
                    logger.debug("【IMCORE" + this.debugTag + "】【QoS发送方】====== 消息发送质量保证线程运行中, 当前需要处理的列表长度为" + sentMessages.size() + "...");
                }

                //** 遍历HashMap方法二（在大数据量情况下，方法二的性能要5倍优于方法一）
                Iterator<Entry<String, Protocal>> entryIt = sentMessages.entrySet().iterator();
                while (entryIt.hasNext()) {
                    Entry<String, Protocal> entry = entryIt.next();
                    String key = entry.getKey();
                    final Protocal p = entry.getValue();

                    if (p != null && p.isQoS()) {
                        //消息的重试次数超过设置的次数，不再尝试发送
                        if (p.getRetryCount() >= QOS_TRY_COUNT) {
                            if (DEBUG) {
                                logger.debug("【IMCORE" + this.debugTag + "】【QoS发送方】指纹为" + p.getFp()
                                        + "的消息包重传次数已达" + p.getRetryCount() + "(最多" + QOS_TRY_COUNT + "次)上限，将判定为丢包！");
                            }
                            //将对象放入发送失败消息集合中
                            lostMessages.add((Protocal) p.clone());
                            //从待发送集合中删除该对象
                            remove(p.getFp());
                        } else {
                            //### 2015104 Bug Fix: 解决了无线网络延较大时，刚刚发出的消息在其应答包还在途中时被错误地进行重传
                            Long sendMessageTimestamp = sendMessagesTimestamp.get(key);
                            long delta = System.currentTimeMillis() - (sendMessageTimestamp == null ? 0 : sendMessageTimestamp);
                            if (delta <= MESSAGES_JUST$NOW_TIME) {
                                if (DEBUG) {
                                    logger.warn("【IMCORE" + this.debugTag + "】【QoS发送方】指纹为" + key + "的包距\"刚刚\"发出才" + delta + "ms(<=" + MESSAGES_JUST$NOW_TIME + "ms将被认定是\"刚刚\"), 本次不需要重传哦.");
                                }
                            }
                            //### 2015103 Bug Fix END
                            else {
                                MBObserver sendResultObserver = (sendOK, extraObj) -> {
                                    if (sendOK) {
                                        if (DEBUG) {
                                            logger.debug("【IMCORE" + debugTag + "】【QoS发送方】指纹为" + p.getFp() + "的消息包已成功进行重传，此次之后重传次数已达" + p.getRetryCount() + "(最多" + QOS_TRY_COUNT + "次).");
                                        }
                                    } else {
                                        if (DEBUG) {
                                            logger.warn("【IMCORE" + debugTag + "】【QoS发送方】指纹为" + p.getFp() + "的消息包重传失败，它的重传次数之前已累计为" + p.getRetryCount() + "(最多" + QOS_TRY_COUNT + "次).");
                                        }
                                    }
                                };
                                //todo 调用发送机制 此处存在疑问，在什么什么谁后在待发送消息中将发送成功的消息删除掉
                                LocalSendHelper.sendData(p, sendResultObserver);
                                //消息的操作数量加1
                                p.increaseRetryCount();
                            }
                        }
                    } else {
                        remove(key);
                    }
                }
            } catch (Exception eee) {
                if (DEBUG) {
                    logger.warn("【IMCORE" + this.debugTag + "】【QoS发送方】消息发送质量保证线程运行时发生异常," + eee.getMessage(), eee);
                }
            }

            if (lostMessages.size() > 0) {
                notifyMessageLost(lostMessages);
            }

            _excuting = false;
        }
    }

    /**
     * 将未送达信息反馈给消息监听者。
     *
     * @param lostMessages 未发送成功的消息的集合
     */
    protected void notifyMessageLost(ArrayList<Protocal> lostMessages) {
        if (serverLauncher != null && serverLauncher.getServerMessageQoSEventListener() != null) {
            //用户自定义的回调函数，这边调用
            serverLauncher.getServerMessageQoSEventListener().messagesLost(lostMessages);
        }
    }

    /**
     * 设置消息发送线程以后台形式启动
     *
     * @param immediately 定时检查时间
     * @return 当前对象的引用
     */
    public QoS4SendDaemonRoot startup(boolean immediately) {
        stop();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
                                      @Override
                                      public void run() {
                                          doTaskOnece();
                                      }
                                  }
                , immediately ? 0 : CHECH_INTERVAL
                , CHECH_INTERVAL);

        logger.debug("【IMCORE" + this.debugTag + "】【QoS发送方】====== 消息发送质量保证线程已成功启动");

        return this;
    }


    /**
     * 停止当前服务
     */
    public void stop() {
        if (timer != null) {
            try {
                timer.cancel();
            } finally {
                timer = null;
            }
        }
    }

    /**
     * 判断消息发送服务是否在运行
     *
     * @return true 在运行 false 不在运行
     */
    public boolean isRunning() {
        return timer != null;
    }


    /**
     * 判断当前消息是否存在
     *
     * @param fingerPrint 消息的唯一id
     * @return true 存在 false 不存在
     */
    public boolean exist(String fingerPrint) {
        return sentMessages.get(fingerPrint) != null;
    }

    /**
     * 将刚接收到的消息push进入待发送集合中，禁行发送
     *
     * @param p 协议消息
     */
    public void put(Protocal p) {
        if (p == null) {
            if (DEBUG) {
                logger.warn(this.debugTag + "Invalid arg p==null.");
            }
            return;
        }
        if (p.getFp() == null) {
            if (DEBUG) {
                logger.warn(this.debugTag + "Invalid arg p.getFp() == null.");
            }
            return;
        }

        if (!p.isQoS()) {
            if (DEBUG) {
                logger.warn(this.debugTag + "This protocal is not QoS pkg, ignore it!");
            }
            return;
        }

        if (sentMessages.get(p.getFp()) != null) {
            if (DEBUG) {
                logger.warn("【IMCORE" + this.debugTag + "】【QoS发送方】指纹为" + p.getFp() + "的消息已经放入了发送质量保证队列，该消息为何会重复？（生成的指纹码重复？还是重复put？）");
            }
        }

        sentMessages.put(p.getFp(), p);
        sendMessagesTimestamp.put(p.getFp(), System.currentTimeMillis());
    }

    /**
     * 根据消息的唯一id，从待发送消息的集合中删除消息
     *
     * @param fingerPrint 消息的唯一id
     */
    public void remove(final String fingerPrint) {
        try {
            // remove it
            sendMessagesTimestamp.remove(fingerPrint);
            Object result = sentMessages.remove(fingerPrint);
            if (DEBUG) {
                logger.warn("【IMCORE" + this.debugTag + "】【QoS发送方】指纹为" + fingerPrint + "的消息已成功从发送质量保证队列中移除(可能是收到接收方的应答也可能是达到了重传的次数上限)，重试次数="
                        + (result != null ? ((Protocal) result).getRetryCount() : "none呵呵."));
            }
        } catch (Exception e) {
            if (DEBUG) {
                logger.warn("【IMCORE" + this.debugTag + "】【QoS发送方】remove(fingerPrint)时出错了：", e);
            }
        }
    }

    /**
     * 待发送消息集合的大小
     *
     * @return 消息数量
     */
    public int size() {
        return sentMessages.size();
    }

    public void setServerLauncher(ServerLauncher serverLauncher) {
        this.serverLauncher = serverLauncher;
    }

    /**
     * 设置调试状态 true 设置为打出日志 false 不输出日志
     *
     * @param debugable 是否输出日志
     * @return
     */
    public QoS4SendDaemonRoot setDebugable(boolean debugable) {
        this.DEBUG = debugable;
        return this;
    }

    public boolean isDebugable() {
        return this.DEBUG;
    }
}
