package de.mydata.rest.server;

import de.mydata.http.RestRequest;
import org.apache.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.*;

import static java.lang.Math.max;
import static java.lang.String.format;

/**
 * @author Bjoern Frohberg, MyData GmbH
 */
public class JettyRestServer extends Server implements IEmbeddedRestServer {
	
	public static final boolean CHECK_PATH_ON_SERVICE_TYPE = true;
	public static       boolean WARNINGS_ALLOWED           = true;
	public static       boolean DEBUG_OK_ALLOWED           = true;
	
	private static final String DEFAULT_PATH = "/";
	
	private static final String PATH_MATCHER_ANY = DEFAULT_PATH + "*";
	private final int                                   port;
	private final ServletContextHandler                 context;
	private final ServletHolder                         jerseyServlet;
	private final Collection<String>                    typeNames;
	private final Collection<Map.Entry<String, Method>> resourcesFound;
	
	public JettyRestServer(int port, boolean sessions, String... servicePackageNames) {
		super(port);
		this.port = port;
		context = new ServletContextHandler(sessions
		                                    ? ServletContextHandler.SESSIONS
		                                    : ServletContextHandler.NO_SESSIONS);
		// root path
		context.setContextPath(DEFAULT_PATH);
		setHandler(context);
		
		jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, PATH_MATCHER_ANY);
		jerseyServlet.setInitOrder(0);
		
		// Tells the Jersey Servlet which REST service/class to load.
		typeNames = new ArrayList<>();
		resourcesFound = new ArrayList<>();
		for (String servicePackageName : servicePackageNames) {
			Collection<Class<IRestService>> services = ClassUtils.findClassesOfType(IRestService.class, servicePackageName);
			for (Class<IRestService> service : services) {
				if(service.isAnnotationPresent(Deprecated.class)) {
					System.out.println(format("[WARN] Service '%s' skipped cause of %s-annotation", service.getCanonicalName(), Deprecated.class.getCanonicalName()));
					continue;
				}
				validateServicePathAnnotationOrDie(service);
				resourcesFound.addAll(warnNoMethods(service));
				typeNames.add(service.getCanonicalName());
			}
		}
		String restServices = StringUtils.collectionToCommaDelimitedString(typeNames);
		jerseyServlet.setInitParameter("jersey.config.server.provider.classnames", restServices);
	}
	
	public void enableSSL(String keyStorePw, String keyManagerPw) throws FileNotFoundException {
		enableSSL(keyStorePw, keyManagerPw, new File(System.getProperty("user.home"), "keystore"));
	}
	
	public void enableSSL(String keyStorePw, String keyManagerPw, File keystoreFile) throws FileNotFoundException {
		if(!keystoreFile.exists()) {
			throw new FileNotFoundException(keystoreFile.getPath());
		}
		
		enableSSL(keystoreFile, keyStorePw, keyManagerPw);
	}
	
	public static final class SSLConfigFactory {
		
		private final Server      server;
		private final File        keyStoreFile;
		private       int         httpsIdleTimeout;
		private       HttpVersion httpVersion;
		private       String      sslCertificateAlias;
		private       int         confidentialPort;
		private       String      keyStorePw;
		private       String      keyManagerPw;
		private       String      hyperTerminalTransferProtocolSSL;
		
		private SSLConfigFactory(Server server, File keyStoreFile) {
			this.server = server;
			this.keyStoreFile = keyStoreFile;
			confidentialPort = 443;
			hyperTerminalTransferProtocolSSL = "https";
			keyStorePw = "00000";
			keyManagerPw = "00000";
			sslCertificateAlias = "jetty";
			httpVersion = HttpVersion.HTTP_1_1;
			httpsIdleTimeout = 500000;
		}
		
		public SSLConfigFactory useDefaultHttpsTimeout() {
			httpsIdleTimeout = 500000;
			return this;
		}
		
		public SSLConfigFactory setHttpsTimeout(int millis) {
			this.httpsIdleTimeout = millis;
			return this;
		}
		
		public SSLConfigFactory useDefaultHTTPVersion() {
			httpVersion = HttpVersion.HTTP_1_1;
			return this;
		}
		
		public SSLConfigFactory setHttpVersion(HttpVersion version) {
			this.httpVersion = version;
			return this;
		}
		
		public SSLConfigFactory useDefaultSSLCertAlias() {
			sslCertificateAlias = "jetty";
			return this;
		}
		
		public SSLConfigFactory setSSLCertificateAlias(String aliasName) {
			this.sslCertificateAlias = aliasName;
			return this;
		}
		
		public SSLConfigFactory useDefaultSSLScheme() {
			hyperTerminalTransferProtocolSSL = "https";
			return this;
		}
		
		public SSLConfigFactory setSSLProtocol(String httProtocol) {
			this.hyperTerminalTransferProtocolSSL = httProtocol;
			return this;
		}
		
		public SSLConfigFactory useDefaultSSLPort() {
			confidentialPort = 443;
			return this;
		}
		
		public SSLConfigFactory setConfidentialPort(int port) {
			this.confidentialPort = port;
			return this;
		}
		
		public SSLConfigFactory setKeyStorePassword(String keyStorePw) {
			this.keyStorePw = keyStorePw == null
			                  ? ""
			                  : keyStorePw;
			if(keyStorePw == null) {
				System.out.println("[WARN] - no keyStorePw in SSLConfig");
			}
			return this;
		}
		
		public SSLConfigFactory setKeyManagerPassword(String keyManagerPw) {
			this.keyManagerPw = keyManagerPw == null
			                    ? ""
			                    : keyManagerPw;
			;
			if(keyManagerPw == null) {
				System.out.println("[WARN] - no keyManagerPw in SSLConfig");
			}
			return this;
		}
		
		public void commit() {
			HttpConfiguration http_config = new HttpConfiguration();
			http_config.setSecureScheme(hyperTerminalTransferProtocolSSL);
			http_config.setSecurePort(confidentialPort);
			
			SslContextFactory sslContextFactory = new SslContextFactory();
			sslContextFactory.setKeyStorePath(keyStoreFile.getAbsolutePath());
			
			//Set correct password here for deployed system.
			sslContextFactory.setKeyStorePassword(keyStorePw);
			
			//Set correct password here for deployed system.
			sslContextFactory.setKeyManagerPassword(keyManagerPw);
			
			sslContextFactory.setCertAlias(sslCertificateAlias);
			
			
			HttpConfiguration https_config = new HttpConfiguration(http_config);
			https_config.addCustomizer(new SecureRequestCustomizer());
			ServerConnector https = new ServerConnector(server,
			                                            new SslConnectionFactory(sslContextFactory, httpVersion.toString()),
			                                            new HttpConnectionFactory(https_config));
			https.setPort(confidentialPort);
			https.setIdleTimeout(httpsIdleTimeout);
			
			server.setConnectors(combineConnectors(http_config, https));
			System.out.println("[OK] - You can access your url using ssl: " + hyperTerminalTransferProtocolSSL + ":" + confidentialPort + "\\ ..");
		}
		
		private Connector[] combineConnectors(HttpConfiguration http_config, ServerConnector https) {
			Connector[] connectors = server.getConnectors();
			if(connectors == null) {
				connectors = new Connector[2];
			}
			Arrays.copyOf(connectors, max(2, connectors.length + 1));
			connectors[connectors.length - 1] = https;
			if(connectors.length == 2 && connectors[0] == null) {
				ServerConnector tempHttpConnector = new ServerConnector(server);
				tempHttpConnector.addConnectionFactory(new HttpConnectionFactory(http_config));
				tempHttpConnector.setPort(server.getURI().getPort());
				connectors[0] = tempHttpConnector;
			}
			return connectors;
		}
	}
	
	public SSLConfigFactory enableSSL(File keystoreFile, String keyStorePw, String keyManagerPw) {
		return new SSLConfigFactory(this, keystoreFile)
				.setKeyStorePassword(keyStorePw)
				.setKeyManagerPassword(keyManagerPw);
	}
	
	public Collection<Map.Entry<String, Method>> getResourcesFound() {
		return resourcesFound;
	}
	
	public ServletContextHandler getContext() {
		return context;
	}
	
	public ServletHolder getServletHolder() {
		return jerseyServlet;
	}
	
	@Override
	public void execute() throws Exception {
		final String pingTestUrl = resourcesFound.stream().filter(url -> {
			if(Boolean.TYPE.equals(url.getValue().getReturnType())) {
				if(url.getValue().getParameterTypes().length == 0) {
					if(url.getValue().getName().equals("ping")) {
						return true;
					}
				}
			}
			return false;
		}).map(Map.Entry::getKey).findFirst().orElse(null);
		
		if(pingTestUrl != null) {
			pingTest(pingTestUrl);
		}
		
		start();
		try {
			join();
		} finally {
			destroy(); /* 'Gracefull' shutdown after exception*/
		}
	}
	
	private static void validateServicePathAnnotationOrDie(Class<IRestService> service) {
		if(CHECK_PATH_ON_SERVICE_TYPE && !service.isAnnotationPresent(Path.class)) {
			throw new IllegalArgumentException(format("Your rest service class '%s' must have a %s-Annotation!",
			                                          service.getCanonicalName(),
			                                          Path.class.getCanonicalName()));
		}
	}
	
	private static Collection<Map.Entry<String, Method>> warnNoMethods(Class<IRestService> service) {
		boolean                               ok             = false;
		Collection<Map.Entry<String, Method>> foundResources = new ArrayList<>();
		for (Method method : service.getDeclaredMethods()) {
			if(method.isAnnotationPresent(Path.class)) {
				if(!ok) {
					ok = true;
				}
				
				boolean warn = false;
				
				boolean getOK    = method.isAnnotationPresent(GET.class);
				boolean putOK    = method.isAnnotationPresent(PUT.class);
				boolean postOK   = method.isAnnotationPresent(POST.class);
				boolean deleteOK = method.isAnnotationPresent(DELETE.class);
				if(!getOK && !putOK && !postOK && !deleteOK) {
					warn = true;
					if(WARNINGS_ALLOWED) {
						System.err.println(format("Your rest method resource must have at least one of these annotations (GET,PUT,POST or DELETE)! in %s method '%s'",
						                          service.getCanonicalName(),
						                          method.getName()));
					}
				}
				
				if(!method.isAnnotationPresent(Produces.class) && !method.getReturnType().equals(Void.TYPE)) {
					warn = true;
					if(WARNINGS_ALLOWED) {
						System.err.println(format("Your rest method resource returns a %s value. Your method must have a %s-annotation! in %s method '%s'",
						                          method.getReturnType().getCanonicalName(),
						                          Produces.class.getCanonicalName(),
						                          service.getCanonicalName(),
						                          method.getName()));
					}
				}
				
				if(!warn && DEBUG_OK_ALLOWED) {
					String url = format("http://%s:%s%s%s", "%s", "%d",
					                    service.getAnnotation(Path.class).value(),
					                    method.getAnnotation(Path.class).value());
					foundResources.add(new AbstractMap.SimpleEntry<>(url, method));
					System.out.println(format("OK - Rest resource accessible %s method '%s' using http://[host:port]/%s%s",
					                          service.getCanonicalName(), method.getName(),
					                          service.getAnnotation(Path.class).value(),
					                          method.getAnnotation(Path.class).value()));
				}
			}
		}
		
		if(!ok) {
			if(DEBUG_OK_ALLOWED) {
				System.err.println(format("Your rest resource does not contain a compatible public method haven a %s-annotation! in %s",
				                          Path.class.getCanonicalName(),
				                          service.getCanonicalName()));
			}
		}
		
		return foundResources;
	}
	
	private void pingTest(final String pingTestUrl) {
		addLifeCycleListener(new AbstractLifeCycleListener() {
			@Override
			public void lifeCycleStarted(LifeCycle event) {
				String host = JettyRestServer.this.getServer().getURI().getHost();
				int    port = JettyRestServer.this.port;
				
				String  url    = format(pingTestUrl, host, port);
				String  result = RestRequest.getRequestText(RestRequest.GET.request(url, MediaType.TEXT_PLAIN_TYPE));
				boolean ping   = RestRequest.StringConverter.toBoolean(result);
				
				if(!ping) {
					System.err.println("[FAIL] - PING was not successful!");
				} else {
					System.out.println("[OK] - PING was successful!");
				}
			}
		});
	}
	
	@SuppressWarnings("WeakerAccess")
	public static class ClassUtils {
		
		protected ClassUtils() {
		}
		
		public static <T> Collection<Class<T>> findClassesOfType(Class<T> implType, String packageName) {
			try {
				return new ComponentClassScanner().getComponentClasses(packageName, implType);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		
		public static class ComponentClassScanner extends ClassPathScanningCandidateComponentProvider {
			
			public ComponentClassScanner() {
				super(false);
			}
			
			@SuppressWarnings("unchecked")
			public final <T> Collection<Class<T>> getComponentClasses(String basePackage, Class<T> implType) throws ClassNotFoundException {
				List<Class<T>> classList = new ArrayList<>();
				addIncludeFilter(new AssignableTypeFilter(implType));
				Collection<BeanDefinition> definitions = findCandidateComponents(basePackage);
				for (BeanDefinition candidate : definitions) {
					classList.add((Class<T>) Class.forName(candidate.getBeanClassName()));
				}
				return classList;
			}
		}
		
	}
	
	/**
	 * Eine Restschnittstelle mit {@link Path} und public Methoden hat, die ebenfalls {@link Path} und {@link GET}, {@link PUT}, {@link DELETE} oder {@link POST} aufweisen
	 * Die {@link Path}-Annotations etc. sollten am Interface und an der implementierenden Klasse (Service genannt) gleichermaßen angebracht werden und gleichgehalten werden!
	 * Am wichtigsten ist jedoch, dass die {@link Path}-Annotation etc. an der konkreten Implementierung (Service) angebracht sind.
	 * Nur, wenn das Interface des Service als Schnittstelle für einen Client dienen soll, sollte das Interface des Service und dessen Methoden ebenfalls die Annoationen aufweisen (identisch).
	 * <p/>
	 * <p>You can use {@link Deprecated} as Annotation to skip usage of a service with this interface {@link IRestService}</p>.
	 */
	public interface IRestService {
		
		boolean ping();
	}
}
