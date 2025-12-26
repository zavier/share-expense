package com.github.zavier.ai.function;

public interface AiFunctionExecutor {
    String execute(Object request, FunctionContext context);
    Class<?> getRequestType();
}
