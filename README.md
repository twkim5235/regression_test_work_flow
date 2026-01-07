# Readme.md



### 본 프로젝트는 테스트 워크플로우를 적용하기 위한 프로젝트입니다.



### 프로젝트 설명

- Spring boot 2.7.2, Java 17
- DDD를 기반으로 한, 쇼핑몰 서비스
- 초기에는 직접 코딩을 했으나, 후반은 claude code를 활용하여 진행함
  - 웹 템플릿은 모두, claude code를 활용
- Claude code를 이용하여, PR-리그레션-테스트-워크플로우.md 생성
- 웹이 존재한다면, playwirght를 이용한 e2e테스트를 진행하며, 웹이 존재하지 않을 시는 api테스트만 진행합니다.



### 테스트 자동화 이용 방법

~~~bash
1. claude code 실행
bash: claude

2. PR-리그레션-테스트-워크플로우.md에서, 테스트환경 수정

3. PR-리그레션-테스트-워크플로우.md 읽기 명령 후 테스트를 진행할 PR 링크 첨부
---------------------------------------------------------------
PR-리그레션-테스트-워크플로우.md 읽은 후, "http:PR Link"를 진행해줘
---------------------------------------------------------------

4. regression-test 폴더를 통해 test 결과 확인
~~~



### 테스트 결과 예시

1. 테스트 파일

![image-20260107193114807](/Users/xodn5235/Library/Application Support/typora-user-images/image-20260107193114807.png)

- 테스트를 진행하면, 위와 같이 테스트 결과에 대한 파일들이 정리 됩니다.



2. PR-분석

   ![image-20260107193211730](/Users/xodn5235/Library/Application Support/typora-user-images/image-20260107193211730.png)

   - 테스트를 진행할 PR에 대하여 정리한 파일입니다.

3. 테스트-결과

   ![image-20260107193249208](/Users/xodn5235/Library/Application Support/typora-user-images/image-20260107193249208.png)

   - 테스트 결과에 대하여 정리한 파일입니다.

   ![image-20260107193318079](/Users/xodn5235/Library/Application Support/typora-user-images/image-20260107193318079.png)

   - 실제 테스트가 제대로 진행됬는지에 대한 확인을 위한 스크린샷 결과 입니다.
