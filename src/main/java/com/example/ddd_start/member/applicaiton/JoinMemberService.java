package com.example.ddd_start.member.applicaiton;

import com.example.ddd_start.common.domain.Address;
import com.example.ddd_start.common.domain.exception.DuplicateEmailException;
import com.example.ddd_start.common.domain.exception.DuplicateUsernameException;
import com.example.ddd_start.common.domain.exception.InvalidUsernameLengthException;
import com.example.ddd_start.member.applicaiton.event.JoinMemberEvent;
import com.example.ddd_start.member.applicaiton.model.AddressCommand;
import com.example.ddd_start.member.applicaiton.model.joinCommand;
import com.example.ddd_start.member.applicaiton.model.joinResponse;
import com.example.ddd_start.member.domain.Member;
import com.example.ddd_start.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JoinMemberService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public joinResponse joinMember(joinCommand req)
      throws DuplicateEmailException, DuplicateUsernameException, InvalidUsernameLengthException {
    //값의 형식 검사
    checkEmpty(req.getEmail(), "email");
    checkEmpty(req.getPassword(), "password");
    checkEmpty(req.getUsername(), "username");
    checkEmpty(req.getRole(), "role");
    checkUsernameLength(req.getUsername());

    //로직 검사
    checkDuplicatedEmail(req.getEmail());
    checkDuplicatedUsername(req.getUsername());

    AddressCommand addressReq = req.getAddressReq();
    Address address = new Address(
        addressReq.getAddress(),
        addressReq.getDetailedAddress(),
        addressReq.getZipCode()
    );

    String encryptedPassword = passwordEncoder.encode(req.getPassword());
    Member member = new Member(req.getUsername(), req.getEmail(), encryptedPassword, req.getName(),
        address,
        req.getRole());

    Member save = memberRepository.save(member);

    eventPublisher.publishEvent(new JoinMemberEvent(save));

    return new joinResponse(member.getId(), member.getUsername());
  }

  private void checkDuplicatedEmail(String email) throws DuplicateEmailException {
    Long count = memberRepository.countByEmail(email);
    if (count > 0) {
      throw new DuplicateEmailException();
    }
  }

  private void checkDuplicatedUsername(String userName) throws DuplicateUsernameException {
    Long count = memberRepository.countByUsername(userName);
    if (count > 0) {
      throw new DuplicateUsernameException();
    }
  }

  private void checkEmpty(String value, String propertyName) {
    if (value == null || value.isEmpty()) {
      throw new NullPointerException(propertyName);
    }
  }

  private void checkUsernameLength(String username) throws InvalidUsernameLengthException {
    if (username != null && username.length() > 10) {
      throw new InvalidUsernameLengthException();
    }
  }
}
