package reactor.web.http.server;

import reactor.fn.Function;
import reactor.fn.selector.UriTemplateSelector;
import reactor.web.http.HttpMethod;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Jon Brisbin
 */
public class ServerHttpRequestSelector extends UriTemplateSelector {

	private Set<Function<ServerHttpRequest<?>, Boolean>> conditions = new HashSet<Function<ServerHttpRequest<?>, Boolean>>();

	public ServerHttpRequestSelector(String uri) {
		super(uri);
	}

	public static ServerHttpRequestSelector delete(String uri) {
		return new ServerHttpRequestSelector(uri).withMethod(HttpMethod.DELETE);
	}

	public static ServerHttpRequestSelector get(String uri) {
		return new ServerHttpRequestSelector(uri).withMethod(HttpMethod.GET);
	}

	public static ServerHttpRequestSelector post(String uri) {
		return new ServerHttpRequestSelector(uri).withMethod(HttpMethod.POST);
	}

	public static ServerHttpRequestSelector put(String uri) {
		return new ServerHttpRequestSelector(uri).withMethod(HttpMethod.PUT);
	}

	public static ServerHttpRequestSelector patch(String uri) {
		return new ServerHttpRequestSelector(uri).withMethod(HttpMethod.PATCH);
	}

	public static ServerHttpRequestSelector options(String uri) {
		return new ServerHttpRequestSelector(uri).withMethod(HttpMethod.OPTIONS);
	}

	public static ServerHttpRequestSelector head(String uri) {
		return new ServerHttpRequestSelector(uri).withMethod(HttpMethod.HEAD);
	}

	public static ServerHttpRequestSelector trace(String uri) {
		return new ServerHttpRequestSelector(uri).withMethod(HttpMethod.TRACE);
	}

	public ServerHttpRequestSelector withMethod(HttpMethod method) {
		conditions.add(new HttpMethodCondition(method));
		return this;
	}

	public ServerHttpRequestSelector withHost(String host) {
		conditions.add(new HttpHostCondition(host));
		return this;
	}

	public ServerHttpRequestSelector withScheme(String scheme) {
		conditions.add(new HttpSchemeCondition(scheme));
		return this;
	}

	public ServerHttpRequestSelector withHeaderExists(String name) {
		conditions.add(new HeaderExistsCondition(name));
		return this;
	}

	public ServerHttpRequestSelector withQueryParamExists(String name) {
		conditions.add(new QueryParameterExistsCondition(name));
		return this;
	}

	public ServerHttpRequestSelector withCondition(Function<ServerHttpRequest<?>, Boolean> condition) {
		conditions.add(condition);
		return this;
	}

	@Override
	public boolean matches(Object key) {
		if (!ServerHttpRequest.class.isInstance(key)) {
			return false;
		}

		ServerHttpRequest<?> request = (ServerHttpRequest<?>) key;
		boolean match = super.matches(request.getURI().getPath());
		if (!match) {
			return match;
		}

		if (!conditions.isEmpty()) {
			for (Function<ServerHttpRequest<?>, Boolean> condition : conditions) {
				if (!condition.apply(request)) {
					return false;
				}
			}
		}

		return match;
	}

	private class HttpMethodCondition implements Function<ServerHttpRequest<?>, Boolean> {
		private final HttpMethod method;

		private HttpMethodCondition(HttpMethod method) {
			this.method = method;
		}

		@Override
		public Boolean apply(ServerHttpRequest<?> request) {
			return method.equals(request.getMethod());
		}
	}

	private class HttpSchemeCondition implements Function<ServerHttpRequest<?>, Boolean> {
		private final String scheme;

		private HttpSchemeCondition(String scheme) {
			this.scheme = scheme;
		}

		@Override
		public Boolean apply(ServerHttpRequest<?> request) {
			return scheme.equals(request.getURI().getScheme());
		}
	}

	private class HttpHostCondition implements Function<ServerHttpRequest<?>, Boolean> {
		private final Pattern host;

		private HttpHostCondition(String host) {
			this.host = Pattern.compile(host);
		}

		@Override
		public Boolean apply(ServerHttpRequest<?> request) {
			return host.matcher(request.getURI().getHost()).matches();
		}
	}

	private class HeaderExistsCondition implements Function<ServerHttpRequest<?>, Boolean> {
		private final String header;

		private HeaderExistsCondition(String header) {
			this.header = header;
		}

		@Override
		public Boolean apply(ServerHttpRequest<?> request) {
			return null != request.getHeaders().get(header);
		}
	}

	private class QueryParameterExistsCondition implements Function<ServerHttpRequest<?>, Boolean> {
		private final String param;

		private QueryParameterExistsCondition(String param) {
			this.param = param;
		}

		@Override
		public Boolean apply(ServerHttpRequest<?> request) {
			return null != request.getQueryParams().get(param);
		}
	}

}
