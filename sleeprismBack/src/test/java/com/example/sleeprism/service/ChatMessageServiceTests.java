package com.example.sleeprism.service;

import com.example.sleeprism.dto.ChatMessageResponseDTO;
import com.example.sleeprism.entity.ChatBlock;
import com.example.sleeprism.entity.ChatMessage;
import com.example.sleeprism.entity.ChatParticipant;
import com.example.sleeprism.entity.ChatRoom;
import com.example.sleeprism.entity.ChatRoomType; // ChatRoomType 임포트
import com.example.sleeprism.entity.NotificationType;
import com.example.sleeprism.entity.User;
import com.example.sleeprism.repository.ChatBlockRepository;
import com.example.sleeprism.repository.ChatMessageRepository;
import com.example.sleeprism.repository.ChatParticipantRepository;
import com.example.sleeprism.repository.ChatRoomRepository;
import com.example.sleeprism.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException; // AccessDeniedException 임포트 (만약 사용한다면)

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Mockito 사용을 위한 JUnit 확장
class ChatMessageServiceTests {

  @Mock // Mock 객체 생성
  private ChatMessageRepository chatMessageRepository;
  @Mock
  private ChatRoomRepository chatRoomRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private ChatParticipantRepository chatParticipantRepository;
  @Mock
  private ChatBlockRepository chatBlockRepository;
  @Mock
  private NotificationService notificationService;

  @InjectMocks // Mock 객체들을 주입받을 실제 서비스 객체
  private ChatMessageService chatMessageService;

  // 테스트에 사용할 공통 엔티티 및 데이터
  private User sender;
  private User recipient;
  private User nonParticipant;
  private ChatRoom singleChatRoom;
  private ChatRoom groupChatRoom;
  private ChatParticipant senderParticipant;
  private ChatParticipant recipientParticipant;
  private ChatMessage message1;
  private ChatMessage message2;

  @BeforeEach // 각 테스트 메서드 실행 전에 호출
  void setUp() {
    // 사용자 객체 초기화
    sender = User.builder().id(1L).email("sender@example.com").nickname("sender").build();
    recipient = User.builder().id(2L).email("recipient@example.com").nickname("recipient").build();
    nonParticipant = User.builder().id(3L).email("non@example.com").nickname("nonParticipant").build();

    // 채팅방 객체 초기화
    singleChatRoom = ChatRoom.builder().id(1L).type(ChatRoomType.SINGLE).isDeleted(false).build();
    groupChatRoom = ChatRoom.builder().id(2L).type(ChatRoomType.GROUP).isDeleted(false).build();

    // 채팅 참가자 객체 초기화
    senderParticipant = ChatParticipant.builder().id(10L).chatRoom(singleChatRoom).user(sender).isLeft(false).build();
    recipientParticipant = ChatParticipant.builder().id(11L).chatRoom(singleChatRoom).user(recipient).isLeft(false).build();

    // 메시지 객체 초기화
    message1 = ChatMessage.builder()
        .id(100L)
        .chatRoom(singleChatRoom)
        .sender(sender)
        .content("안녕하세요")
        .createdAt(LocalDateTime.now().minusMinutes(5))
        .isRead(false)
        .build();
    message2 = ChatMessage.builder()
        .id(101L)
        .chatRoom(singleChatRoom)
        .sender(recipient)
        .content("반갑습니다")
        .createdAt(LocalDateTime.now().minusMinutes(2))
        .isRead(false)
        .build();
  }

  // --- saveChatMessage 테스트 ---

  @Test
  @DisplayName("성공적으로 1대1 채팅 메시지를 저장하고 알림을 보낸다")
  void saveChatMessage_singleChat_success() {
    // Mocking 설정
    when(chatRoomRepository.findByIdAndIsDeletedFalse(singleChatRoom.getId())).thenReturn(Optional.of(singleChatRoom));
    when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
    when(chatParticipantRepository.findByChatRoomAndUserAndIsLeft(singleChatRoom, sender, false)).thenReturn(Optional.of(senderParticipant));
    when(chatParticipantRepository.findByChatRoomAndIsLeft(singleChatRoom, false)).thenReturn(Arrays.asList(senderParticipant, recipientParticipant));
    when(chatBlockRepository.findByBlockerAndBlocked(any(User.class), any(User.class))).thenReturn(Optional.empty()); // 차단되지 않음
    when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(message1); // 저장된 메시지 반환

    // 테스트 실행
    ChatMessageResponseDTO result = chatMessageService.saveChatMessage(singleChatRoom.getId(), sender.getId(), "테스트 메시지");

    // 검증
    assertNotNull(result);
    assertEquals("테스트 메시지", result.getContent());
    assertEquals(sender.getId(), result.getSenderId());
    assertEquals(sender.getNickname(), result.getSenderNickname());

    // 메서드 호출 검증
    verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    // 알림 서비스 호출 검증 (수신자에게만)
    verify(notificationService, times(1)).createNotification(
        eq(recipient),
        eq(NotificationType.CHAT_MESSAGE),
        anyString(), // 메시지 내용은 substring으로 인해 정확히 매칭하기 어려움
        eq("ChatRoom"),
        eq(singleChatRoom.getId()),
        anyString()
    );
  }

  @Test
  @DisplayName("사용자가 채팅방 참가자가 아니면 메시지 저장에 실패한다")
  void saveChatMessage_notParticipant_throwsException() {
    // Mocking 설정: 발신자가 채팅방에 없는 것으로 설정
    when(chatRoomRepository.findByIdAndIsDeletedFalse(singleChatRoom.getId())).thenReturn(Optional.of(singleChatRoom));
    when(userRepository.findById(nonParticipant.getId())).thenReturn(Optional.of(nonParticipant));
    when(chatParticipantRepository.findByChatRoomAndUserAndIsLeft(singleChatRoom, nonParticipant, false)).thenReturn(Optional.empty());

    // 테스트 실행 및 예외 검증
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        chatMessageService.saveChatMessage(singleChatRoom.getId(), nonParticipant.getId(), "테스트 메시지"));

    assertEquals("메시지를 보낼 수 있는 채팅방 참가자가 아닙니다.", exception.getMessage());
    // 메시지 저장이 호출되지 않음을 검증
    verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("발신자가 수신자를 차단했으면 메시지 저장에 실패한다")
  void saveChatMessage_senderBlockedRecipient_throwsException() {
    // Mocking 설정
    when(chatRoomRepository.findByIdAndIsDeletedFalse(singleChatRoom.getId())).thenReturn(Optional.of(singleChatRoom));
    when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
    when(chatParticipantRepository.findByChatRoomAndUserAndIsLeft(singleChatRoom, sender, false)).thenReturn(Optional.of(senderParticipant));
    when(chatParticipantRepository.findByChatRoomAndIsLeft(singleChatRoom, false)).thenReturn(Arrays.asList(senderParticipant, recipientParticipant));
    // 발신자가 수신자를 차단한 상황 설정
    when(chatBlockRepository.findByBlockerAndBlocked(sender, recipient)).thenReturn(Optional.of(new ChatBlock()));

    // 테스트 실행 및 예외 검증
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        chatMessageService.saveChatMessage(singleChatRoom.getId(), sender.getId(), "테스트 메시지"));

    assertEquals("채팅이 차단된 사용자에게 메시지를 보낼 수 없습니다.", exception.getMessage());
    verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("수신자가 발신자를 차단했으면 메시지 저장에 실패한다")
  void saveChatMessage_recipientBlockedSender_throwsException() {
    // Mocking 설정
    when(chatRoomRepository.findByIdAndIsDeletedFalse(singleChatRoom.getId())).thenReturn(Optional.of(singleChatRoom));
    when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
    when(chatParticipantRepository.findByChatRoomAndUserAndIsLeft(singleChatRoom, sender, false)).thenReturn(Optional.of(senderParticipant));
    when(chatParticipantRepository.findByChatRoomAndIsLeft(singleChatRoom, false)).thenReturn(Arrays.asList(senderParticipant, recipientParticipant));
    // 수신자가 발신자를 차단한 상황 설정
    when(chatBlockRepository.findByBlockerAndBlocked(recipient, sender)).thenReturn(Optional.of(new ChatBlock()));

    // 테스트 실행 및 예외 검증
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        chatMessageService.saveChatMessage(singleChatRoom.getId(), sender.getId(), "테스트 메시지"));

    assertEquals("채팅이 차단된 사용자에게 메시지를 보낼 수 없습니다.", exception.getMessage());
    verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("채팅방이 존재하지 않으면 메시지 저장에 실패한다")
  void saveChatMessage_chatRoomNotFound_throwsException() {
    // Mocking 설정: 채팅방이 존재하지 않음
    when(chatRoomRepository.findByIdAndIsDeletedFalse(anyLong())).thenReturn(Optional.empty());

    // 테스트 실행 및 예외 검증
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
        chatMessageService.saveChatMessage(999L, sender.getId(), "테스트 메시지"));

    assertEquals("Chat room not found or deleted with ID: 999", exception.getMessage());
    verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
  }

  // --- getChatHistory 테스트 ---

  @Test
  @DisplayName("채팅 내역을 성공적으로 조회한다")
  void getChatHistory_success() {
    Pageable pageable = PageRequest.of(0, 10);
    List<ChatMessage> messages = Arrays.asList(message2, message1); // 최신순 정렬
    PageImpl<ChatMessage> messagePage = new PageImpl<>(messages);

    when(chatRoomRepository.findByIdAndIsDeletedFalse(singleChatRoom.getId())).thenReturn(Optional.of(singleChatRoom));
    when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
    when(chatParticipantRepository.findByChatRoomAndUserAndIsLeft(singleChatRoom, sender, false)).thenReturn(Optional.of(senderParticipant));
    when(chatMessageRepository.findByChatRoomOrderByCreatedAtDesc(singleChatRoom, pageable)).thenReturn(messages);
    when(chatParticipantRepository.findByChatRoomAndUser(singleChatRoom, sender)).thenReturn(Optional.of(senderParticipant));

    List<ChatMessageResponseDTO> result = chatMessageService.getChatHistory(singleChatRoom.getId(), sender.getId(), 0, 10);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(2, result.size());
    assertEquals(message2.getId(), result.get(0).getId()); // 최신 메시지가 먼저 와야 함
    assertEquals(message1.getId(), result.get(1).getId());

    // lastReadMessageId 업데이트 호출 검증
    verify(senderParticipant, times(1)).updateLastReadMessageId(message2.getId());
    verify(chatParticipantRepository, times(1)).save(senderParticipant);
  }

  @Test
  @DisplayName("조회하려는 사용자가 채팅방 참가자가 아니면 채팅 내역 조회에 실패한다")
  void getChatHistory_notParticipant_throwsException() {
    when(chatRoomRepository.findByIdAndIsDeletedFalse(singleChatRoom.getId())).thenReturn(Optional.of(singleChatRoom));
    when(userRepository.findById(nonParticipant.getId())).thenReturn(Optional.of(nonParticipant));
    when(chatParticipantRepository.findByChatRoomAndUserAndIsLeft(singleChatRoom, nonParticipant, false)).thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        chatMessageService.getChatHistory(singleChatRoom.getId(), nonParticipant.getId(), 0, 10));

    assertEquals("회원님은 이 채팅방의 참가자가 아닙니다.", exception.getMessage());
    verify(chatMessageRepository, never()).findByChatRoomOrderByCreatedAtDesc(any(), any());
  }

  // --- markMessageAsRead 테스트 ---

  @Test
  @DisplayName("메시지를 성공적으로 읽음 처리한다")
  void markMessageAsRead_success() {
    when(chatMessageRepository.findById(message1.getId())).thenReturn(Optional.of(message1));
    when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
    when(chatParticipantRepository.findByChatRoomAndUserAndIsLeft(message1.getChatRoom(), sender, false)).thenReturn(Optional.of(senderParticipant));
    when(chatParticipantRepository.findByChatRoomAndUser(message1.getChatRoom(), sender)).thenReturn(Optional.of(senderParticipant));

    ChatMessageResponseDTO result = chatMessageService.markMessageAsRead(message1.getId(), sender.getId());

    assertTrue(result.isRead());
    verify(message1, times(1)).markAsRead(); // markAsRead 메서드 호출 검증 (spy가 더 적합할 수 있음)
    verify(senderParticipant, times(1)).updateLastReadMessageId(message1.getId());
    verify(chatParticipantRepository, times(1)).save(senderParticipant);
  }

  @Test
  @DisplayName("메시지를 읽을 권한이 없으면 읽음 처리 실패한다")
  void markMessageAsRead_noPermission_throwsException() {
    when(chatMessageRepository.findById(message1.getId())).thenReturn(Optional.of(message1));
    when(userRepository.findById(nonParticipant.getId())).thenReturn(Optional.of(nonParticipant));
    when(chatParticipantRepository.findByChatRoomAndUserAndIsLeft(message1.getChatRoom(), nonParticipant, false)).thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        chatMessageService.markMessageAsRead(message1.getId(), nonParticipant.getId()));

    assertEquals("메시지를 읽을 권한이 없습니다 (채팅방 참가자 아님).", exception.getMessage());
    verify(message1, never()).markAsRead();
    verify(chatParticipantRepository, never()).save(any(ChatParticipant.class));
  }
}
