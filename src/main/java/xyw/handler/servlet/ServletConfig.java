package xyw.handler.servlet;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ServletConfig {
    final String workPath;
    final String context;
    final boolean defaultReturn;
    final boolean useCache;
    final boolean useAction;
}
