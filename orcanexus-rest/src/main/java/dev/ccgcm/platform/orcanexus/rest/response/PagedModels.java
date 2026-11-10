package dev.ccgcm.platform.orcanexus.rest.response;

import dev.ccgcm.platform.orcanexus.domain.model.PageResult;
import java.util.function.Function;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;

/** Converts the framework-free domain {@link PageResult} into the REST {@link PagedModel}. */
public final class PagedModels {

  private PagedModels() {}

  public static <T, R> PagedModel<R> from(PageResult<T> result, Function<T, R> contentMapper) {
    var content = result.content().stream().map(contentMapper).toList();

    return new PagedModel<>(
        new PageImpl<>(
            content, PageRequest.of(result.page(), result.size()), result.totalElements()));
  }
}
