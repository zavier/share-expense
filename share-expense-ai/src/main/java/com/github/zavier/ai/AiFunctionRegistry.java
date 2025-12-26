package com.github.zavier.ai;

import com.github.zavier.ai.function.AiFunction;
import com.github.zavier.ai.function.AiFunctionExecutor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AiFunctionRegistry {

    private final Map<String, AiFunctionExecutor> functions = new HashMap<>();
    private final Map<String, Class<?>> requestTypes = new HashMap<>();

    public AiFunctionRegistry(List<AiFunctionExecutor> functionExecutors) {
        for (AiFunctionExecutor executor : functionExecutors) {
            AiFunction annotation = executor.getClass().getAnnotation(AiFunction.class);
            if (annotation != null) {
                String name = annotation.name();
                functions.put(name, executor);
                requestTypes.put(name, executor.getRequestType());
            }
        }
    }

    public AiFunctionExecutor getFunction(String name) {
        return functions.get(name);
    }

    public Class<?> getRequestType(String name) {
        return requestTypes.get(name);
    }

    public Map<String, String> getFunctionDescriptions() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        for (AiFunctionExecutor executor : functions.values()) {
            AiFunction annotation = executor.getClass().getAnnotation(AiFunction.class);
            if (annotation != null) {
                descriptions.put(annotation.name(), annotation.description());
            }
        }
        return descriptions;
    }
}
