package com.github.zavier.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingAction {
    private String actionId;
    private String actionType;  // createProject, addMembers, addExpenseRecord, getSettlement
    private String description;
    private Map<String, Object> params;
}
