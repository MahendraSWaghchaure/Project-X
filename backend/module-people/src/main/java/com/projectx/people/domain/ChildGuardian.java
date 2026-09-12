package com.projectx.people.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A guardian beyond the child's primary one - see V25__child_guardians.sql. Child.primaryParentId
 * stays a plain column (unchanged from before this table existed) since it's still the one
 * guardian route auto-allocation and fee-invoice billing key off; this table is purely additive.
 */
@Entity
@Table(name = "child_guardians", uniqueConstraints = @UniqueConstraint(columnNames = {"child_id", "parent_id"}))
public class ChildGuardian extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    protected ChildGuardian() {
    }

    public ChildGuardian(Long childId, Long parentId) {
        this.childId = childId;
        this.parentId = parentId;
    }

    public Long getId() {
        return id;
    }

    public Long getChildId() {
        return childId;
    }

    public Long getParentId() {
        return parentId;
    }
}
