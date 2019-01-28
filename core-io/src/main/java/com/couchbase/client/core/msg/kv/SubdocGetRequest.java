/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.client.core.msg.kv;

import com.couchbase.client.core.CoreContext;
import com.couchbase.client.core.io.netty.kv.ChannelContext;
import com.couchbase.client.core.io.netty.kv.MemcacheProtocol;
import com.couchbase.client.core.retry.RetryStrategy;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.couchbase.client.core.io.netty.kv.MemcacheProtocol.body;
import static com.couchbase.client.core.io.netty.kv.MemcacheProtocol.cas;
import static com.couchbase.client.core.io.netty.kv.MemcacheProtocol.decodeStatus;
import static com.couchbase.client.core.io.netty.kv.MemcacheProtocol.noCas;
import static com.couchbase.client.core.io.netty.kv.MemcacheProtocol.noDatatype;
import static com.couchbase.client.core.io.netty.kv.MemcacheProtocol.noExtras;
import static com.couchbase.client.core.io.netty.kv.MemcacheProtocol.request;

public class SubdocGetRequest extends BaseKeyValueRequest<SubdocGetResponse> {

  private static final byte SUBDOC_FLAG_XATTR_PATH = (byte) 0x04;

  private final byte flags;
  private final List<Command> commands;

  public SubdocGetRequest(final Duration timeout, final CoreContext ctx, final String bucket,
                          final RetryStrategy retryStrategy, final String key,
                          final byte[] collection, final byte flags, final List<Command> commands) {
    super(timeout, ctx, bucket, retryStrategy, key, collection);
    this.flags = flags;
    this.commands = commands;
  }

  @Override
  public ByteBuf encode(ByteBufAllocator alloc, int opaque, ChannelContext ctx) {
    ByteBuf key = Unpooled.wrappedBuffer(ctx.collectionsEnabled() ? keyWithCollection() : key());

    ByteBuf extras = flags != 0
      ? alloc.buffer(1, 1).writeByte(flags)
      : noExtras();

    ByteBuf body;
    if (commands.size() == 1) {
      // todo: Optimize into single get request only?
      body = commands.get(0).encode(alloc);
    } else {
      body = alloc.compositeBuffer(commands.size());
      for (Command command : commands) {
        ByteBuf commandBuffer = command.encode(alloc);
        ((CompositeByteBuf) body).addComponent(commandBuffer);
        body.writerIndex(body.writerIndex() + commandBuffer.readableBytes());
      }
    }


    ByteBuf request = request(
      alloc,
      MemcacheProtocol.Opcode.SUBDOC_MULTI_LOOKUP,
      noDatatype(),
      partition(),
      opaque,
      noCas(),
      extras,
      key,
      body
    );

    if (flags != 0) {
      extras.release();
    }
    key.release();
    body.release();
    return request;
  }

  @Override
  public SubdocGetResponse decode(final ByteBuf response, ChannelContext ctx) {
    Optional<ByteBuf> maybeBody = body(response);
    List<SubdocGetResponse.ResponseValue> values;
    if (maybeBody.isPresent()) {
      ByteBuf body = maybeBody.get();
      values = new ArrayList<>(commands.size());
      for (Command command : commands) {
        short status = body.readShort();
        int valueLength = body.readInt();
        byte[] value = new byte[valueLength];
        body.readBytes(value, 0, valueLength);
        values.add(new SubdocGetResponse.ResponseValue(status, value, command.path));
      }
    } else {
      values = new ArrayList<>();
    }
    return new SubdocGetResponse(decodeStatus(response), values, cas(response));
  }

  public static class Command {
    private final CommandType type;
    private final String path;
    private final boolean xattr;

    public Command(CommandType type, String path, boolean xattr) {
      this.type = type;
      this.path = path;
      this.xattr = xattr;
    }

    public ByteBuf encode(ByteBufAllocator alloc) {
      byte[] path = this.path.getBytes(CharsetUtil.UTF_8);
      int pathLength = path.length;

      ByteBuf buffer = alloc.buffer(4 + pathLength);
      buffer.writeByte(type.opcode());
      if (xattr) {
        buffer.writeByte(SUBDOC_FLAG_XATTR_PATH);
      } else {
        buffer.writeByte(0);
      }
      buffer.writeShort(pathLength);
      buffer.writeBytes(path);
      return buffer;
    }
  }

  public enum CommandType {
    GET((byte) 0xc5),
    EXISTS((byte) 0xc6),
    COUNT((byte) 0xd2),
    GET_DOC((byte) 0x00);

    private final byte opcode;

    CommandType(byte opcode) {
      this.opcode = opcode;
    }

    byte opcode() {
      return opcode;
    }
  }
}
