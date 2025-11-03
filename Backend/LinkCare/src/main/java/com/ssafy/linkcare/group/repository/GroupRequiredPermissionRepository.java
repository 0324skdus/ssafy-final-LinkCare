package com.ssafy.linkcare.group.repository;

import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.entity.GroupRequiredPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRequiredPermissionRepository extends JpaRepository<GroupRequiredPermission, Long> {

    Optional<GroupRequiredPermission> findByGroup(Group group);
}
