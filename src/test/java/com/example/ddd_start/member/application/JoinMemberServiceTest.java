package com.example.ddd_start.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ddd_start.common.domain.Address;
import com.example.ddd_start.common.domain.exception.InvalidUsernameLengthException;
import com.example.ddd_start.member.applicaiton.JoinMemberService;
import com.example.ddd_start.member.applicaiton.model.AddressCommand;
import com.example.ddd_start.member.applicaiton.model.joinCommand;
import com.example.ddd_start.member.applicaiton.model.joinResponse;
import com.example.ddd_start.member.domain.Member;
import com.example.ddd_start.member.domain.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("JoinMemberService 단위 테스트")
class JoinMemberServiceTest {

  @Mock
  private MemberRepository memberRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private JoinMemberService joinMemberService;

  private AddressCommand validAddressCommand;

  @BeforeEach
  void setUp() {
    validAddressCommand = new AddressCommand("서울시 강남구", "테헤란로 123", 6234);
  }

  @Test
  @DisplayName("정상 케이스 - username이 10자 이하일 때 회원가입 성공")
  void shouldJoinSuccessfully_whenUsernameIs10CharsOrLess() throws Throwable {
    // given
    String validUsername = "user123456"; // 10자
    joinCommand command = new joinCommand(
        "test@example.com",
        "password123",
        validUsername,
        "테스트유저",
        validAddressCommand,
        "USER"
    );

    when(memberRepository.countByEmail(anyString())).thenReturn(0L);
    when(memberRepository.countByUsername(anyString())).thenReturn(0L);
    when(passwordEncoder.encode(anyString())).thenReturn("encryptedPassword");
    when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
      Member member = invocation.getArgument(0);
      return Member.builder()
          .id(1L)
          .username(member.getUsername())
          .email(member.getEmail())
          .password(member.getPassword())
          .name(member.getName())
          .address(member.getAddress())
          .blocked(false)
          .build();
    });

    // when
    joinResponse response = joinMemberService.joinMember(command);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getName()).isEqualTo(validUsername);
    verify(memberRepository, times(1)).save(any(Member.class));
  }

  @Test
  @DisplayName("정상 케이스 - username이 정확히 10자일 때 성공")
  void shouldJoinSuccessfully_whenUsernameIsExactly10Chars() throws Throwable {
    // given
    String exactlyTenChars = "username10"; // 정확히 10자
    joinCommand command = new joinCommand(
        "test@example.com",
        "password123",
        exactlyTenChars,
        "테스트유저",
        validAddressCommand,
        "USER"
    );

    when(memberRepository.countByEmail(anyString())).thenReturn(0L);
    when(memberRepository.countByUsername(anyString())).thenReturn(0L);
    when(passwordEncoder.encode(anyString())).thenReturn("encryptedPassword");
    when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
      Member member = invocation.getArgument(0);
      return Member.builder()
          .id(1L)
          .username(member.getUsername())
          .build();
    });

    // when
    joinResponse response = joinMemberService.joinMember(command);

    // then
    assertThat(response).isNotNull();
    verify(memberRepository, times(1)).save(any(Member.class));
  }

  @Test
  @DisplayName("정상 케이스 - username이 10자 미만일 때 성공")
  void shouldJoinSuccessfully_whenUsernameIsLessThan10Chars() throws Throwable {
    // given
    String shortUsername = "user"; // 4자
    joinCommand command = new joinCommand(
        "test@example.com",
        "password123",
        shortUsername,
        "테스트유저",
        validAddressCommand,
        "USER"
    );

    when(memberRepository.countByEmail(anyString())).thenReturn(0L);
    when(memberRepository.countByUsername(anyString())).thenReturn(0L);
    when(passwordEncoder.encode(anyString())).thenReturn("encryptedPassword");
    when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
      Member member = invocation.getArgument(0);
      return Member.builder()
          .id(1L)
          .username(member.getUsername())
          .build();
    });

    // when
    joinResponse response = joinMemberService.joinMember(command);

    // then
    assertThat(response).isNotNull();
    verify(memberRepository, times(1)).save(any(Member.class));
  }

  @Test
  @DisplayName("실패 케이스 - username이 10자 초과 시 InvalidUsernameLengthException 발생")
  void shouldThrowInvalidUsernameLengthException_whenUsernameExceeds10Chars() {
    // given
    String tooLongUsername = "verylongusername123"; // 19자
    joinCommand command = new joinCommand(
        "test@example.com",
        "password123",
        tooLongUsername,
        "테스트유저",
        validAddressCommand,
        "USER"
    );

    // when & then
    assertThatThrownBy(() -> joinMemberService.joinMember(command))
        .isInstanceOf(InvalidUsernameLengthException.class)
        .hasMessage("Username must be 10 characters or less");

    // Member 저장이 호출되지 않아야 함
    verify(memberRepository, never()).save(any(Member.class));
  }

  @Test
  @DisplayName("실패 케이스 - username이 정확히 11자일 때 예외 발생")
  void shouldThrowInvalidUsernameLengthException_whenUsernameIs11Chars() {
    // given
    String elevenCharsUsername = "username123"; // 11자
    joinCommand command = new joinCommand(
        "test@example.com",
        "password123",
        elevenCharsUsername,
        "테스트유저",
        validAddressCommand,
        "USER"
    );

    // when & then
    assertThatThrownBy(() -> joinMemberService.joinMember(command))
        .isInstanceOf(InvalidUsernameLengthException.class);

    verify(memberRepository, never()).save(any(Member.class));
  }

  @Test
  @DisplayName("실패 케이스 - username이 null일 때 NullPointerException 발생")
  void shouldThrowNullPointerException_whenUsernameIsNull() {
    // given
    joinCommand command = new joinCommand(
        "test@example.com",
        "password123",
        null,
        "테스트유저",
        validAddressCommand,
        "USER"
    );

    // when & then
    assertThrows(NullPointerException.class, () -> joinMemberService.joinMember(command));

    verify(memberRepository, never()).save(any(Member.class));
  }

  @Test
  @DisplayName("실패 케이스 - username이 빈 문자열일 때 NullPointerException 발생")
  void shouldThrowNullPointerException_whenUsernameIsEmpty() {
    // given
    joinCommand command = new joinCommand(
        "test@example.com",
        "password123",
        "",
        "테스트유저",
        validAddressCommand,
        "USER"
    );

    // when & then
    assertThrows(NullPointerException.class, () -> joinMemberService.joinMember(command));

    verify(memberRepository, never()).save(any(Member.class));
  }

  @Test
  @DisplayName("경계값 테스트 - 한글 username 10자 성공")
  void shouldJoinSuccessfully_whenKoreanUsernameIs10Chars() throws Throwable {
    // given
    String koreanUsername = "사용자이름테스트열"; // 10자
    joinCommand command = new joinCommand(
        "test@example.com",
        "password123",
        koreanUsername,
        "테스트유저",
        validAddressCommand,
        "USER"
    );

    when(memberRepository.countByEmail(anyString())).thenReturn(0L);
    when(memberRepository.countByUsername(anyString())).thenReturn(0L);
    when(passwordEncoder.encode(anyString())).thenReturn("encryptedPassword");
    when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
      Member member = invocation.getArgument(0);
      return Member.builder()
          .id(1L)
          .username(member.getUsername())
          .build();
    });

    // when
    joinResponse response = joinMemberService.joinMember(command);

    // then
    assertThat(response).isNotNull();
    verify(memberRepository, times(1)).save(any(Member.class));
  }

  @Test
  @DisplayName("경계값 테스트 - 한글 username 11자 실패")
  void shouldThrowException_whenKoreanUsernameIs11Chars() {
    // given
    String koreanUsername = "사용자이름테스트열자이"; // 11자
    joinCommand command = new joinCommand(
        "test@example.com",
        "password123",
        koreanUsername,
        "테스트유저",
        validAddressCommand,
        "USER"
    );

    // when & then
    assertThatThrownBy(() -> joinMemberService.joinMember(command))
        .isInstanceOf(InvalidUsernameLengthException.class);

    verify(memberRepository, never()).save(any(Member.class));
  }
}
