package com.oracle.svm.core.jdk17;

import java.security.Provider;

import org.graalvm.compiler.serviceprovider.JavaVersionUtil;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;

import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.jdk.JSSEProviderSupport;

public class JSSEProviderSupportJDK17orLater extends JSSEProviderSupport {

    @Override
    public Provider getFipsJSSEProvider() {
        return getSunJSSEProvider();
    }

}

@AutomaticFeature
final class JSSEProviderFeatureJDK17orLater implements Feature {
    @Override
    public boolean isInConfiguration(IsInConfigurationAccess access) {
        return JavaVersionUtil.JAVA_SPEC >= 17;
    }

    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        ImageSingletons.add(JSSEProviderSupport.class, new JSSEProviderSupportJDK17orLater());
    }
}