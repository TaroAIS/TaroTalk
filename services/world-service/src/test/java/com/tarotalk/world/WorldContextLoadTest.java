package com.tarotalk.world;

import com.tarotalk.world.api.WorldController;
import com.tarotalk.world.service.WorldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = WorldServiceApplication.class)
public class WorldContextLoadTest {
    @Autowired
    private WorldController worldController;

    @Autowired
    private WorldService worldService;

    @Test
    void contextLoads() {
        assertNotNull(worldController);
        assertNotNull(worldService);
    }
}

