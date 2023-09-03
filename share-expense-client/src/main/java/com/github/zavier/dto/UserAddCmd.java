package com.github.zavier.dto;

import com.github.zavier.dto.data.UserDto;
import lombok.Data;

@Data
public class UserAddCmd {

    private String username;
    private String email;
    private String password;
}
