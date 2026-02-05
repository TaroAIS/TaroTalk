package com.tarotalk.relationship.api;

import com.tarotalk.common.api.ApiResponse;
import com.tarotalk.relationship.service.RelationshipService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/relationships")
@Validated
public class RelationshipController {
    private final RelationshipService relationshipService;

    public RelationshipController(RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    @GetMapping("/{userId}")
    public ApiResponse<List<RelationshipResponse>> list(@PathVariable String userId) {
        return ApiResponse.ok(relationshipService.getRelationships(userId));
    }

    @PostMapping("/{userId}")
    public ApiResponse<Void> update(@PathVariable String userId,
                                    @Valid @RequestBody RelationshipUpdateRequest request) {
        relationshipService.upsertRelationship(userId, request);
        return ApiResponse.ok(null);
    }
}
