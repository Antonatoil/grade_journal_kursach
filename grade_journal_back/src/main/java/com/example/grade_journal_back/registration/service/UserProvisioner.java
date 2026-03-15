package com.example.grade_journal_back.registration.service;

import com.example.grade_journal_back.user.entity.UserAccount;

public interface UserProvisioner {

    String supportedRole();

    void provision(UserAccount userAccount);
}