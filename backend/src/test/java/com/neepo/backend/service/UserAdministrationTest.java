package com.neepo.backend.service;

import com.neepo.backend.domain.UserRole;
import com.neepo.backend.dto.AcceptInvitationRequest;
import com.neepo.backend.dto.CreateUserInvitationRequest;
import com.neepo.backend.dto.UpdateUserRequest;
import com.neepo.backend.dto.UserInvitationResponse;
import com.neepo.backend.entity.Company;
import com.neepo.backend.entity.User;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.repository.CompanyRepository;
import com.neepo.backend.repository.UserInvitationRepository;
import com.neepo.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class UserAdministrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserInvitationService invitationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInvitationRepository invitationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private UUID companyId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = Company.create(
                "Test Logistics " + suffix,
                "TEST-" + suffix,
                "Test Logistics Legal " + suffix,
                "Test Logistics Trading " + suffix,
                "REG-" + suffix,
                "Logistics",
                "123 Test Way",
                "Test City",
                "CA",
                "US",
                "94000",
                "Admin Contact",
                "admin-" + suffix + "@testcorp.local",
                "+15550100000",
                "UTC",
                "METRIC",
                "STRICT_COLD_CHAIN",
                null,
                90
        );
        company = companyRepository.save(company);
        companyId = company.getId();
    }

    @Test
    void cannotDemoteOrSuspendLastActiveAdministrator() {
        User admin = userRepository.save(User.createForCompany(
                companyId, "Sole Admin", "soleadmin-" + UUID.randomUUID() + "@test.com", "+15550109999", UserRole.ADMIN, "hashedPassword"
        ));

        // Attempt to demote sole admin to DISPATCHER
        UpdateUserRequest demoteReq = new UpdateUserRequest(
                "Sole Admin", "+15550109999", UserRole.DISPATCHER
        );
        assertThatThrownBy(() -> userService.updateUser(companyId, admin.getId(), admin.getId(), UUID.randomUUID(), demoteReq))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Cannot demote the last active administrator");

        // Attempt to suspend sole admin
        assertThatThrownBy(() -> userService.suspendUser(companyId, admin.getId(), admin.getId(), UUID.randomUUID()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Cannot suspend the last active administrator");

        // Attempt to delete sole admin
        assertThatThrownBy(() -> userService.deleteUser(companyId, admin.getId(), admin.getId(), UUID.randomUUID()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Cannot delete the last active administrator");
    }

    @Test
    void canDemoteAdminIfAnotherActiveAdminExists() {
        User admin1 = userRepository.save(User.createForCompany(
                companyId, "Admin One", "admin1-" + UUID.randomUUID() + "@test.com", "+15550109991", UserRole.ADMIN, "hashedPassword"
        ));
        User admin2 = userRepository.save(User.createForCompany(
                companyId, "Admin Two", "admin2-" + UUID.randomUUID() + "@test.com", "+15550109992", UserRole.ADMIN, "hashedPassword"
        ));

        UpdateUserRequest demoteReq = new UpdateUserRequest(
                "Admin One", "+15550109991", UserRole.DISPATCHER
        );
        var updated = userService.updateUser(companyId, admin1.getId(), admin2.getId(), UUID.randomUUID(), demoteReq);
        assertThat(updated.role()).isEqualTo(UserRole.DISPATCHER);
    }

    @Test
    void createAndAcceptInvitationFlow() {
        User admin = userRepository.save(User.createForCompany(
                companyId, "Admin Inviter", "inviter-" + UUID.randomUUID() + "@test.com", "+15550109990", UserRole.ADMIN, "hashedPassword"
        ));

        String inviteEmail = "newmember-" + UUID.randomUUID() + "@test.com";

        // Create Invitation
        CreateUserInvitationRequest inviteReq = new CreateUserInvitationRequest(
                "New Member", inviteEmail, "+15550108888", UserRole.DISPATCHER
        );
        UserInvitationResponse invitation = invitationService.inviteUser(
                companyId, admin.getId(), UserRole.ADMIN, UUID.randomUUID(), inviteReq
        );

        assertThat(invitation.token()).isNotNull();
        assertThat(invitation.email()).isEqualTo(inviteEmail);

        // Accept Invitation
        var acceptedUser = invitationService.acceptInvitation(
                new AcceptInvitationRequest(invitation.token(), "SecureNewPassword123!"), UUID.randomUUID()
        );
        assertThat(acceptedUser.email()).isEqualTo(inviteEmail);
        assertThat(acceptedUser.role()).isEqualTo(UserRole.DISPATCHER);

        // Try accepting again (should fail)
        assertThatThrownBy(() -> invitationService.acceptInvitation(
                new AcceptInvitationRequest(invitation.token(), "AnotherPassword123!"), UUID.randomUUID()
        ))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already been accepted");
    }
}
