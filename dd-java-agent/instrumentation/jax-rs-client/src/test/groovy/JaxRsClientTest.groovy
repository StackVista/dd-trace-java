import datadog.trace.agent.test.base.HttpClientTest
import datadog.trace.instrumentation.jaxrs.JaxRsClientDecorator
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import org.apache.cxf.jaxrs.client.spec.ClientBuilderImpl
import org.glassfish.jersey.client.JerseyClientBuilder
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder

abstract class JaxRsClientTest extends HttpClientTest<JaxRsClientDecorator> {

  @Override
  int doRequest(String method, URI uri, Map<String, String> headers, Closure callback) {

    Client client = builder().build()
    WebTarget service = client.target(uri)
    Invocation.Builder request = service.request(MediaType.TEXT_PLAIN)
    headers.each { request.header(it.key, it.value) }
    def body = BODY_METHODS.contains(method) ? Entity.text("") : null
    Response response = request.method(method, (Entity) body)
    callback?.call()

    return response.status
  }

  @Override
  JaxRsClientDecorator decorator() {
    return JaxRsClientDecorator.DECORATE
  }

  @Override
  String expectedOperationName() {
    return "jax-rs.client.call"
  }

  boolean testRedirects() {
    false
  }

  abstract ClientBuilder builder()
}

class JerseyClientTest extends JaxRsClientTest {

  @Override
  ClientBuilder builder() {
    return new JerseyClientBuilder()
  }
}

class ResteasyClientTest extends JaxRsClientTest {

  @Override
  ClientBuilder builder() {
    return new ResteasyClientBuilder()
  }
}

class CxfClientTest extends JaxRsClientTest {

  @Override
  ClientBuilder builder() {
    return new ClientBuilderImpl()
  }

  boolean testConnectionFailure() {
    false
  }
}
