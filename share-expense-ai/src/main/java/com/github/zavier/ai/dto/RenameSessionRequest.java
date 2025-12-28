package com.github.zavier.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 重命名会话请求
 */
public record RenameSessionRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题长度不能超过200个字符")
        String title
) {
}
