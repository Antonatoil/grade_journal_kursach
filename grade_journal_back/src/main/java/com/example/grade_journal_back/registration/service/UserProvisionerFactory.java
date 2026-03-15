package com.example.grade_journal_back.registration.service;

import com.example.grade_journal_back.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserProvisionerFactory {

    private final List<UserProvisioner> provisioners;

    public UserProvisionerFactory(List<UserProvisioner> provisioners) {
        this.provisioners = provisioners;
    }

    public UserProvisioner getByRole(String roleCode) {
        return provisioners.stream()
            .filter(provisioner -> provisioner.supportedRole().equals(roleCode))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Не найден обработчик роли: " + roleCode));
    }
}