package com.oracle.svm.core.jdk17;

import java.security.Provider;

import org.graalvm.compiler.serviceprovider.JavaVersionUtil;
import org.graalvm.nativeimage.ImageSingletons;

import com.oracle.svm.core.feature.AutomaticallyRegisteredFeature;
import com.oracle.svm.core.feature.InternalFeature;
import com.oracle.svm.core.jdk.JSSEProviderSupport;

public class JSSEProviderSupportJDK17orLater extends JSSEProviderSupport {

    @Override
    public Provider getFipsJSSEProvider() {
        return getSunJSSEProvider();
    }

}

@AutomaticallyRegisteredFeature
final class JSSEProviderFeatureJDK17orLater implements InternalFeature {
    @Override
    public boolean isInConfiguration(IsInConfigurationAccess access) {
        return JavaVersionUtil.JAVA_SPEC >= 17;
    }

    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        ImageSingletons.add(JSSEProviderSupport.class, new JSSEProviderSupportJDK17orLater());
    }
}