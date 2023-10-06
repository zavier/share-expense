package com.github.zavier.dto;

import lombok.Data;

@Data
public class UserAddCmd {

    private String username;
    private String email;
    private String password;
}
