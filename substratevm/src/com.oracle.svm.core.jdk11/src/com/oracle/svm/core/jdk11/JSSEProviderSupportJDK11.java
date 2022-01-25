package com.oracle.svm.core.jdk11;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Provider;
import java.util.stream.Collectors;

import org.graalvm.compiler.serviceprovider.JavaVersionUtil;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;

import com.oracle.svm.core.SubstrateUtil;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.jdk.FipsEnabled;
import com.oracle.svm.core.jdk.JDK11OrEarlier;
import com.oracle.svm.core.jdk.JDK11OrLater;
import com.oracle.svm.core.jdk.JSSEProviderSupport;
import com.oracle.svm.core.jdk.NssConfig;

public class JSSEProviderSupportJDK11 extends JSSEProviderSupport {

    @Override
    public Provider getFipsJSSEProvider() {
        Provider p = super.getSunPKCSProvider();
        return SubstrateUtil.cast(new Target_com_sun_net_ssl_internal_ssl_Provider(p), Provider.class);
    }

}

@TargetClass(className = "com.sun.net.ssl.internal.ssl.Provider", onlyWith = {JDK11OrEarlier.class})
final class Target_com_sun_net_ssl_internal_ssl_Provider {

    @Alias
    @TargetElement(name = TargetElement.CONSTRUCTOR_NAME)
    native void originalConstructor(Provider p);

    public Target_com_sun_net_ssl_internal_ssl_Provider(Provider p) {
        originalConstructor(p);
    }
}

@AutomaticFeature
final class JSSEProviderFeatureJDK17orLater implements Feature {
    @Override
    public boolean isInConfiguration(IsInConfigurationAccess access) {
        return JavaVersionUtil.JAVA_SPEC == 11;
    }

    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        ImageSingletons.add(JSSEProviderSupport.class, new JSSEProviderSupportJDK11());
    }
}