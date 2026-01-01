package com.github.zavier.expense;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "expense_record")
public class ExpenseRecordDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "pay_member")
    private String payMember;

    @Column(name = "project_id")
    private Integer projectId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "pay_date")
    private Date payDate;

    @Column(name = "expense_type")
    private String expenseType;

    @Column(name = "remark")
    private String remark;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;
}