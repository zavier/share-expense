package com.github.zavier.ai.function;

import org.springframework.context.annotation.Description;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Description("")
public @interface AiFunction {
    String name();
    String description();
}
