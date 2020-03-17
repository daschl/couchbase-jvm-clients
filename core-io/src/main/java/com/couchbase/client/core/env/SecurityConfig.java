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

package com.couchbase.client.core.env;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.error.InvalidArgumentException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.couchbase.client.core.util.Validators.notNull;
import static com.couchbase.client.core.util.Validators.notNullOrEmpty;

/**
 * The {@link SecurityConfig} allows to enable transport encryption between the client and the servers.
 */
public class SecurityConfig {

  /**
   * By default, TLS is disabled.
   */
  private static final boolean DEFAULT_TLS_ENABLED = false;

  /**
   * By default, netty native tls (OpenSSL) is enabled for better performance.
   */
  private static final boolean DEFAULT_NATIVE_TLS_ENABLED = true;

  private final boolean nativeTlsEnabled;
  private final boolean tlsEnabled;
  private final List<X509Certificate> trustCertificates;
  private final TrustManagerFactory trustManagerFactory;

  /**
   * Creates a builder to customize the {@link SecurityConfig} configuration.
   *
   * @return the builder to customize.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a {@link SecurityConfig} with the default configuration.
   *
   * @return the default security config.
   */
  public static SecurityConfig create() {
    return new SecurityConfig(builder());
  }

  /**
   * Enables TLS for all client/server communication (disabled by default).
   *
   * @param tlsEnabled true if enabled, false otherwise.
   * @return this {@link Builder} for chaining purposes.
   */
  public static Builder enableTls(boolean tlsEnabled) {
    return builder().enableTls(tlsEnabled);
  }

  /**
   * Enables/disables native TLS (enabled by default).
   *
   * @param nativeTlsEnabled true if it should be enabled, false otherwise.
   * @return this {@link Builder} for chaining purposes.
   */
  public static Builder enableNativeTls(boolean nativeTlsEnabled) {
    return builder().enableNativeTls(nativeTlsEnabled);
  }

  /**
   * Loads the given list of X.509 certificates into the trust store.
   *
   * @param certificates the list of certificates to load.
   * @return this {@link Builder} for chaining purposes.
   */
  public static Builder trustCertificates(final List<X509Certificate> certificates) {
    return builder().trustCertificates(certificates);
  }

  /**
   * Loads a X.509 trust certificate from the given path and uses it.
   *
   * @param certificatePath the path to load the certificate from.
   * @return this {@link Builder} for chaining purposes.
   */
  public static Builder trustCertificate(final Path certificatePath) {
    return builder().trustCertificate(certificatePath);
  }

  /**
   * Initializes the {@link TrustManagerFactory} with the given trust store.
   *
   * @param trustStore the loaded trust store to use.
   * @return this {@link Builder} for chaining purposes.
   */
  public static Builder trustStore(final KeyStore trustStore) {
    return builder().trustStore(trustStore);
  }

  /**
   * Loads a trust store from a file path and password and initializes the {@link TrustManagerFactory}.
   *
   * @param trustStorePath the path to the truststore.
   * @param trustStorePassword the password (can be null if not password protected).
   * @param trustStoreType the type of the trust store. If empty, the {@link KeyStore#getDefaultType()} will be used.
   * @return this {@link Builder} for chaining purposes.
   */
  public static Builder trustStore(final Path trustStorePath, final String trustStorePassword,
                                   final Optional<String> trustStoreType) {
    return builder().trustStore(trustStorePath, trustStorePassword, trustStoreType);
  }

  /**
   * Allows to provide a trust manager factory directly for maximum flexibility.
   * <p>
   * While providing the most flexibility, most users will find the other overloads more convenient, like passing
   * in a {@link #trustStore(KeyStore)} directly or via filepath {@link #trustStore(Path, String, Optional)}.
   *
   * @param trustManagerFactory the trust manager factory to use.
   * @return this {@link Builder} for chaining purposes.
   */
  public static Builder trustManagerFactory(final TrustManagerFactory trustManagerFactory) {
    return builder().trustManagerFactory(trustManagerFactory);
  }

  private SecurityConfig(final Builder builder) {
    tlsEnabled = builder.tlsEnabled;
    nativeTlsEnabled = builder.nativeTlsEnabled;
    trustCertificates = builder.trustCertificates;
    trustManagerFactory = builder.trustManagerFactory;

    if (tlsEnabled) {
      if (trustCertificates != null && trustManagerFactory != null) {
        throw InvalidArgumentException.fromMessage("Either trust certificates or a trust manager factory" +
          " can be provided, but not both!");
      }
      if ((trustCertificates == null || trustCertificates.isEmpty()) && trustManagerFactory == null) {
        throw InvalidArgumentException.fromMessage("Either a trust certificate or a trust manager factory" +
          " must be provided when TLS is enabled!");
      }

    }
  }

  /**
   * True if TLS is enabled, false otherwise.
   *
   * @return a boolean if tls/transport encryption is enabled.
   */
  public boolean tlsEnabled() {
    return tlsEnabled;
  }

  /**
   * The list of trust certificates that should be used, if present.
   *
   * @return the list of certificates.
   */
  public List<X509Certificate> trustCertificates() {
    return trustCertificates;
  }

  /**
   * The currently configured trust manager factory, if present.
   *
   * @return the trust manager factory.
   */
  public TrustManagerFactory trustManagerFactory() {
    return trustManagerFactory;
  }

  /**
   * Returns whether native TLS is enabled.
   *
   * @return true if enabled, false otherwise.
   */
  public boolean nativeTlsEnabled() {
    return nativeTlsEnabled;
  }

  /**
   * Returns this config as a map so it can be exported into i.e. JSON for display.
   */
  @Stability.Volatile
  Map<String, Object> exportAsMap() {
    final Map<String, Object> export = new LinkedHashMap<>();
    export.put("tlsEnabled", tlsEnabled);
    export.put("nativeTlsEnabled", nativeTlsEnabled);
    export.put("hasTrustCertificates", trustCertificates != null && !trustCertificates.isEmpty());
    export.put("trustManagerFactory", trustManagerFactory != null ? trustManagerFactory.getClass().getSimpleName() : null);
    return export;
  }

  /**
   * This builder allows to customize the default security configuration.
   */
  public static class Builder {

    private boolean tlsEnabled = DEFAULT_TLS_ENABLED;
    private boolean nativeTlsEnabled = DEFAULT_NATIVE_TLS_ENABLED;
    private List<X509Certificate> trustCertificates = null;
    private TrustManagerFactory trustManagerFactory = null;

    /**
     * Builds the {@link SecurityConfig} out of this builder.
     *
     * @return the built security config.
     */
    public SecurityConfig build() {
      return new SecurityConfig(this);
    }

    /**
     * Enables TLS for all client/server communication (disabled by default).
     *
     * @param tlsEnabled true if enabled, false otherwise.
     * @return this {@link Builder} for chaining purposes.
     */
    public Builder enableTls(boolean tlsEnabled) {
      this.tlsEnabled = tlsEnabled;
      return this;
    }

    /**
     * Enables/disables native TLS (enabled by default).
     *
     * @param nativeTlsEnabled true if it should be enabled, false otherwise.
     * @return this {@link Builder} for chaining purposes.
     */
    public Builder enableNativeTls(boolean nativeTlsEnabled) {
      this.nativeTlsEnabled = nativeTlsEnabled;
      return this;
    }

    /**
     * Loads the given list of X.509 certificates into the trust store.
     *
     * @param certificates the list of certificates to load.
     * @return this {@link Builder} for chaining purposes.
     */
    public Builder trustCertificates(final List<X509Certificate> certificates) {
      this.trustCertificates = notNullOrEmpty(certificates, "X509 Certificates");
      return this;
    }

    /**
     * Loads a X.509 trust certificate from the given path and uses it.
     *
     * @param certificatePath the path to load the certificate from.
     * @return this {@link Builder} for chaining purposes.
     */
    public Builder trustCertificate(final Path certificatePath) {
      notNull(certificatePath, "CertificatePath");

      final StringBuilder contentBuilder = new StringBuilder();
      try {
        Files.lines(certificatePath, StandardCharsets.UTF_8).forEach(s -> contentBuilder.append(s).append("\n"));
      } catch (IOException ex) {
        throw InvalidArgumentException.fromMessage(
          "Could not read trust certificate from file \"" + certificatePath + "\"" ,
          ex
        );
      }
      return trustCertificates(decodeCertificates(Collections.singletonList(contentBuilder.toString())));
    }

    /**
     * Allows to provide a trust manager factory directly for maximum flexibility.
     * <p>
     * While providing the most flexibility, most users will find the other overloads more convenient, like passing
     * in a {@link #trustStore(KeyStore)} directly or via filepath {@link #trustStore(Path, String, Optional)}.
     *
     * @param trustManagerFactory the trust manager factory to use.
     * @return this {@link Builder} for chaining purposes.
     */
    public Builder trustManagerFactory(final TrustManagerFactory trustManagerFactory) {
      this.trustManagerFactory = notNull(trustManagerFactory, "TrustManagerFactory");
      return this;
    }

    /**
     * Initializes the {@link TrustManagerFactory} with the given trust store.
     *
     * @param trustStore the loaded trust store to use.
     * @return this {@link Builder} for chaining purposes.
     */
    public Builder trustStore(final KeyStore trustStore) {
      notNull(trustStore, "TrustStore");

      try {
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        return trustManagerFactory(tmf);
      } catch (Exception ex) {
        throw InvalidArgumentException.fromMessage(
          "Could not initialize TrustManagerFactory from TrustStore",
          ex
        );
      }
    }

    /**
     * Loads a trust store from a file path and password and initializes the {@link TrustManagerFactory}.
     *
     * @param trustStorePath the path to the truststore.
     * @param trustStorePassword the password (can be null if not password protected).
     * @param trustStoreType the type of the trust store. If empty, the {@link KeyStore#getDefaultType()} will be used.
     * @return this {@link Builder} for chaining purposes.
     */
    public Builder trustStore(final Path trustStorePath, final String trustStorePassword,
                              final Optional<String> trustStoreType) {
      notNull(trustStorePath, "TrustStorePath");
      notNull(trustStoreType, "TrustStoreType");

      try {
        final KeyStore store = KeyStore.getInstance(trustStoreType.orElse(KeyStore.getDefaultType()));
        store.load(
          Files.newInputStream(trustStorePath),
          trustStorePassword != null ? trustStorePassword.toCharArray() : null
        );
        return trustStore(store);
      } catch (Exception ex) {
        throw InvalidArgumentException.fromMessage("Could not initialize TrustStore", ex);
      }
    }

  }

  /**
   * Helper method to decode string-encoded certificates into their x.509 format.
   *
   * @param certificates the string-encoded certificates.
   * @return the decoded certs in x.509 format.
   */
  public static List<X509Certificate> decodeCertificates(final List<String> certificates) {
    notNull(certificates, "Certificates");

    final CertificateFactory cf;
    try {
      cf = CertificateFactory.getInstance("X.509");
    } catch (CertificateException e) {
      throw InvalidArgumentException.fromMessage("Could not instantiate X.509 CertificateFactory", e);
    }

    return certificates.stream().map(c -> {
      try {
        return (X509Certificate) cf.generateCertificate(
          new ByteArrayInputStream(c.getBytes(StandardCharsets.UTF_8))
        );
      } catch (CertificateException e) {
        throw InvalidArgumentException.fromMessage("Could not generate certificate from raw input: \"" + c + "\"", e);
      }
    }).collect(Collectors.toList());
  }

}
