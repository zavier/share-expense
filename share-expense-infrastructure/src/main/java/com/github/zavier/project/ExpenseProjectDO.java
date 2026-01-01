package com.github.zavier.project;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "expense_project")
public class ExpenseProjectDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "create_user_id")
    private Integer createUserId;

    @Column(name = "locked")
    private Boolean locked;

    @Column(name = "description")
    private String description;

    @Column(name = "version")
    private Integer version;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;
}
