package stackstate.trace.instrumentation.jaxrs;

import static net.bytebuddy.matcher.ElementMatchers.failSafe;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import com.google.auto.service.AutoService;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.client.ClientBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatcher;
import stackstate.trace.agent.tooling.*;

@AutoService(Instrumenter.class)
public final class JaxRsClientInstrumentation extends Instrumenter.Default {

  public JaxRsClientInstrumentation() {
    super("jax-rs", "jaxrs", "jax-rs-client");
  }

  @Override
  public ElementMatcher typeMatcher() {
    return failSafe(hasSuperType(named("javax.ws.rs.client.ClientBuilder")));
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      "stackstate.trace.instrumentation.jaxrs.ClientTracingFeature",
      "stackstate.trace.instrumentation.jaxrs.ClientTracingFilter",
      "stackstate.trace.instrumentation.jaxrs.InjectAdapter"
    };
  }

  @Override
  public Map<ElementMatcher, String> transformers() {
    Map<ElementMatcher, String> transformers = new HashMap<>();
    transformers.put(
        named("build").and(returns(hasSuperType(named("javax.ws.rs.client.Client")))),
        ClientBuilderAdvice.class.getName());
    return transformers;
  }

  public static class ClientBuilderAdvice {

    @Advice.OnMethodEnter
    public static void registerFeature(@Advice.This final ClientBuilder builder) {
      builder.register(ClientTracingFeature.class);
    }
  }
}
