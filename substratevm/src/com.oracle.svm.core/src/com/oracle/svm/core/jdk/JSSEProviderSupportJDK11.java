package com.oracle.svm.core.jdk;

import java.security.Provider;

import org.graalvm.compiler.serviceprovider.JavaVersionUtil;
import org.graalvm.nativeimage.ImageSingletons;

import com.oracle.svm.core.SubstrateUtil;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;
import com.oracle.svm.core.feature.AutomaticallyRegisteredFeature;
import com.oracle.svm.core.feature.InternalFeature;

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

@AutomaticallyRegisteredFeature
final class JSSEProviderFeatureJDK17orLater implements InternalFeature {
    @Override
    public boolean isInConfiguration(IsInConfigurationAccess access) {
        return JavaVersionUtil.JAVA_SPEC == 11;
    }

    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        ImageSingletons.add(JSSEProviderSupport.class, new JSSEProviderSupportJDK11());
    }
}
