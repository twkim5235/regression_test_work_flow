package com.example.ddd_start.member.presentation;

import com.example.ddd_start.auth.model.JwtToken;
import com.example.ddd_start.common.domain.exception.DuplicateEmailException;
import com.example.ddd_start.common.domain.exception.DuplicateUsernameException;
import com.example.ddd_start.common.domain.exception.InvalidUsernameLengthException;
import com.example.ddd_start.common.domain.exception.NoMemberFoundException;
import com.example.ddd_start.common.domain.exception.PasswordNotMatchException;
import com.example.ddd_start.member.applicaiton.ChangePasswordService;
import com.example.ddd_start.member.applicaiton.DeleteMemberService;
import com.example.ddd_start.member.applicaiton.JoinMemberService;
import com.example.ddd_start.member.applicaiton.MemberService;
import com.example.ddd_start.member.applicaiton.UpdateMemberService;
import com.example.ddd_start.member.applicaiton.model.*;
import com.example.ddd_start.member.presentation.model.ChangePasswordRequest;
import com.example.ddd_start.member.presentation.model.JoinMemberRequest;
import com.example.ddd_start.member.presentation.model.MemberDto;
import com.example.ddd_start.member.presentation.model.MemberResponse;
import com.example.ddd_start.member.presentation.model.SignInRequest;
import com.example.ddd_start.member.presentation.model.UpdateMemberRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MemberController {

  private final JoinMemberService joinMemberService;
  private final ChangePasswordService changePasswordService;
  private final MemberService memberService;
  private final DeleteMemberService deleteMemberService;
  private final UpdateMemberService updateMemberService;

  @PostMapping("/members/join")
  public ResponseEntity join(@RequestBody JoinMemberRequest req, Errors errors) {
    String email = req.getEmail();
    String password = req.getPassword();
    String username = req.getUsername();
    String name = req.getName();
    AddressCommand addressReq = req.getAddressReq();

    try {
      joinResponse joinResponse = joinMemberService.joinMember(
          new joinCommand(email, password, username, name, addressReq, "USER"));

      return new ResponseEntity<MemberResponse>(
          new MemberResponse(
              joinResponse.getMemberId(), joinResponse.getName(), "회원가입을 축하드립니다."),
          HttpStatus.ACCEPTED);
    } catch (DuplicateEmailException e) {
      errors.rejectValue(e.getMessage(), "duplicate");
      return new ResponseEntity("이메일이 중복됩니다.", HttpStatus.BAD_REQUEST);
    } catch (DuplicateUsernameException e) {
      errors.rejectValue(e.getMessage(), "duplicate");
      return new ResponseEntity("아이디가 중복됩니다.", HttpStatus.BAD_REQUEST);
    } catch (InvalidUsernameLengthException e) {
      return new ResponseEntity("아이디는 10자 이하로 입력해주세요.", HttpStatus.BAD_REQUEST);
    }
  }

  @PostMapping("/members/join/admin")
  public ResponseEntity joinAdmin(@RequestBody JoinMemberRequest req, Errors errors) {
    String email = req.getEmail();
    String password = req.getPassword();
    String username = req.getUsername();
    String name = req.getName();
    AddressCommand addressReq = req.getAddressReq();

    try {
      joinResponse joinResponse = joinMemberService.joinMember(
          new joinCommand(email, password, username, name, addressReq, "ADMIN"));

      return new ResponseEntity<MemberResponse>(
          new MemberResponse(
              joinResponse.getMemberId(), joinResponse.getName(), "회원가입을 축하드립니다."),
          HttpStatus.ACCEPTED);
    } catch (DuplicateEmailException e) {
      errors.rejectValue(e.getMessage(), "duplicate");
      return new ResponseEntity("이메일이 중복됩니다.", HttpStatus.BAD_REQUEST);
    } catch (DuplicateUsernameException e) {
      errors.rejectValue(e.getMessage(), "duplicate");
      return new ResponseEntity("아이디가 중복됩니다.", HttpStatus.BAD_REQUEST);
    } catch (InvalidUsernameLengthException e) {
      return new ResponseEntity("아이디는 10자 이하로 입력해주세요.", HttpStatus.BAD_REQUEST);
    }
  }

  @PutMapping("/members/update")
  public ResponseEntity updateMember(@RequestBody UpdateMemberRequest req) {
    Long id = req.id();
    String email = req.email();
    String username = req.username();
    String name = req.name();
    AddressCommand addressReq = req.addressReq();

    try {
      updateMemberService.updateMember(
          new UpdateCommand(id, email, username, name, addressReq));
      return new ResponseEntity("회원정보가 정상적으로 변경되었습니다.", HttpStatus.ACCEPTED);
    } catch (NotFoundException e) {
      return new ResponseEntity("회원정보가 존재하지 않습니다.", HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping("/members/change-password")
  public ResponseEntity changePassword(@RequestBody ChangePasswordRequest req)
      throws NoMemberFoundException {
    try {
      changePasswordService.changePassword(
          new ChangePasswordCommand(
              req.memberId(),
              req.curPw(),
              req.newPw()));

      return new ResponseEntity<>(HttpStatus.ACCEPTED);
    } catch (PasswordNotMatchException e) {
      return new ResponseEntity<>("이전 비밀번호가 일치 하지 않습니다.", HttpStatus.BAD_REQUEST);
    }
  }

  @PostMapping("/members/sign-in")
  public ResponseEntity signIn(@RequestBody SignInRequest req) {
    JwtToken jwtToken = memberService.signIn(new SignInCommand(
        req.username(),
        req.password()
    ));
    return new ResponseEntity(jwtToken, HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/members/{memberId}")
  public ResponseEntity delete(@PathVariable Long memberId) {
    try {
      deleteMemberService.delete(memberId);
      return new ResponseEntity("회원탈퇴가 정상적으로 이루어졌습니다.", HttpStatus.ACCEPTED);
    } catch (EmptyResultDataAccessException e) {
      return new ResponseEntity("해당 회원은 존재하지 않는 회원입니다.", HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping("/get-current-member")
  public ResponseEntity getCurrentMember(Authentication authentication) {
    MemberDto memberDto = memberService.getMember(authentication.getName());
    return ResponseEntity.ok(memberDto);
  }
}
