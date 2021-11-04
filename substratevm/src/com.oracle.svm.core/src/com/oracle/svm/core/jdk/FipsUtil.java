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

import org.graalvm.compiler.serviceprovider.JavaVersionUtil;

import com.oracle.svm.core.util.VMError;

public class FipsUtil {

    private static volatile Fips INSTANCE;

    public static Fips get() {
        if (INSTANCE == null) {
            INSTANCE = createInstance();
        }
        return INSTANCE;
    }

    private static synchronized Fips createInstance() {
        if (INSTANCE == null) {
            try {
                if (JavaVersionUtil.JAVA_SPEC == 11) {
                    String sharedSecretsClassName = "jdk.internal.misc.SharedSecrets";
                    Class<?> sharedSecrets = Class.forName(sharedSecretsClassName);
                    String javaAccessClassName = "jdk.internal.misc.JavaSecuritySystemConfiguratorAccess";
                    Class<?> javaAccess = Class.forName(javaAccessClassName);
                    return new FipsImpl(11, sharedSecrets, javaAccess);
                } else if (JavaVersionUtil.JAVA_SPEC == 17) {
                    String sharedSecretsClassName = "jdk.internal.access.SharedSecrets";
                    Class<?> sharedSecrets = Class.forName(sharedSecretsClassName);
                    String javaAccessClassName = "jdk.internal.access.JavaSecuritySystemConfiguratorAccess";
                    Class<?> javaAccess = Class.forName(javaAccessClassName);
                    return new FipsImpl(17, sharedSecrets, javaAccess);
                }
            } catch (ClassNotFoundException e) {
                /*
                 * If we build with a non-FIPS capable JDK then we get a CNFE. Specifically on class
                 * JavaSecuritySystemConfiguratorAccess. In that case we are running on a JDK
                 * without the FIPS patches. Return a FIPS=false stub instead in that case.
                 */
                return new Fips() {
                    @Override
                    public boolean isFipsEnabled() {
                        return false;
                    }
                };
            }
            // Unsupported JDK version.
            VMError.unimplemented("Fips support for JDK " + JavaVersionUtil.JAVA_SPEC + " not implemented");
        }
        return INSTANCE;
    }

    public static interface Fips {
        boolean isFipsEnabled();
    }
}
