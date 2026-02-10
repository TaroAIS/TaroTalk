package com.tarotalk.relationship;

import com.tarotalk.common.exception.ApiException;
import com.tarotalk.relationship.api.RelationshipUpdateRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RelationshipUpdateRequestTest {
    @Test
    void allowKnownRelationshipType() {
        RelationshipUpdateRequest request = new RelationshipUpdateRequest();
        request.setType("FRIEND");
        assertEquals("friend", request.getType());
    }

    @Test
    void rejectUnknownRelationshipType() {
        RelationshipUpdateRequest request = new RelationshipUpdateRequest();
        assertThrows(ApiException.class, () -> request.setType("friend]; MATCH (n) DETACH DELETE n //"));
    }
}
