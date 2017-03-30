package com.monitorjbl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class VaultConfiguration extends AbstractConfiguration implements Configuration {

  private final ObjectMapper mapper;
  private final PoolingHttpClientConnectionManager cm;
  private final CloseableHttpClient httpClient;
  private final String vaultUrl;
  private final String vaultToken;
  private final String vaultBackend;
  private final String configName;

  public VaultConfiguration(String vaultUrl, String vaultToken, String vaultBackend, String configName) {
    this.vaultUrl = vaultUrl;
    this.vaultToken = vaultToken;
    this.vaultBackend = vaultBackend;
    this.configName = configName;
    this.cm = new PoolingHttpClientConnectionManager();
    this.httpClient = HttpClients.custom()
        .setConnectionManager(cm)
        .build();
    this.mapper = new ObjectMapper();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> loadFromVault() {
    HttpGet get = new HttpGet(vaultUrl + "/v1/" + vaultBackend + "/" + configName);
    get.addHeader("X-Vault-Token", vaultToken);
    try(CloseableHttpResponse response = httpClient.execute(get)) {
      if(response.getStatusLine().getStatusCode() != 200) {
        throw new RuntimeException("Could not read from Vault (" + response.getStatusLine().getStatusCode() + ")");
      }
      Map map = mapper.readValue(response.getEntity().getContent(), Map.class);
      return (Map<String, Object>) map.get("data");
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void addPropertyDirect(String key, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void clearPropertyDirect(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected boolean isEmptyInternal() {
    return loadFromVault().isEmpty();
  }

  @Override
  protected boolean containsKeyInternal(String key) {
    return loadFromVault().containsKey(key);
  }

  @Override
  protected Object getPropertyInternal(String key) {
    return loadFromVault().get(key);
  }

  @Override
  protected Iterator<String> getKeysInternal() {
    return loadFromVault().keySet().iterator();
  }

  public static void main(String[] args) {
    VaultConfiguration config = new VaultConfiguration(
        "http://127.0.0.1:8200",
        "26a78c5a-9d72-e020-493a-20ab39dd5136",
        "secret",
        "account-management");
    config.getString("excited");
  }
}
