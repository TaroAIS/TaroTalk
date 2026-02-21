package com.tarotalk.feed;

import com.tarotalk.feed.api.CommentRequest;
import com.tarotalk.feed.api.FeedResponse;
import com.tarotalk.feed.api.LikeActionRequest;
import com.tarotalk.feed.domain.Feed;
import com.tarotalk.feed.domain.FeedInteraction;
import com.tarotalk.feed.domain.FeedRankingDecision;
import com.tarotalk.feed.repo.FeedInteractionRepository;
import com.tarotalk.feed.repo.FeedRankingDecisionRepository;
import com.tarotalk.feed.repo.FeedRepository;
import com.tarotalk.feed.service.BanditPolicyService;
import com.tarotalk.feed.service.FeedEventPublisher;
import com.tarotalk.feed.service.FeedService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FeedBanditShadowTest {
    @Test
    void shadowModePersistsDecisionsWithoutChangingOrder() {
        FeedRepository feedRepository = mock(FeedRepository.class);
        FeedInteractionRepository interactionRepository = mock(FeedInteractionRepository.class);
        FeedRankingDecisionRepository decisionRepository = mock(FeedRankingDecisionRepository.class);
        FeedEventPublisher eventPublisher = mock(FeedEventPublisher.class);
        BanditPolicyService banditPolicyService = mock(BanditPolicyService.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        FeedService feedService = new FeedService(
                feedRepository,
                interactionRepository,
                eventPublisher,
                restTemplate,
                "",
                "http://rel",
                "",
                "contact",
                72,
                200,
                0.35,
                0.25,
                0.20,
                0.20,
                decisionRepository,
                banditPolicyService,
                "shadow"
        );

        UUID viewerId = UUID.randomUUID();
        UUID authorA = UUID.randomUUID();
        UUID authorB = UUID.randomUUID();

        Feed feedA = new Feed(UUID.randomUUID(), authorA, "A");
        Feed feedB = new Feed(UUID.randomUUID(), authorB, "B");
        Instant same = Instant.now().minusSeconds(60);
        feedA.setCreatedAt(same);
        feedB.setCreatedAt(same);

        when(interactionRepository.findByFeedIdIn(any())).thenReturn(List.of());

        Map<String, Object> relation = new HashMap<>();
        relation.put("targetId", authorB.toString());
        relation.put("intimacyScore", 1.0);
        Map<String, Object> response = new HashMap<>();
        response.put("data", List.of(relation));
        when(restTemplate.getForObject(eq("http://rel/api/relationships/" + viewerId), eq(Map.class)))
                .thenReturn(response);

        when(banditPolicyService.scoreShadow(eq(viewerId), eq(feedA.getFeedId()), anyMap())).thenReturn(0.99);
        when(banditPolicyService.scoreShadow(eq(viewerId), eq(feedB.getFeedId()), anyMap())).thenReturn(0.05);
        when(decisionRepository.save(any(FeedRankingDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<FeedResponse> responses = feedService.buildResponses(List.of(feedA, feedB), viewerId);

        assertEquals(authorB, responses.get(0).getAuthorId());
        verify(decisionRepository, times(2)).save(any(FeedRankingDecision.class));

        ArgumentCaptor<FeedRankingDecision> captor = ArgumentCaptor.forClass(FeedRankingDecision.class);
        verify(decisionRepository, times(2)).save(captor.capture());
        long chosenCount = captor.getAllValues().stream().filter(FeedRankingDecision::getChosen).count();
        assertEquals(1L, chosenCount);
        assertTrue(captor.getAllValues().stream().allMatch(decision -> "linucb-epsilon-shadow".equals(decision.getPolicy())));
    }

    @Test
    void likeAndCommentBackfillBanditReward() {
        FeedRepository feedRepository = mock(FeedRepository.class);
        FeedInteractionRepository interactionRepository = mock(FeedInteractionRepository.class);
        FeedRankingDecisionRepository decisionRepository = mock(FeedRankingDecisionRepository.class);
        FeedEventPublisher eventPublisher = mock(FeedEventPublisher.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        FeedService feedService = new FeedService(
                feedRepository,
                interactionRepository,
                eventPublisher,
                restTemplate,
                "",
                "",
                "",
                "contact",
                72,
                200,
                0.35,
                0.25,
                0.20,
                0.20,
                decisionRepository,
                new BanditPolicyService(),
                "shadow"
        );

        UUID feedId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        Feed feed = new Feed(feedId, authorId, "hello");
        FeedRankingDecision decision = new FeedRankingDecision(UUID.randomUUID(), viewerId, feedId, "linucb-epsilon-shadow", 0.5);

        when(feedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        when(interactionRepository.findFirstByFeedIdAndUserIdAndTypeOrderByCreatedAtDesc(feedId, viewerId, FeedInteraction.Type.LIKE))
                .thenReturn(Optional.empty());
        when(interactionRepository.save(any(FeedInteraction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventPublisher.publishFeedLiked(feed, viewerId)).thenReturn("evt-like");
        when(eventPublisher.publishFeedCommented(eq(feed), eq(viewerId), eq("nice"))).thenReturn("evt-comment");
        when(decisionRepository.findFirstByViewerIdAndFeedIdOrderByCreatedAtDesc(viewerId, feedId))
                .thenReturn(Optional.of(decision));
        when(decisionRepository.save(any(FeedRankingDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LikeActionRequest likeRequest = new LikeActionRequest();
        likeRequest.setUserId(viewerId);
        likeRequest.setAction(LikeActionRequest.Action.LIKE);
        feedService.toggleLike(feedId, likeRequest);

        CommentRequest commentRequest = new CommentRequest();
        commentRequest.setUserId(viewerId);
        commentRequest.setContent("nice");
        feedService.commentV2(feedId, commentRequest);

        assertEquals(3.0, decision.getReward());
        verify(decisionRepository, atLeast(2)).save(any(FeedRankingDecision.class));
    }
}
