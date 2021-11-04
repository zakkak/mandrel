package com.oracle.svm.core.jdk;

import java.lang.reflect.Method;
import java.security.Security;

import com.oracle.svm.core.jdk.FipsUtil.Fips;

public class FipsImpl implements Fips {

    private final int jdkVersion;
    @SuppressWarnings("rawtypes")//
    private final Class sharedSecrets;
    @SuppressWarnings("rawtypes")//
    private final Class javaAccessClass;
    private static final String IS_ENABLED_METHOD_NAME = "isSystemFipsEnabled";
    private static final String CONFIG_ACCESS_METHOD_NAME = "getJavaSecuritySystemConfiguratorAccess";

    FipsImpl(int jdkVersion, Class<?> sharedSecrets, Class<?> javaAccess) {
        this.jdkVersion = jdkVersion;
        this.sharedSecrets = sharedSecrets;
        this.javaAccessClass = javaAccess;
    }

    @Override
    public boolean isFipsEnabled() {
        /*
         * work around for: https://bugzilla.redhat.com/show_bug.cgi?id=2021263
         */
        Security.getProperty("jdk.tls.disabledAlgorithms");

        try {
            @SuppressWarnings("unchecked")
            Method getJavaSecSysConfigAccess = sharedSecrets.getDeclaredMethod(CONFIG_ACCESS_METHOD_NAME);
            Object javaAccessObj = getJavaSecSysConfigAccess.invoke(null);
            @SuppressWarnings("unchecked")
            Method isSysFipsEnabledMethod = javaAccessClass.getDeclaredMethod(IS_ENABLED_METHOD_NAME);
            return (boolean) isSysFipsEnabledMethod.invoke(javaAccessObj);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Error establishing FIPS status (JDK " + jdkVersion + ")", e);
        }
    }

}
