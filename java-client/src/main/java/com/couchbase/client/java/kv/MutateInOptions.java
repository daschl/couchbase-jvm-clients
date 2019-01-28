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

package com.couchbase.client.java.kv;

import com.couchbase.client.java.CommonOptions;

import java.time.Duration;

public class MutateInOptions extends CommonOptions<MutateInOptions> {

  public static MutateInOptions DEFAULT = new MutateInOptions();

  private Duration expiry = Duration.ZERO;
  private long cas = 0;

  public static MutateInOptions mutateInOptions() {
    return new MutateInOptions();
  }

  private MutateInOptions() {

  }

  public Duration expiry() {
    return expiry;
  }

  public MutateInOptions expiry(final Duration expiry) {
    this.expiry = expiry;
    return this;
  }

  public long cas() {
    return cas;
  }

  public MutateInOptions cas(long cas) {
    this.cas = cas;
    return this;
  }
}
