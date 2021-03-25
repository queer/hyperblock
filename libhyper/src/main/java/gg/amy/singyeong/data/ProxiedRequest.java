package gg.amy.singyeong.data;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import gg.amy.singyeong.client.query.Query;
import io.vertx.core.http.HttpMethod;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A proxied request that singyeong can execute on behalf of the client.
 *
 * @author amy
 * @since 6/5/19.
 */
@Value
@Accessors(fluent = true)
@Builder(toBuilder = true)
public final class ProxiedRequest {
    @Default
    private final HttpMethod method = HttpMethod.GET;
    private final String route;
    private final Query query;
    @Default
    private final Multimap<String, String> headers = Multimaps.newMultimap(new HashMap<>(), ArrayList::new);
    private final Object body;
}
