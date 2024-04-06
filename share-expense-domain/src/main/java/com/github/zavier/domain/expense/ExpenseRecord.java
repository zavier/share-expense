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

    private Map<Integer, ExpenseSharing> userIdSharingMap = new HashMap<>();
    private Map<Integer, String> consumerIdMap = new HashMap<>();

    @Getter
    @Setter
    private Integer id;
    @Getter
    @Setter
    private Integer projectId;

    @Getter
    @Setter
    private Integer payUserId;
    @Getter
    @Setter
    private String payUserName;
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

    public void addConsumer(Integer consumerId, String consumerName) {
        checkConsumerNameExist(consumerName);

        final String old = consumerIdMap.put(consumerId, consumerName);
        Assert.isTrue(old == null, "消费人已存在");
    }

    private void checkConsumerNameExist(String consumerName) {
        if (consumerIdMap.isEmpty()) {
            return;
        }
        final boolean hasSame = consumerIdMap.values().stream().anyMatch(it -> it.equals(consumerName));
        Assert.isFalse(hasSame, "消费人名称重复");
    }

    public Map<Integer, String> getConsumerIdMap() {
        return Collections.unmodifiableMap(consumerIdMap);
    }

    // TODO 权重废弃？
    public void addUserSharing(Integer userId, String userName, Integer weight) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(userName, "用户名称不能为空");
        Assert.notNull(weight, "权重不能为空");

        Assert.isFalse(userIdSharingMap.containsKey(userId), "用户ID重复");
        userIdSharingMap.put(userId, new ExpenseSharing(userId, userName, weight));

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