/*
 * Copyright (c) 2019 Couchbase, Inc.
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

package com.couchbase.client.core.cnc.events.core;

import com.couchbase.client.core.Core;
import com.couchbase.client.core.CoreContext;
import com.couchbase.client.core.cnc.AbstractEvent;
import com.couchbase.client.core.cnc.Context;
import com.couchbase.client.core.env.CoreEnvironment;
import com.couchbase.client.core.env.SeedNode;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This event is emitted when a {@link Core} is created.
 */
public class CoreCreatedEvent extends AbstractEvent {

  private final CoreEnvironment environment;

  public CoreCreatedEvent(final CoreContext context, final CoreEnvironment environment, final Set<SeedNode> seedNodes) {
    super(Severity.INFO, Category.CORE, Duration.ZERO, attachSeedNodes(context, seedNodes));
    this.environment = environment;
  }

  private static Context attachSeedNodes(final CoreContext context, final Set<SeedNode> seedNodes) {
    return new CoreContext(context.core(), context.id(), context.environment(), context.authenticator()) {
      @Override
      public void injectExportableParams(final Map<String, Object> input) {
        super.injectExportableParams(input);

        input.put("seedNodes", seedNodes.stream().map(seedNode -> {
          Map<String, Object> mapped = new HashMap<>();
          mapped.put("address", seedNode.address());
          seedNode.kvPort().ifPresent(p -> mapped.put("kvPort", p));
          seedNode.clusterManagerPort().ifPresent(p -> mapped.put("mgmtPort", p));
          return mapped;
        }).collect(Collectors.toSet()));
      }
    };
  }

  @Override
  public String description() {
    return environment.exportAsString(Context.ExportFormat.JSON);
  }

}
