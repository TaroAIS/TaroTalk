package com.tarotalk.feed.api;

import com.tarotalk.common.api.ApiResponse;
import com.tarotalk.feed.domain.FeedInteraction;
import com.tarotalk.feed.service.FeedService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feeds")
@Validated
public class FeedController {
    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @PostMapping
    public ApiResponse<FeedResponse> create(@Valid @RequestBody CreateFeedRequest request) {
        return ApiResponse.ok(FeedResponse.from(feedService.create(request)));
    }

    @GetMapping
    public ApiResponse<List<FeedResponse>> list(@RequestParam(required = false) java.util.UUID viewerId,
                                                @RequestParam(required = false) String visibility) {
        List<com.tarotalk.feed.domain.Feed> feeds = viewerId == null
                ? feedService.list()
                : feedService.listVisible(viewerId, visibility);
        return ApiResponse.ok(feedService.buildResponses(feeds, viewerId));
    }

    @PostMapping("/{feedId}/comments")
    public ApiResponse<FeedInteraction> comment(@PathVariable java.util.UUID feedId,
                                                @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(feedService.comment(feedId, request));
    }

    @PostMapping("/{feedId}/like")
    public ApiResponse<FeedInteraction> like(@PathVariable java.util.UUID feedId,
                                             @Valid @RequestBody LikeRequest request) {
        return ApiResponse.ok(feedService.like(feedId, request));
    }
}
