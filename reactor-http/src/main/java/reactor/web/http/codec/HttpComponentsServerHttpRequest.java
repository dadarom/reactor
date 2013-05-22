package reactor.web.http.codec;

import org.apache.http.Header;
import reactor.core.Composable;
import reactor.util.LinkedMultiValueMap;
import reactor.util.MultiValueMap;
import reactor.web.http.Cookies;
import reactor.web.http.HttpHeaders;
import reactor.web.http.HttpMethod;

import java.security.Principal;

/**
 * @author Jon Brisbin
 */
class HttpComponentsServerHttpRequest<T> extends ServerHttpRequestSupport<T> {

	private final org.apache.http.HttpRequest   request;
	private final Composable<T>                 body;
	private       HttpHeaders                   headers;
	private       Cookies                       cookies;
	private       MultiValueMap<String, String> queryParams;

	HttpComponentsServerHttpRequest(org.apache.http.HttpRequest request,
																	Composable<T> body) {
		super(HttpMethod.valueOf(request.getRequestLine().getMethod()),
					request.getRequestLine().getUri(),
					null != request.getFirstHeader("X-Remote-Host") ? request.getFirstHeader("X-Remote-Host").getValue() : null,
					null != request.getFirstHeader("X-Remote-Addr") ? request.getFirstHeader("X-Remote-Addr").getValue() : null);
		this.request = request;
		this.body = body;
	}

	@Override
	public MultiValueMap<String, String> getQueryParams() {
		if (null == queryParams) {
			queryParams = new LinkedMultiValueMap<String, String>();
			String queryStr = getURI().getQuery();
			// TODO: decode query string
		}
		return queryParams;
	}

	@Override
	public Principal getPrincipal() {
		return null;
	}

	@Override
	public Composable<T> getBody() {
		return body;
	}

	@Override
	public HttpHeaders getHeaders() {
		if (null == headers) {
			headers = new HttpHeaders();
			for (Header header : request.getAllHeaders()) {
				headers.add(header.getName(), header.getValue());
			}
		}
		return headers;
	}

	@Override
	public Cookies getCookies() {
		if (null == cookies) {
			cookies = new Cookies();
			// TODO: parse cookies
		}
		return cookies;
	}

}
