/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2020 aoju.org and other contributors.                      *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 *                                                                               *
 ********************************************************************************/
package org.aoju.bus.socket.plugins;

import org.aoju.bus.logger.Logger;
import org.aoju.bus.socket.AioSession;
import org.aoju.bus.socket.QuickTimer;
import org.aoju.bus.socket.StateMachine;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * 心跳插件
 *
 * @author Kimi Liu
 * @version 6.1.2
 * @since JDK 1.8+
 */
public abstract class HeartPlugin<T> extends AbstractPlugin<T> {

    private static final TimeoutCallback DEFAULT_TIMEOUT_CALLBACK = (session, lastTime) -> session.close(true);
    private final Map<AioSession, Long> sessionMap = new HashMap<>();
    /**
     * 心跳频率
     */
    private final long heartRate;
    /**
     * 在超时时间内未收到消息,关闭连接。
     */
    private final long timeout;
    private final TimeoutCallback timeoutCallback;

    /**
     * 心跳插件
     *
     * @param heartRate 心跳触发频率
     * @param timeUnit  heatRate单位
     */
    public HeartPlugin(int heartRate, TimeUnit timeUnit) {
        this(heartRate, 0, timeUnit);
    }

    /**
     * 心跳插件
     * <p>
     * 心跳插件在断网场景可能会触发TCP Retransmission,导致无法感知到网络实际状态,可通过设置timeout关闭连接
     * </p>
     *
     * @param heartRate 心跳触发频率
     * @param timeout   消息超时时间
     * @param unit      时间单位
     */
    public HeartPlugin(int heartRate, int timeout, TimeUnit unit) {
        this(heartRate, timeout, unit, DEFAULT_TIMEOUT_CALLBACK);
    }

    /**
     * 心跳插件
     * <p>
     * 心跳插件在断网场景可能会触发TCP Retransmission,导致无法感知到网络实际状态,可通过设置timeout关闭连接
     * </p>
     *
     * @param heartRate       心跳触发频率
     * @param timeout         消息超时时间
     * @param timeUnit        时间单位
     * @param timeoutCallback 超时回调
     */
    public HeartPlugin(int heartRate, int timeout, TimeUnit timeUnit, TimeoutCallback timeoutCallback) {
        if (timeout > 0 && heartRate >= timeout) {
            throw new IllegalArgumentException("heartRate must little then timeout");
        }
        this.heartRate = timeUnit.toMillis(heartRate);
        this.timeout = timeUnit.toMillis(timeout);
        this.timeoutCallback = timeoutCallback;
    }

    @Override
    public final boolean preProcess(AioSession session, T t) {
        sessionMap.put(session, System.currentTimeMillis());
        //是否心跳响应消息
        //延长心跳监测时间
        return !isHeartMessage(session, t);
    }

    @Override
    public final void stateEvent(StateMachine stateMachine, AioSession session, Throwable throwable) {
        switch (stateMachine) {
            case NEW_SESSION:
                sessionMap.put(session, System.currentTimeMillis());
                registerHeart(session, heartRate);
                //注册心跳监测
                break;
            case SESSION_CLOSED:
                //移除心跳监测
                sessionMap.remove(session);
                break;
            default:
                break;
        }
    }

    /**
     * 自定义心跳消息并发送
     *
     * @param session 会话
     * @throws IOException 异常
     */
    public abstract void sendHeartRequest(AioSession session) throws IOException;

    /**
     * 判断当前收到的消息是否为心跳消息。
     * 心跳请求消息与响应消息可能相同，也可能不同，因实际场景而异，故接口定义不做区分。
     *
     * @param session 会话
     * @param msg     消息
     * @return the true/false
     */
    public abstract boolean isHeartMessage(AioSession session, T msg);

    private void registerHeart(final AioSession session, final long heartRate) {
        if (heartRate <= 0) {
            Logger.info("sesssion:{} 因心跳超时时间为:{},终止启动心跳监测任务", session, heartRate);
            return;
        }
        Logger.debug("session:{}注册心跳任务,超时时间:{}", session, heartRate);
        QuickTimer.SCHEDULED_EXECUTOR_SERVICE.schedule(new TimerTask() {
            @Override
            public void run() {
                if (session.isInvalid()) {
                    sessionMap.remove(session);
                    Logger.info("session:{} 已失效，移除心跳任务", session);
                    return;
                }
                Long lastTime = sessionMap.get(session);
                if (lastTime == null) {
                    Logger.warn("session:{} timeout is null", session);
                    lastTime = System.currentTimeMillis();
                    sessionMap.put(session, lastTime);
                }
                long current = System.currentTimeMillis();
                //超时未收到消息，关闭连接
                if (timeout > 0 && (current - lastTime) > timeout) {
                    timeoutCallback.callback(session, lastTime);
                }
                //超时未收到消息,尝试发送心跳消息
                else if (current - lastTime > heartRate) {
                    try {
                        sendHeartRequest(session);
                        session.writeBuffer().flush();
                    } catch (IOException e) {
                        Logger.error("heart exception,will close session:{}", session, e);
                        session.close(true);
                    }
                }
                registerHeart(session, heartRate);
            }
        }, heartRate, TimeUnit.MILLISECONDS);
    }

    public interface TimeoutCallback {
        void callback(AioSession session, long lastTime);
    }

}