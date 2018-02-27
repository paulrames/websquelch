package websquelch.player.handlers.ssl;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SSLContextFactory {

	private SSLContextFactory() { }

	public static SSLContext createSSLContext(String keystoreDir, String keystorePassword, String keyPassword)
			throws Exception {
		final KeyStore keyStore = KeyStore.getInstance("JKS");
		Path keystore = Paths.get(keystoreDir);
		try (final InputStream in = Files.newInputStream(keystore)) {
			keyStore.load(in, keystorePassword.toCharArray());
		}
		final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keyStore, keyPassword.toCharArray());
		final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(keyStore);
		final SSLContext sc = SSLContext.getInstance("TLS");
		sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());
		return sc;
	}

}
