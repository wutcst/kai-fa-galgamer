package cn.edu.whut.sept.zuul.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void initShouldReturnMockGameSnapshot() throws Exception {
        mockMvc.perform(get("/api/game/init"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRoomId").value("fate_hall"))
                .andExpect(jsonPath("$.roomTitle").value("命运大厅"))
                .andExpect(jsonPath("$.playerHp").value(100))
                .andExpect(jsonPath("$.gamePhase").value("EXPLORING"))
                .andExpect(jsonPath("$.roomAssetKey").value("scene.fate_hall"))
                .andExpect(jsonPath("$.availableActions[0].actionType").value("MOVE"))
                .andExpect(jsonPath("$.logs[0]").value("新游戏已初始化。你在命运大厅醒来。"));
    }

    @Test
    void actionShouldReturnUpdatedMockMessage() throws Exception {
        mockMvc.perform(get("/api/game/init"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/game/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "MOVE",
                                  "target": "north",
                                  "value": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRoomId").value("memory_library"))
                .andExpect(jsonPath("$.systemMessage").value("你向北移动，抵达：记忆图书馆"))
                .andExpect(jsonPath("$.errorMessage").value(nullValue()));
    }

    @Test
    void invalidDirectionShouldReturnErrorSnapshot() throws Exception {
        mockMvc.perform(get("/api/game/init"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/game/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "MOVE",
                                  "target": "up",
                                  "value": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRoomId").value("fate_hall"))
                .andExpect(jsonPath("$.errorMessage").value("当前房间不能向上移动。"));
    }

    @Test
    void saveAndLoadShouldReturnRealSnapshot() throws Exception {
        mockMvc.perform(post("/api/game/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "saveId": "slot_1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemMessage").value("存档已保存：slot_1"));

        mockMvc.perform(post("/api/game/load")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "saveId": "slot_1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemMessage").value("存档已读取：slot_1"));
    }
}
