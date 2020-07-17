/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2020 aoju.org OSHI and other contributors.                 *
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
package org.aoju.bus.health.unix.aix.software;

import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibCAPI;
import org.aoju.bus.core.annotation.ThreadSafe;
import org.aoju.bus.core.lang.Normal;
import org.aoju.bus.core.lang.RegEx;
import org.aoju.bus.health.Executor;
import org.aoju.bus.health.builtin.software.AbstractNetworkParams;
import org.aoju.bus.health.unix.aix.AixLibc;

/**
 * AixNetworkParams class.
 *
 * @author Kimi Liu
 * @version 6.0.3
 * @since JDK 1.8+
 */
@ThreadSafe
final class AixNetworkParams extends AbstractNetworkParams {

    private static final AixLibc LIBC = AixLibc.INSTANCE;

    private static String getDefaultGateway(String netstat) {
        /*-
        $ netstat -rnf inet
        Routing tables
        Destination        Gateway           Flags   Refs     Use  If   Exp  Groups
        *
        Route Tree for Protocol Family 2 (Internet):
        default            192.168.10.1      UG        9    873816 en0      -      -
        127/8              127.0.0.1         U         9    839480 lo0      -      -
        192.168.10.0       192.168.10.80     UHSb      0         0 en0      -      -   =>
        192.168.10/24      192.168.10.80     U         3    394820 en0      -      -
        192.168.10.80      127.0.0.1         UGHS      0         7 lo0      -      -
        192.168.10.255     192.168.10.80     UHSb      2      7466 en0      -      -
        */
        for (String line : Executor.runNative(netstat)) {
            String[] split = RegEx.SPACES.split(line);
            if (split.length > 7 && "default".equals(split[0])) {
                return split[1];
            }
        }
        return Normal.UNKNOWN;
    }

    @Override
    public String getHostName() {
        byte[] hostnameBuffer = new byte[LibCAPI.HOST_NAME_MAX + 1];
        if (0 != LIBC.gethostname(hostnameBuffer, hostnameBuffer.length)) {
            return super.getHostName();
        }
        return Native.toString(hostnameBuffer);
    }

    @Override
    public String getIpv4DefaultGateway() {
        return getDefaultGateway("netstat -rnf inet");
    }

    @Override
    public String getIpv6DefaultGateway() {
        return getDefaultGateway("netstat -rnf inet6");
    }

}
