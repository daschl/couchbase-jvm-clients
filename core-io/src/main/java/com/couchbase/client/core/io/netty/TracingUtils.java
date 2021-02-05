/*
 * Copyright (c) 2021 Couchbase, Inc.
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

package com.couchbase.client.core.io.netty;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.cnc.RequestSpan;
import com.couchbase.client.core.cnc.TracingIdentifiers;

/**
 * Contains various utils to set attributes for tracing spans.
 */
@Stability.Internal
public class TracingUtils {

  private TracingUtils() {}

  /**
   * Sets common dispatch span attributes as per spec.
   *
   * @param span the affected span.
   * @param localId the local socket ID.
   * @param localHost the local hostname or ip.
   * @param localPort the local port.
   * @param remoteHost the remote hostname or ip.
   * @param remotePort the remote port.
   * @param operationId the unique operation ID - can be null (then ignored).
   */
  public static void setCommonDispatchSpanAttributes(final RequestSpan span, final String localId,
                                                     final String localHost, final int localPort,
                                                     final String remoteHost, final int remotePort,
                                                     final String operationId) {
    span.setAttribute(TracingIdentifiers.ATTR_SYSTEM, TracingIdentifiers.ATTR_SYSTEM_COUCHBASE);
    span.setAttribute(TracingIdentifiers.ATTR_NET_TRANSPORT, TracingIdentifiers.ATTR_NET_TRANSPORT_TCP);
    span.setAttribute(TracingIdentifiers.ATTR_LOCAL_ID, localId);

    if (localHost != null) {
      span.setAttribute(TracingIdentifiers.ATTR_LOCAL_HOSTNAME, localHost);
    }
    if (localPort != 0) {
      span.setAttribute(TracingIdentifiers.ATTR_LOCAL_PORT, localPort);
    }
    if (remoteHost != null) {
      span.setAttribute(TracingIdentifiers.ATTR_REMOTE_HOSTNAME, remoteHost);
    }
    if (remotePort != 0) {
      span.setAttribute(TracingIdentifiers.ATTR_REMOTE_PORT, remotePort);
    }
    if (operationId != null) {
      span.setAttribute(TracingIdentifiers.ATTR_OPERATION_ID, operationId);
    }
  }

  /**
   * Sets the server duration attribute, if larger than 0 (will ignore it otherwise).
   *
   * @param span the span where it should be set.
   * @param serverDuration the actual duration.
   */
  public static void setServerDurationAttribute(final RequestSpan span, final long serverDuration) {
    if (serverDuration > 0) {
      span.setAttribute(TracingIdentifiers.ATTR_SERVER_DURATION, serverDuration);
    }
  }

}
