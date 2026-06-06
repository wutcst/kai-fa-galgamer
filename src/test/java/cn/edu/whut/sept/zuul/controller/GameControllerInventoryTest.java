package cn.edu.whut.sept.zuul.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GameControllerInventoryTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void inspectRewardAddsKeyItemOnce() throws Exception {
        mockMvc.perform(get("/api/game/init"))
                .andExpect(status().isOk());
        move("north");
        move("west");

        inspect().andExpect(jsonPath("$.inventoryItems", contains("blank_dice")));
        inspect().andExpect(jsonPath("$.inventoryItems", contains("blank_dice")));
    }

    @Test
    void craftSoulBellIsHandledByBackend() throws Exception {
        mockMvc.perform(get("/api/game/init"))
                .andExpect(status().isOk());
        move("west");
        move("south");
        move("north");
        move("east");
        move("south");
        move("south");
        move("west");
        inspect().andExpect(jsonPath("$.inventoryItems", hasItem("broken_bell")));
        move("east");
        move("east");
        inspect().andExpect(jsonPath("$.inventoryItems", hasItem("soul_flower")));
        move("west");
        move("north");
        move("north");
        move("west");

        mockMvc.perform(post("/api/game/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "CRAFT",
                                  "target": "soul_bell",
                                  "value": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryItems", hasItem("soul_bell")));
    }

    @Test
    void craftSoulBellReportsMissingMaterials() throws Exception {
        mockMvc.perform(get("/api/game/init"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/game/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "CRAFT",
                                  "target": "soul_bell",
                                  "value": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryItems", not(hasItem("soul_bell"))))
                .andExpect(jsonPath("$.errorMessage").exists());
    }

    private org.springframework.test.web.servlet.ResultActions inspect() throws Exception {
        return mockMvc.perform(post("/api/game/action")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "actionType": "INSPECT",
                          "target": null,
                          "value": null
                        }
                        """))
                .andExpect(status().isOk());
    }

    private void move(String direction) throws Exception {
        mockMvc.perform(post("/api/game/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "MOVE",
                                  "target": "%s",
                                  "value": null
                                }
                                """.formatted(direction)))
                .andExpect(status().isOk());
    }
}
