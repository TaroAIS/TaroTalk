package com.tarotalk.feed.api;

import com.tarotalk.common.api.ApiResponse;
import com.tarotalk.feed.service.FeedService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/feeds")
@Validated
public class FeedV2Controller {
    private final FeedService feedService;

    public FeedV2Controller(FeedService feedService) {
        this.feedService = feedService;
    }

    @PostMapping("/{feedId}/like")
    public ApiResponse<LikeV2Response> like(@PathVariable UUID feedId,
                                            @Valid @RequestBody LikeActionRequest request) {
        FeedService.ToggleLikeResult result = feedService.toggleLike(feedId, request);
        LikeV2Response response = new LikeV2Response(
                feedId,
                request.getUserId(),
                request.getAction().name(),
                result.isLiked(),
                result.getEventId()
        );
        return ApiResponse.ok(response);
    }

    @PostMapping("/{feedId}/comments")
    public ApiResponse<CommentV2Response> comment(@PathVariable UUID feedId,
                                                  @Valid @RequestBody CommentRequest request) {
        FeedService.CommentResult result = feedService.commentV2(feedId, request);
        return ApiResponse.ok(new CommentV2Response(result.getInteraction(), result.getEventId()));
    }
}
