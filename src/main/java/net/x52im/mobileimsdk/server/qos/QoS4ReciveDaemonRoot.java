package net.x52im.mobileimsdk.server.qos;

import net.x52im.mobileimsdk.server.protocal.Protocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map.Entry;
import java.util.concurrent.*;

public class QoS4ReciveDaemonRoot {

    private static Logger logger = LoggerFactory.getLogger(QoS4ReciveDaemonRoot.class);

    private boolean DEBUG = false;

    /**
     * 定时检查时间5分钟
     */
    private int CHECH_INTERVAL = 5 * 60 * 1000;

    /**
     * 10分钟
     */
    private int MESSAGES_VALID_TIME = 10 * 60 * 1000;

    /**
     *
     */
    private ConcurrentMap<String, Long> recievedMessages = new ConcurrentHashMap<String, Long>();

    /**
     * 定时器
     */
    private ScheduledExecutorService scheduledExecutorService;


    private Runnable runnable = null;

    /**
     * 是否在执行
     */
    private boolean excuting = false;

    /**
     * debug 标记
     */
    private String debugTag = "";

    /**
     * 构造函数
     *
     * @param CHECH_INTERVAL
     * @param MESSAGES_VALID_TIME
     * @param DEBUG
     * @param debugTag
     */
    public QoS4ReciveDaemonRoot(int CHECH_INTERVAL, int MESSAGES_VALID_TIME, boolean DEBUG, String debugTag) {
        if (CHECH_INTERVAL > 0) {
            this.CHECH_INTERVAL = CHECH_INTERVAL;
        }
        if (MESSAGES_VALID_TIME > 0) {
            this.MESSAGES_VALID_TIME = MESSAGES_VALID_TIME;
        }
        this.DEBUG = DEBUG;
        this.debugTag = debugTag;
    }

    /**
     * 执行任务
     */
    private void doTaskOnece() {
        if (!excuting) {
            excuting = true;

            if (DEBUG) {
                logger.debug("【IMCORE" + this.debugTag + "】【QoS接收方】+++++ START 暂存处理线程正在运行中，当前长度" + recievedMessages.size() + ".");
            }

            //** 遍历HashMap方法二（在大数据量情况下，方法二的性能要5倍优于方法一）
            for (Entry<String, Long> entry : recievedMessages.entrySet()) {
                String key = entry.getKey();
                long value = entry.getValue();

                long delta = System.currentTimeMillis() - value;
                if (delta >= MESSAGES_VALID_TIME) {
                    if (DEBUG) {
                        logger.debug("【IMCORE" + this.debugTag + "】【QoS接收方】指纹为" + key + "的包已生存" + delta + "ms(最大允许" + MESSAGES_VALID_TIME + "ms), 马上将删除之.");
                    }
                    recievedMessages.remove(key);
                }
            }
        }

        if (DEBUG) {
            logger.debug("【IMCORE" + this.debugTag + "】【QoS接收方】+++++ END 暂存处理线程正在运行中，当前长度" + recievedMessages.size() + ".");
        }

        //
        excuting = false;
    }

    public void startup() {
        stop();
        if (recievedMessages != null && recievedMessages.size() > 0) {
            for (String key : recievedMessages.keySet()) {
                putImpl(key);
            }
        }
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(this::doTaskOnece, CHECH_INTERVAL, CHECH_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (null != scheduledExecutorService) {
            try {
                scheduledExecutorService.shutdown();
            } finally {
                scheduledExecutorService = null;
            }
        }
    }

    public boolean isRunning() {
        return scheduledExecutorService != null;
    }

    public void addRecieved(Protocal p) {
        if (p != null && p.isQoS()) {
            addRecieved(p.getFp());
        }
    }

    public void addRecieved(String fingerPrintOfProtocal) {
        if (fingerPrintOfProtocal == null) {
            logger.debug("【IMCORE" + this.debugTag + "】无效的 fingerPrintOfProtocal==null!");
            return;
        }

        if (recievedMessages.containsKey(fingerPrintOfProtocal)) {
            logger.debug("【IMCORE" + this.debugTag + "】【QoS接收方】指纹为" + fingerPrintOfProtocal
                    + "的消息已经存在于接收列表中，该消息重复了（原理可能是对方因未收到应答包而错误重传导致），更新收到时间戳哦.");
        }

        putImpl(fingerPrintOfProtocal);
    }

    private void putImpl(String fingerPrintOfProtocal) {
        if (fingerPrintOfProtocal != null) {
            recievedMessages.put(fingerPrintOfProtocal, System.currentTimeMillis());
        }
    }

    public boolean hasRecieved(String fingerPrintOfProtocal) {
        return recievedMessages.containsKey(fingerPrintOfProtocal);
    }

    public int size() {
        return recievedMessages.size();
    }

    public QoS4ReciveDaemonRoot setDebugable(boolean debugable) {
        this.DEBUG = debugable;
        return this;
    }

    public boolean isDebugable() {
        return this.DEBUG;
    }
}
