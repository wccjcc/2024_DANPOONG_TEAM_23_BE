package com.doctorgc.doctorgrandchild.service;



import com.doctorgc.doctorgrandchild.dto.KakaoTokenResponseDto;
import com.doctorgc.doctorgrandchild.dto.KakaoUserInfoResponseDto;
import com.doctorgc.doctorgrandchild.dto.LoginResponseDto;
import com.doctorgc.doctorgrandchild.entity.member.Member;
import com.doctorgc.doctorgrandchild.repository.MemberRepository;
import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {
    @Value("${kakao.client.rest-api-key}")
    private String clientId;
    private final String KAUTH_TOKEN_URL_HOST ="https://kauth.kakao.com";
    private final String KAUTH_USER_URL_HOST = "https://kapi.kakao.com";

    private final MemberRepository memberRepository;


    //프론트로 부터 받은 인가코드를 카카오 서버에 보내 accesstoken 받는 메서드
    public String getAccessTokenFromKakao(String code){
        KakaoTokenResponseDto kakaoTokenResponseDto = WebClient.create(KAUTH_TOKEN_URL_HOST).post()
                                                              .uri(uriBuilder -> uriBuilder
                                                                                         .scheme("https")
                                                                                         .path("/oauth/token")
                                                                                         .queryParam("grant_type", "authorization_code")
                                                                                         .queryParam("client_id", clientId)
                                                                                         .queryParam("code", code)
                                                                                         .build(true))
                                                              .header(HttpHeaders.CONTENT_TYPE,
                                                                      HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())
                                                              .retrieve()
                                                              .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.error(new RuntimeException("Invalid Parameter")))
                                                              .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new RuntimeException("Internal Server Error")))
                                                              .bodyToMono(KakaoTokenResponseDto.class)
                                                              .block();
        log.info(" [Kakao Service] Access Token ------> {}", kakaoTokenResponseDto.getAccessToken());
        log.info(" [Kakao Service] Refresh Token ------> {}", kakaoTokenResponseDto.getRefreshToken());
        //제공 조건: OpenID Connect가 활성화 된 앱의 토큰 발급 요청인 경우 또는 scope에 openid를 포함한 추가 항목 동의 받기 요청을 거친 토큰 발급 요청인 경우
        log.info(" [Kakao Service] Id Token ------> {}", kakaoTokenResponseDto.getIdToken());
        log.info(" [Kakao Service] Scope ------> {}", kakaoTokenResponseDto.getScope());

        return kakaoTokenResponseDto.getAccessToken();


    }
    //카카오로부터 받아온 accesstoken으로 유저 정보 받아오는 메서드
    public KakaoUserInfoResponseDto getUserInfo(String accessToken) {

        KakaoUserInfoResponseDto userInfo = WebClient.create(KAUTH_USER_URL_HOST)
                                                    .get()
                                                    .uri(uriBuilder -> uriBuilder
                                                                               .scheme("https")
                                                                               .path("/v2/user/me")
                                                                               .build(true))
                                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken) // access token 인가
                                                    .header(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())
                                                    .retrieve()
                                                    //TODO : Custom Exception
                                                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.error(new RuntimeException("Invalid Parameter")))
                                                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new RuntimeException("Internal Server Error")))
                                                    .bodyToMono(KakaoUserInfoResponseDto.class)
                                                    .block();

        log.info("[ Kakao Service ] Auth ID ---> {} ", userInfo.getId());
        log.info("[ Kakao Service ] NickName ---> {} ", userInfo.getKakaoAccount().getProfile().getNickName());
        log.info("[ Kakao Service ] ProfileImageUrl ---> {} ", userInfo.getKakaoAccount().getProfile().getProfileImageUrl());
        log.info("[ Kakao Service ] Email ---> {}", userInfo.getKakaoAccount().getEmail());
        return userInfo;
    }
    //받아온 유저정보로 회원가입&로그인하는 메서드(의사손주만의 로직)
    public LoginResponseDto kakaoUserLogin(KakaoUserInfoResponseDto userInfo){
        //isActive 도 확인해야
        Long id = userInfo.getId();
        String nickname = userInfo.getKakaoAccount().getProfile().getNickName();
        String profileImage = "true".equals(userInfo.getKakaoAccount().getProfile().getIsDefaultImage()) ? null : userInfo.getKakaoAccount().getProfile().getProfileImageUrl();
        String email = userInfo.getKakaoAccount().getEmail();
        //db에서 member가 존재하는지 확인
        Member finded_member = memberRepository.findByEmail(email).orElse(null);

        //등록된 멤버가 아니거나, 비활성화 상태라면 회원가입 처리
        if(finded_member == null || finded_member.isActive() == false){
            Member member = Member.builder()
                                    .email(email)
                                    .name(nickname)
                                    .profileImage(profileImage)
                                    .isActive(true)
                                    .build();
            memberRepository.save(member);
        }
        return new LoginResponseDto(id,nickname,profileImage,email);

    }



}

