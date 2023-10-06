package com.github.zavier.dto;

import lombok.Data;

@Data
public class UserListQry {
    private String userName;
    private String email;

    private Integer page;
    private Integer size;
}
