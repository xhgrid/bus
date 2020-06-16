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
 ********************************************************************************/
package org.aoju.bus.socket.netty;

/**
 * @author Kimi Liu
 * @version 6.0.0
 * @since JDK 1.8+
 */
public class RequestHandler {

    public static void execute(SocketRequest request) {
        if (request.getEvent() != null) {
            if (NettyConsts.SUBSCRIBE.equals(request.getEvent())) {
                CommandExecutor.execute(new SubscribeCommand(request));
            } else if (NettyConsts.HEARTBEAT.equals(request.getEvent())) {
                CommandExecutor.execute(new HeartbeatCommand(request));
            } else if (NettyConsts.CANCEL.equals(request.getEvent())) {
                CommandExecutor.execute(new CancelCommand(request));
            } else if (NettyConsts.MESSAGE.equals(request.getEvent())) {
                CommandExecutor.execute(new MessageCommand(request));
            }
        }
    }

}
