package com.github.zavier.mobile;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.Assert;
import com.alibaba.fastjson.JSON;
import com.github.zavier.api.ProjectService;
import com.github.zavier.api.UserService;
import com.github.zavier.domain.utils.ShareTokenHelper;
import com.github.zavier.dto.ExpenseRecordQry;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.dto.ProjectSharingQry;
import com.github.zavier.dto.data.ExpenseRecordDTO;
import com.github.zavier.dto.data.ProjectDTO;
import com.github.zavier.dto.data.UserSharingDTO;
import com.github.zavier.dto.wx.ProjectShareDTO;
import com.github.zavier.vo.SingleResponseVo;
import com.github.zavier.web.filter.UserHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户微信小程序相关功能
 *
 */
@RestController
@RequestMapping("/expense/wx")
public class CustomerMobileAdaptor {


    @Resource
    private UserService userService;
    @Resource
    private ProjectService projectService;

    @GetMapping("/user/login")
    public SingleResponseVo wxLogin(@RequestParam String code, HttpServletResponse httpServletResponse) throws IOException {
        if (StringUtils.isBlank(code)) {
            return SingleResponseVo.buildFailure("WX_LOGIN_FAILED", "微信登录失败");
        }

        final SingleResponse<String> tokenResp = userService.wxLogin(code);
        if (!tokenResp.isSuccess()) {
            return SingleResponseVo.buildFailure("WX_LOGIN_FAILED", "微信登录失败");
        }

        // 临时方案
        final Cookie cookie = new Cookie("jwtToken", tokenResp.getData());
        cookie.setPath("/");
        cookie.setMaxAge((int) TimeUnit.DAYS.toSeconds(30));
        httpServletResponse.addCookie(cookie);
        return SingleResponseVo.of(tokenResp.getData());
    }

    @GetMapping("/project/shareToken")
    public SingleResponseVo generateProjectShareToken(@RequestParam Integer projectId) {
        final int userId = UserHolder.getUser().getUserId();
        final ProjectShareDTO projectShareDTO = new ProjectShareDTO();
        projectShareDTO.setProjectId(projectId);
        projectShareDTO.setUserId(userId);

        final boolean userCreate = checkProjectIsUserCreate(projectId, userId);
        if (!userCreate) {
            return SingleResponseVo.buildFailure("PROJECT_NOT_FOUND", "项目不存在");
        }

        final String shareToken = ShareTokenHelper.generateShareToken(JSON.toJSONString(projectShareDTO));
        return SingleResponseVo.of(shareToken);
    }

    @GetMapping("/project/listRecord/share")
    public SingleResponseVo<Map<String, List<ExpenseRecordDTO>>> listShareRecord(@RequestParam String shareToken) {
        final boolean valid = ShareTokenHelper.validateShareToken(shareToken);
        if (!valid) {
            return SingleResponseVo.buildFailure("SHARE_TOKEN_INVALID", "分享链接已失效");
        }

        final ProjectShareDTO projectShareDTO = getProjectShareDTO(shareToken);

        final ExpenseRecordQry expenseRecordQry = new ExpenseRecordQry();
        expenseRecordQry.setProjectId(projectShareDTO.getProjectId());
        expenseRecordQry.setOperatorId(projectShareDTO.getUserId());
        final SingleResponse<List<ExpenseRecordDTO>> listSingleResponse = projectService.listRecord(expenseRecordQry);
        Map<String, List<ExpenseRecordDTO>> map = new HashMap<>();
        map.put("rows", listSingleResponse.getData());
        return SingleResponseVo.of(map);
    }

    @GetMapping("/project/sharing/share")
    public SingleResponseVo getProjectSharingDetail(@RequestParam String shareToken) {
        final boolean valid = ShareTokenHelper.validateShareToken(shareToken);
        if (!valid) {
            return SingleResponseVo.buildFailure("SHARE_TOKEN_INVALID", "分享链接已失效");
        }
        final ProjectShareDTO projectShareDTO = getProjectShareDTO(shareToken);

        final ProjectSharingQry projectSharingQry = new ProjectSharingQry();
        projectSharingQry.setProjectId(projectShareDTO.getProjectId());
        projectSharingQry.setOperatorId(projectShareDTO.getUserId());

        final SingleResponse<List<UserSharingDTO>> projectSharingDetail = projectService.getProjectSharingDetail(projectSharingQry);
        if (!projectSharingDetail.isSuccess()) {
            return SingleResponseVo.buildFailure(projectSharingDetail.getErrCode(), projectSharingDetail.getErrMessage());
        }
        Map<String, Object> map = new HashMap<>();
        map.put("rows", projectSharingDetail.getData());
        return SingleResponseVo.of(map);
    }

    private boolean checkProjectIsUserCreate(Integer projectId, int userId) {
        final ProjectListQry projectListQry = new ProjectListQry();
        projectListQry.setOperatorId(userId);
        projectListQry.setPage(1);
        projectListQry.setSize(10);
        projectListQry.setId(projectId);
        final PageResponse<ProjectDTO> projectDTOPageResponse = projectService.pageProject(projectListQry);
        if (projectDTOPageResponse.getData().isEmpty()) {
            return false;
        }
        final int size = projectDTOPageResponse.getData().size();
        if (size != 1) {
            return false;
        }
        return projectDTOPageResponse.getData().get(0).getProjectId().equals(projectId);
    }

    private static ProjectShareDTO getProjectShareDTO(String shareToken) {
        final String shareTokenBody = ShareTokenHelper.getShareTokenBody(shareToken);
        final ProjectShareDTO projectShareDTO = JSON.parseObject(shareTokenBody, ProjectShareDTO.class);
        Assert.notNull(projectShareDTO, "SHARE_TOKEN_INVALID", "分享链接已失效");
        Assert.notNull(projectShareDTO.getProjectId(), "SHARE_TOKEN_INVALID", "分享链接已失效");
        Assert.notNull(projectShareDTO.getUserId(), "SHARE_TOKEN_INVALID", "分享链接已失效");
        return projectShareDTO;
    }
}
