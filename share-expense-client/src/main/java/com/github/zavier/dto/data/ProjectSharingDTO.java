package com.github.zavier.dto.data;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectSharingDTO {

    private List<UserSharingDTO> userSharingList = new ArrayList<>();

}
