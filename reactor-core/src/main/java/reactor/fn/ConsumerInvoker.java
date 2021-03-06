/*
 * Copyright (c) 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.fn;

import reactor.convert.Converter;

/**
 * Implementations of this interface are responsible for invoking a {@link Consumer} that may take into account
 * automatic argument conversion, return values, and other situations that might be specific to a particular use-case.
 *
 * @author Jon Brisbin
 */
public interface ConsumerInvoker extends Supports<Consumer<?>> {

	/**
	 * Invoke a {@link Consumer}.
	 *
	 * @param consumer     The {@link Consumer} to invoke.
	 * @param converter    The {@link Converter} to be aware of. May be {@literal null}.
	 * @param returnType   If the {@link Consumer} also implements a value-returning type, convert it to this type before
	 *                     returning.
	 * @param possibleArgs An array of possible arguments that may or may not be used.
	 * @param <T>          The return type.
	 * @return A result if available, or {@literal null} otherwise.
	 * @throws Exception
	 */
	<T> T invoke(Consumer<?> consumer,
							 Converter converter,
							 Class<? extends T> returnType,
							 Object... possibleArgs) throws Exception;

}
