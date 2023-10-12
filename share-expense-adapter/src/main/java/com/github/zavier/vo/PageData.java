package com.github.zavier.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageData<T> {
    private List<T> items;
    private Integer total;

    public static <T> PageData<T> empty() {
        return new PageData<>(Collections.emptyList(), 0);
    }
}
