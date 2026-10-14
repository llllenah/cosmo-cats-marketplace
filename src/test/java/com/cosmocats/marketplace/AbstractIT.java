package com.cosmocats.marketplace;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for integration tests (*IT): starts the whole application context.
 * The context is recreated for every test class, so in-memory data changed by one class
 * does not leak into another.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
public abstract class AbstractIT {

    @Autowired
    protected MockMvc mockMvc;
}
