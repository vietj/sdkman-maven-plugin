package io.sdkman.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class BaseMojo extends AbstractMojo {

  /** The SDK consumer key */
  @Parameter(property = "sdkman.consumer.key", required = true)
  protected String consumerKey;

  /** The SDK consumer token */
  @Parameter(property = "sdkman.consumer.token", required = true)
  protected String consumerToken;

  /** candidate identifier */
  @Parameter(property = "sdkman.candidate", required = true)
  protected String candidate;

  /** candidate version */
  @Parameter(property = "sdkman.version", required = true)
  protected String version;

  /** SDK service hostname */
  @Parameter(property = "sdkman.api.host", defaultValue = "vendors.sdkman.io")
  protected String apiHost;

  /** Skip this execution */
  @Parameter(property = "sdkman.skip")
  private boolean skip;

  protected Map<String, String> getPayload() {
    Map<String, String> payload = new HashMap<>();
    payload.put("candidate", candidate);
    payload.put("version", version);
    return payload;
  }

  protected abstract HttpEntityEnclosingRequestBase createHttpRequest();

  @Override
  public final void execute() throws MojoExecutionException {
    if (skip) {
      getLog().info("Skipping plugin execution");
      return;
    }
    doExecute();
  }

  protected void doExecute() throws MojoExecutionException {
    try {
      HttpResponse resp = execCall(getPayload(), createHttpRequest());
      int statusCode = resp.getStatusLine().getStatusCode();
      if (statusCode < 200 || statusCode >= 300) {
        throw new IllegalStateException("Server returned error " + resp.getStatusLine());
      }
    } catch (Exception e) {
      throw new MojoExecutionException("Sdk vendor operation failed", e);
    }
  }

  protected HttpResponse execCall(Map<String, String> payload, HttpEntityEnclosingRequestBase req) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(payload);

    req.addHeader("Consumer-Key", consumerKey);
    req.addHeader("Consumer-Token", consumerToken);
    req.addHeader("Content-Type", "application/json");
    req.addHeader("Accept", "application/json");
    req.setEntity(new StringEntity(json));

    CloseableHttpClient client = HttpClientBuilder.create().build();
    CloseableHttpResponse resp = client.execute(req);
    try(InputStream in = resp.getEntity().getContent()) {
      Map<String, ?> sdkmanResp = (Map<String, ?>) mapper.readValue(in, Map.class);
      for (Map.Entry<String, ?> prop : sdkmanResp.entrySet()) {
        getLog().debug(prop.getKey() + ":" + prop.getValue());
      }
    }
    return resp;
  }
}
