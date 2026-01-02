package com.github.zavier.ai.resolver;

import com.alibaba.cola.dto.PageResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.dto.data.ProjectDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 项目标识符解析器
 * <p>
 * 智能解析项目标识符（ID或名称），支持：
 * <ul>
 *   <li>数字ID：直接解析（如"5"）</li>
 *   <li>项目名称：精确匹配或模糊匹配（如"周末聚餐"）</li>
 * </ul>
 * <p>
 * 基于 Anthropic 最佳实践：优先使用自然语言标识符而非技术ID。
 *
 * @author AI Optimization
 * @since 2025-01-02
 */
@Slf4j
@Component
public class ProjectIdentifierResolver {

    @Resource
    private ProjectService projectService;

    /**
     * 解析项目标识符（ID或名称）
     * <p>
     * 解析策略：
     * <ol>
     *   <li>如果是纯数字，直接作为项目ID返回</li>
     *   <li>否则作为项目名称进行查询</li>
     *   <li>优先精确匹配项目名称</li>
     *   <li>其次模糊匹配（包含关系）</li>
     *   <li>最后返回第一个结果</li>
     * </ol>
     *
     * @param identifier 项目标识符（ID或名称）
     * @param userId     用户ID
     * @return 项目ID，如果未找到返回null
     */
    public Integer resolve(String identifier, Integer userId) {
        if (identifier == null || identifier.isBlank()) {
            log.warn("[项目标识符解析] 标识符为空");
            return null;
        }

        // 1. 尝试解析为数字ID
        if (identifier.matches("\\d+")) {
            try {
                Integer projectId = Integer.parseInt(identifier);
                log.debug("[项目标识符解析] 解析为数字ID: {}", projectId);
                return projectId;
            } catch (NumberFormatException e) {
                log.warn("[项目标识符解析] 数字解析失败: {}", identifier);
                return null;
            }
        }

        // 2. 作为项目名称查找
        return findProjectByName(identifier, userId);
    }

    /**
     * 根据项目名称查找项目ID
     * <p>
     * 查找策略：
     * <ol>
     *   <li>精确匹配：项目名称完全相等</li>
     *   <li>模糊匹配：项目名称包含查询字符串</li>
     *   <li>默认返回：第一个结果</li>
     * </ol>
     *
     * @param projectName 项目名称
     * @param userId     用户ID
     * @return 项目ID，如果未找到返回null
     */
    private Integer findProjectByName(String projectName, Integer userId) {
        ProjectListQry qry = new ProjectListQry();
        qry.setOperatorId(userId);
        qry.setName(projectName);
        qry.setPage(1);
        qry.setSize(10);

        PageResponse<ProjectDTO> response = projectService.pageProject(qry);

        if (!response.isSuccess() || response.getData() == null || response.getData().isEmpty()) {
            log.debug("[项目标识符解析] 未找到项目: projectName={}, userId={}", projectName, userId);
            return null;
        }

        // 1. 精确匹配优先
        for (ProjectDTO project : response.getData()) {
            if (project.getProjectName().equals(projectName)) {
                log.debug("[项目标识符解析] 精确匹配成功: projectName={}, projectId={}",
                        projectName, project.getProjectId());
                return project.getProjectId();
            }
        }

        // 2. 模糊匹配（包含）
        for (ProjectDTO project : response.getData()) {
            if (project.getProjectName().contains(projectName)) {
                log.debug("[项目标识符解析] 模糊匹配成功: query={}, matchedName={}, projectId={}",
                        projectName, project.getProjectName(), project.getProjectId());
                return project.getProjectId();
            }
        }

        // 3. 返回第一个结果
        ProjectDTO firstProject = response.getData().get(0);
        log.debug("[项目标识符解析] 返回第一个结果: query={}, matchedName={}, projectId={}",
                projectName, firstProject.getProjectName(), firstProject.getProjectId());
        return firstProject.getProjectId();
    }
}
