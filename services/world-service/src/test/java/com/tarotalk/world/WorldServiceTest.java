package com.tarotalk.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarotalk.world.api.WorldEventRequest;
import com.tarotalk.world.domain.StoryArc;
import com.tarotalk.world.domain.WorldEvent;
import com.tarotalk.world.domain.WorldState;
import com.tarotalk.world.repo.AgentGoalRepository;
import com.tarotalk.world.repo.StoryArcRepository;
import com.tarotalk.world.repo.WorldEventRepository;
import com.tarotalk.world.repo.WorldStateRepository;
import com.tarotalk.world.service.WorldService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class WorldServiceTest {
    @Test
    void appendEventUpdatesWorldStateVersion() {
        WorldStateRepository worldStateRepository = mock(WorldStateRepository.class);
        WorldEventRepository worldEventRepository = mock(WorldEventRepository.class);
        StoryArcRepository storyArcRepository = mock(StoryArcRepository.class);
        AgentGoalRepository agentGoalRepository = mock(AgentGoalRepository.class);

        WorldService service = new WorldService(
                worldStateRepository,
                worldEventRepository,
                storyArcRepository,
                agentGoalRepository,
                new ObjectMapper()
        );

        UUID worldId = UUID.randomUUID();
        WorldState existing = new WorldState(worldId);
        when(worldStateRepository.findById(worldId)).thenReturn(Optional.of(existing));
        when(worldEventRepository.save(any(WorldEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storyArcRepository.findByWorldIdOrderByUpdatedAtDesc(worldId)).thenReturn(Collections.singletonList(new StoryArc(UUID.randomUUID(), worldId, "arc")));
        when(storyArcRepository.save(any(StoryArc.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(worldStateRepository.save(any(WorldState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorldEventRequest request = new WorldEventRequest();
        request.setEventType("WORLD_EVOLUTION");
        request.setActorId("director");
        request.setTraceId("trace-1");

        service.appendEvent(worldId, request);

        verify(worldEventRepository, times(1)).save(any(WorldEvent.class));
        verify(worldStateRepository, atLeastOnce()).save(any(WorldState.class));
        assertEquals(1L, existing.getVersion());
        verify(storyArcRepository, times(1)).findByWorldIdOrderByUpdatedAtDesc(eq(worldId));
    }
}

