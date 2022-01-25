package com.oracle.svm.core.jdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Provider;
import java.util.stream.Collectors;

import org.graalvm.nativeimage.ImageSingletons;

import com.oracle.svm.core.SubstrateUtil;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;

/**
 * Abstracts the information about the JSSE security provider support, which is different between
 * JDK 11 and JDK 17.
 */
public abstract class JSSEProviderSupport {

    public static JSSEProviderSupport singleton() {
        return ImageSingletons.lookup(JSSEProviderSupport.class);
    }

    public abstract Provider getFipsJSSEProvider();

    public Provider getSunPKCSProvider() {
        return SunPKCS11Holder.getInstance();
    }

    public Provider getSunJSSEProvider() {
        return SubstrateUtil.cast(new Target_sun_security_ssl_SunJSSE(), Provider.class);
    }
}

@TargetClass(className = "sun.security.pkcs11.SunPKCS11", onlyWith = {JDK11OrLater.class, FipsEnabled.class})
@SuppressWarnings({"unused"})
final class Target_sun_security_pkcs11_SunPKCS11_FIPS {

    @Alias //
    @RecomputeFieldValue(kind = Kind.Reset) //
    Target_sun_security_pkcs11_wrapper_PKCS11 p11;

    @Alias //
    @TargetElement(name = TargetElement.CONSTRUCTOR_NAME)
    native void originalConstructor();

    @Alias //
    public Target_sun_security_pkcs11_SunPKCS11_FIPS() {
        originalConstructor();
    }

    @Alias //
    public native Provider configure(String c);
}

@TargetClass(className = "sun.security.pkcs11.Token", onlyWith = {JDK11OrLater.class, FipsEnabled.class})
@SuppressWarnings({"unused"})
final class Target_sun_security_pkcs11_Token {

    @Alias//
    @RecomputeFieldValue(kind = Kind.Reset) //
    Target_sun_security_pkcs11_SunPKCS11_FIPS provider;

    @Alias //
    @RecomputeFieldValue(kind = Kind.Reset) //
    Target_sun_security_pkcs11_wrapper_PKCS11 p11;

}

@TargetClass(className = "sun.security.pkcs11.wrapper.PKCS11", onlyWith = {JDK11OrLater.class, FipsEnabled.class})
final class Target_sun_security_pkcs11_wrapper_PKCS11 {
}

/**
 * Holder for code being run at image runtime. Holds the SunPKCS provider instance.
 */
final class SunPKCS11Holder {
    private static volatile Provider INSTANCE;

    static Provider getInstance() {
        if (INSTANCE == null) {
            INSTANCE = createOnce();
        }
        return INSTANCE;
    }

    private static synchronized Provider createOnce() {
        if (INSTANCE == null) {
            String config = NssConfigRetriever.getInlineConfig();
            Target_sun_security_pkcs11_SunPKCS11_FIPS sunPKCS11 = new Target_sun_security_pkcs11_SunPKCS11_FIPS();
            // SunPKCS11's configure() method calls constructor SunPKCS11(Config), which performs
            // initialization of Secmod
            INSTANCE = sunPKCS11.configure(config);
        }
        return INSTANCE;
    }
}

final class NssConfigRetriever {

    static String getInlineConfig() {
        String file = "/" + NssConfig.CONFIG_FILE_NAME;
        try (InputStream in = SunPKCS11ProviderAccessors.class.getResourceAsStream(file);
                        BufferedReader bufIn = new BufferedReader(new InputStreamReader(in))) {
            return "--" + bufIn.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve " + file + " from image heap");
        }
    }
}
