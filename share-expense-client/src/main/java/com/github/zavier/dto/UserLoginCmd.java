package com.github.zavier.dto;

import lombok.Data;

@Data
public class UserLoginCmd {

    private String username;
    private String password;
}
