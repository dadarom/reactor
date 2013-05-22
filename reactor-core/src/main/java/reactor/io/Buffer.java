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

package reactor.io;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class Buffer implements Comparable<Buffer> {

	public static int SMALL_BUFFER_SIZE = Integer.parseInt(
			System.getProperty("reactor.small_buffer_size", "" + 1024 * 16)
	);
	public static int MAX_BUFFER_SIZE   = Integer.parseInt(
			System.getProperty("reactor.max_buffer_size", "" + 1024 * 1000)
	);
	private final boolean    dynamic;
	private       ByteBuffer buffer;

	public Buffer() {
		dynamic = true;
	}

	public Buffer(int atLeast, boolean fixed) {
		if (fixed) {
			if (atLeast <= MAX_BUFFER_SIZE) {
				buffer = ByteBuffer.allocate(atLeast);
			} else {
				throw new IllegalArgumentException("Requested buffer size exceeds maximum allowed (" + MAX_BUFFER_SIZE + ")");
			}
		} else {
			ensureCapacity(atLeast);
		}
		dynamic = !fixed;
	}

	public Buffer(Buffer bufferToCopy) {
		this.dynamic = bufferToCopy.dynamic;
		this.buffer = bufferToCopy.buffer.duplicate();
	}

	public Buffer(ByteBuffer bufferToStartWith) {
		this.dynamic = true;
		this.buffer = bufferToStartWith;
	}

	public static Buffer wrap(byte[] bytes) {
		return new Buffer(bytes.length, true)
				.append(bytes)
				.flip();
	}

	public static Buffer wrap(String str, boolean fixed) {
		return new Buffer(str.length(), fixed)
				.append(str)
				.flip();
	}

	public static Buffer wrap(String str) {
		return wrap(str, true);
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public int position() {
		return (null == buffer ? 0 : buffer.position());
	}

	public int capacity() {
		return (null == buffer ? SMALL_BUFFER_SIZE : buffer.capacity());
	}

	public int remaining() {
		return (null == buffer ? SMALL_BUFFER_SIZE : buffer.remaining());
	}

	public Buffer clear() {
		if (null != buffer) {
			buffer = null;
		}
		return this;
	}

	public Buffer flip() {
		if (null != buffer) {
			buffer.flip();
		}
		return this;
	}

	public Buffer rewind() {
		if (null != buffer) {
			buffer.rewind();
		}
		return this;
	}

	public byte read() {
		if (null != buffer) {
			return buffer.get();
		}
		throw new BufferUnderflowException();
	}

	public Buffer read(byte[] b) {
		if (null != buffer) {
			buffer.get(b);
		}
		return this;
	}

	public int readInt() {
		if (null != buffer) {
			return buffer.getInt();
		}
		throw new BufferUnderflowException();
	}

	public float readFloat() {
		if (null != buffer) {
			return buffer.getFloat();
		}
		throw new BufferUnderflowException();
	}

	public double readDouble() {
		if (null != buffer) {
			return buffer.getDouble();
		}
		throw new BufferUnderflowException();
	}

	public long readLong() {
		if (null != buffer) {
			return buffer.getLong();
		}
		throw new BufferUnderflowException();
	}

	public char readChar() {
		if (null != buffer) {
			return buffer.getChar();
		}
		throw new BufferUnderflowException();
	}

	public String asString() {
		if (null != buffer) {
			buffer.mark();
			byte[] b = new byte[buffer.remaining()];
			buffer.get(b);
			buffer.reset();
			return new String(b);
		} else {
			return null;
		}
	}

	public byte[] asBytes() {
		if (null != buffer) {
			buffer.mark();
			byte[] b = new byte[buffer.remaining()];
			buffer.get(b);
			buffer.reset();
			return b;
		} else {
			return null;
		}
	}

	public Buffer slice(int start, int len) {
		buffer.mark();
		byte[] bytes = new byte[len];
		buffer.position(start);
		buffer.get(bytes);
		buffer.reset();
		//buffer.limit(buffer.capacity());
		return new Buffer(len, true).append(bytes);
	}

	public Buffer append(String s) {
		ensureCapacity(s.length());
		buffer.put(s.getBytes());
		return this;
	}

	public Buffer append(int i) {
		ensureCapacity(4);
		buffer.putInt(i);
		return this;
	}

	public Buffer append(long l) {
		ensureCapacity(8);
		buffer.putLong(l);
		return this;
	}

	public Buffer append(ByteBuffer b) {
		ensureCapacity(b.remaining());
		buffer.put(b);
		return this;
	}

	public Buffer append(Buffer b) {
		int pos = buffer.position();
		int len = b.remaining();
		ensureCapacity(len);
		buffer.put(b.asByteBuffer());
		buffer.position(pos + len);
		return this;
	}

	public Buffer append(byte b) {
		ensureCapacity(1);
		buffer.put(b);
		return this;
	}

	public Buffer append(byte[] b) {
		ensureCapacity(b.length);
		buffer.put(b);
		return this;
	}

	public ByteBuffer asByteBuffer() {
		return buffer;
	}

	public ReadableByteChannel asReadableByteChannel() {
		return new ReadableByteChannel() {
			@Override
			public int read(ByteBuffer dst) throws IOException {
				int start = buffer.position();
				dst.put(buffer);
				return buffer.position() - start;
			}

			@Override
			public boolean isOpen() {
				return buffer.hasRemaining();
			}

			@Override
			public void close() throws IOException {
			}
		};
	}

	public WritableByteChannel asWritableByteChannel() {
		return new WritableByteChannel() {
			@Override
			public int write(ByteBuffer src) throws IOException {
				int start = position();
				append(src);
				return position() - start;
			}

			@Override
			public boolean isOpen() {
				return isDynamic();
			}

			@Override
			public void close() throws IOException {
			}
		};
	}

	@Override
	public String toString() {
		return (null != buffer ? buffer.toString() : "<EMPTY>");
	}

	@Override
	public int compareTo(Buffer buffer) {
		return (null != buffer ? this.buffer.compareTo(buffer.buffer) : -1);
	}

	private void ensureCapacity(int atLeast) {
		if (null == buffer) {
			buffer = ByteBuffer.allocateDirect(SMALL_BUFFER_SIZE);
			return;
		}
		if (dynamic && buffer.remaining() < atLeast) {
			if (buffer.capacity() + SMALL_BUFFER_SIZE <= MAX_BUFFER_SIZE) {
				ByteBuffer newBuff = ByteBuffer.allocate(buffer.limit() + SMALL_BUFFER_SIZE);
				buffer.flip();
				newBuff.put(buffer);
				buffer = newBuff;
			} else {
				throw new IllegalStateException("Requested buffer size exceeds maximum allowed (" + MAX_BUFFER_SIZE + ")");
			}
		}
	}

}
