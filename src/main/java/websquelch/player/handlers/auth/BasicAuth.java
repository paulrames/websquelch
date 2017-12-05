package websquelch.player.handlers.auth;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HttpHandler;

public class BasicAuth {

	private static final Logger logger = LoggerFactory.getLogger(BasicAuth.class);

	/* Wraps a handler with an HTTP Basic authentication filter */
	public static HttpHandler wrap(HttpHandler handler, String username, String password) {
		handler = new AuthenticationCallHandler(handler);
		handler = new AuthenticationConstraintHandler(handler);
		handler = new AuthenticationMechanismsHandler(handler, createAuthenticationMechanism());
		return new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, createIdentityManager(username, password),
				handler);
	}

	private static List<AuthenticationMechanism> createAuthenticationMechanism() {
		return Collections.singletonList(new BasicAuthenticationMechanism("websquelch"));
	}

	private static IdentityManager createIdentityManager(String username, String password) {
		final AccountImpl account = new AccountImpl(username);
		return new IdentityManager() {
			@Override
			public Account verify(String id, Credential credential) {
				if (credential instanceof PasswordCredential) {
					char[] passwordCredential = ((PasswordCredential) credential).getPassword();
					if (username.equals(id) && Arrays.equals(password.toCharArray(), passwordCredential)) {
						return account;
					}
				}
				logger.info("Authentication failed for user '{}'", id);
				return null;
			}

			@Override
			public Account verify(Credential credential) {
				return null;
			}

			@Override
			public Account verify(Account account) {
				return null;
			}
		};
	}

	@SuppressWarnings("serial")
	private static class AccountImpl implements Account {

		private Principal principal;

		AccountImpl(String name) {
			this.principal = new Principal() {
				@Override
				public String getName() {
					return name;
				}
			};
		}

		@Override
		public Principal getPrincipal() {
			return principal;
		}

		@Override
		public Set<String> getRoles() {
			return Collections.emptySet();
		}

	}

}