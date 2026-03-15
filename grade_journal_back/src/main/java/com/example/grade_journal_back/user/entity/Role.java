package com.example.grade_journal_back.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "role_code", nullable = false, unique = true, length = 20)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;
}