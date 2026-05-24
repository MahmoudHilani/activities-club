package com.activitiesclub.activitiesclub_backend;

import java.util.EnumSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.activitiesclub.activitiesclub_backend.auth.AuthenticatedUser;
import com.activitiesclub.activitiesclub_backend.dto.SportsClubSignupRequest;
import com.activitiesclub.activitiesclub_backend.dto.SportsClubSignupResponse;

@Service
public class SportsClubSignupService {
    private final SportsClubSignupRepository signupRepository;
    private final UserRepository userRepository;

    public SportsClubSignupService(SportsClubSignupRepository signupRepository, UserRepository userRepository) {
        this.signupRepository = signupRepository;
        this.userRepository = userRepository;
    }

    public SportsClubSignupResponse submit(SportsClubSignupRequest request, AuthenticatedUser currentUser) {
        SportsClubSignup signup = new SportsClubSignup();
        signup.setName(request.name().trim());
        signup.setEmail(request.email().trim().toLowerCase());
        signup.setPhoneNumber(request.phoneNumber().trim());
        signup.setStudentNumber(request.studentNumber().trim());
        signup.setCourse(request.course().trim());
        signup.setGender(request.gender());
        signup.setSportsClubs(EnumSet.copyOf(request.sportsClubs()));

        if (currentUser != null) {
            userRepository.findById(currentUser.id()).ifPresent(signup::setUser);
        }

        return SportsClubSignupResponse.from(signupRepository.save(signup));
    }

    public Page<SportsClubSignupResponse> listAdmin(Pageable pageable) {
        return signupRepository.findAllByOrderByCreatedAtDesc(pageable).map(SportsClubSignupResponse::from);
    }
}
