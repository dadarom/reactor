package reactor.web.http.server;

import reactor.core.Composable;
import reactor.web.http.ResponseEntity;

/**
 * @author Jon Brisbin
 */
public abstract class ServerHttpRequestHandler<T> {

	public Class<T> getRequestBodyType() {
		return null;
	}

	public abstract void serve(ServerHttpRequest<T> req, Composable<ResponseEntity<?>> resp);

}
