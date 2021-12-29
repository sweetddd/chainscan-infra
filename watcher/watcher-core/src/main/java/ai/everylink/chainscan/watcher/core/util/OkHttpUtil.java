package ai.everylink.chainscan.watcher.core.util;

import okhttp3.OkHttpClient;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * OKHttp实用类
 *
 * @author david.zhang@everylink.ai
 * @since 2021-12-27
 */
public final class OkHttpUtil {
    private OkHttpUtil(){}

    private static final Long CONNECTION_TIMEOUT = 30 * 1000L;
    private static final Long WRITE_TIMEOUT = 30 * 1000L;
    private static final Long READ_TIMEOUT = 30 * 1000L;

    public static OkHttpClient buildOkHttpClient() {
        return buildOkHttpClient(CONNECTION_TIMEOUT, WRITE_TIMEOUT, READ_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static OkHttpClient buildOkHttpClient(Long connectionTimeout) {
        return buildOkHttpClient(connectionTimeout, WRITE_TIMEOUT, READ_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static OkHttpClient buildOkHttpClient(Long connectionTimeout, Long writeTimeout, Long readTimeout) {
        return buildOkHttpClient(connectionTimeout, writeTimeout, readTimeout, TimeUnit.MILLISECONDS);
    }

    public static OkHttpClient buildOkHttpClient(
            Long connectionTimeout, Long writeTimeout, Long readTimeout, TimeUnit timeUnit) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(connectionTimeout, timeUnit);
        builder.writeTimeout(writeTimeout, timeUnit);
        builder.readTimeout(readTimeout, timeUnit);
        OkHttpClient httpClient = builder
                .sslSocketFactory(createSSLSocketFactory(), new TrustAllCerts())
                .hostnameVerifier(new TrustAllHostnameVerifier())
                .build();

        return httpClient;
    }

    /**
     * HTTPS  begin
     */
    private static SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory ssfFactory = null;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new TrustAllCerts()}, new SecureRandom());
            ssfFactory = sc.getSocketFactory();
        } catch (Exception e) {
        }

        return ssfFactory;
    }

    static class TrustAllHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;//trust All的写法就是在这里写的，直接无视hostName，直接返回true，表示信任所有主机
        }
    }

    static class TrustAllCerts implements X509TrustManager {

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
