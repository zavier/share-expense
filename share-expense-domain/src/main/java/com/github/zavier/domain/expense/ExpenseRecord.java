package com.github.zavier.domain.expense;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.common.ChangingStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ExpenseRecord {

    @Getter
    @Setter
    private Integer id;
    @Getter
    @Setter
    private Integer projectId;

    @Getter
    @Setter
    private Integer costUserId;
    @Getter
    @Setter
    private String costUserName;
    @Getter
    @Setter
    private BigDecimal amount;
    @Getter
    @Setter
    private Date date;
    @Getter
    @Setter
    private String expenseType;
    @Getter
    @Setter
    private String remark;

    /**
     * 版本号
     */
    @Getter
    @Setter
    private Integer version;

    @Getter
    @Setter
    private ChangingStatus changingStatus = ChangingStatus.NEW;

    /**
     * 是否需要分摊
     */
    @Getter
    @Setter
    private Boolean needSharding = false;

    private Map<Integer, ExpenseSharing> userIdSharingMap = new HashMap<>();


    public void addUserSharing(Integer userId, Integer weight) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(weight, "权重不能为空");

        Assert.isFalse(userIdSharingMap.containsKey(userId), "用户ID重复");
        userIdSharingMap.put(userId, new ExpenseSharing(userId, weight));

        calcSharingAmount();
    }


    public void updateUserWeight(Integer userId, Integer weight) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(weight, "权重不能为空");

        Assert.isTrue(userIdSharingMap.containsKey(userId), "用户权重不存在");
        userIdSharingMap.get(userId).setWeight(weight);

        calcSharingAmount();
    }

    public void removeUserSharing(Integer userId) {
        Assert.notNull(userId, "用户ID不能为空");

        Assert.isTrue(userIdSharingMap.containsKey(userId), "用户权重不存在");
        userIdSharingMap.remove(userId);

        calcSharingAmount();
    }

    private void calcSharingAmount() {
        if (!hasSharing()) {
            return;
        }

        final Integer totalWeight = userIdSharingMap.values().stream()
                .map(ExpenseSharing::getWeight)
                .reduce(0, Integer::sum);
        userIdSharingMap.values().forEach(expenseSharing ->
                expenseSharing.setAmount(amount.multiply(new BigDecimal(expenseSharing.getWeight())).divide(new BigDecimal(totalWeight), 2, RoundingMode.HALF_UP)));
    }

    public boolean hasSharing() {
        return !userIdSharingMap.isEmpty();
    }

    public Map<Integer, ExpenseSharing> getUserIdSharingMap() {
        return Collections.unmodifiableMap(userIdSharingMap);
    }

}