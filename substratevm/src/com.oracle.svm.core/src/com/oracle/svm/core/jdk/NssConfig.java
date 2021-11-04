/*
 * Copyright (c) 2021, Red Hat, Inc.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.svm.core.jdk;

public final class NssConfig {

    public static final String SunPKCS_NSS_FIPS_NAME = "SunPKCS11-NSS-FIPS";
    public static final String CONFIG_FILE_NAME = "nss.fips.cfg";
    public static final boolean IS_FIPS = FipsUtil.get().isFipsEnabled();

    /**
     * Find the nss config file. Using the following lookup: 1. Try to use {@code nss.fips.cfg}
     * property. If set, use that. 2. Try {@code $java.home/conf/security/nss.fips.cfg} 3. Try
     * /usr/lib/jvm/java-11-openjdk/conf/security/nss.fips.cfg
     *
     * @return The path of the nss fips config file. Never null.
     */
    public static String get() {
        String filePath = System.getProperty("nss.fips.cfg");
        if (filePath != null) {
            return filePath;
        }
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            return javaHome + "/conf/security/nss.fips.cfg";
        }
        // hard-coded default
        return "/usr/lib/jvm/java-11-openjdk/conf/security/nss.fips.cfg";
    }
}