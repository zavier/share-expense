package com.github.zavier.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserListQry {
    private Integer userId;
    private List<Integer> userIdList;

    private String userName;
    private String email;

    private Integer page;
    private Integer size;
}
