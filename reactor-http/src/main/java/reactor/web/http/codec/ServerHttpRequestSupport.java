package reactor.web.http.codec;

import reactor.web.http.HttpMethod;
import reactor.web.http.server.ServerHttpRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;

/**
 * @author Jon Brisbin
 */
public abstract class ServerHttpRequestSupport<T> implements ServerHttpRequest<T> {

	protected final HttpMethod method;
	protected final String     requestUri;
	protected final String     remoteHost;
	protected final String     remoteAddress;

	protected ServerHttpRequestSupport(@Nonnull HttpMethod method,
																		 @Nonnull String requestUri,
																		 @Nullable String remoteHost,
																		 @Nullable String remoteAddress) {
		this.method = method;
		this.requestUri = requestUri;
		this.remoteHost = remoteHost;
		this.remoteAddress = remoteAddress;
	}

	@Override
	public String getRemoteHostName() {
		return remoteHost;
	}

	@Override
	public String getRemoteAddress() {
		return remoteAddress;
	}

	@Override
	public HttpMethod getMethod() {
		return method;
	}

	@Override
	public URI getURI() {
		return URI.create(requestUri);
	}

}
