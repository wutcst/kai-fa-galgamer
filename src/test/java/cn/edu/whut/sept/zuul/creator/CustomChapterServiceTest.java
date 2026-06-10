package cn.edu.whut.sept.zuul.creator;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.edu.whut.sept.zuul.save.SaveService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomChapterServiceTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validChapterShouldPassValidation() throws Exception {
        CustomChapterService service = new CustomChapterService(tempDir, new SaveService(tempDir.resolve("saves")));

        var errors = service.validate(objectMapper.readTree("""
                {
                  "chapterId": "sample",
                  "title": "Sample",
                  "startRoomId": "start",
                  "rooms": [
                    {
                      "roomId": "start",
                      "name": "Start",
                      "description": "Start room",
                      "exits": {},
                      "event": { "type": "story", "text": "hello" }
                    }
                  ]
                }
                """));

        assertTrue(errors.isEmpty());
    }

    @Test
    void invalidExitShouldFailValidation() throws Exception {
        CustomChapterService service = new CustomChapterService(tempDir, new SaveService(tempDir.resolve("saves")));

        var errors = service.validate(objectMapper.readTree("""
                {
                  "chapterId": "sample",
                  "title": "Sample",
                  "startRoomId": "start",
                  "rooms": [
                    { "roomId": "start", "exits": { "east": "missing" } }
                  ]
                }
                """));

        assertFalse(errors.isEmpty());
    }

    @Test
    void validDialogueGraphShouldPassValidation() throws Exception {
        CustomChapterService service = new CustomChapterService(tempDir, new SaveService(tempDir.resolve("saves")));

        var errors = service.validate(objectMapper.readTree("""
                {
                  "chapterId": "sample",
                  "title": "Sample",
                  "startRoomId": "start",
                  "rooms": [
                    {
                      "roomId": "start",
                      "exits": {},
                      "dialogue": {
                        "dialogueGroupId": "dial_sample",
                        "startNodeId": "node_01",
                        "nodes": {
                          "node_01": {
                            "type": "SPEECH",
                            "text": "hello",
                            "nextNodeId": "node_choice"
                          },
                          "node_choice": {
                            "type": "CHOICE",
                            "choices": [
                              {
                                "choiceId": "ok",
                                "text": "ok",
                                "conditions": [
                                  { "type": "HAS_ITEM", "itemKey": "nameless_badge" }
                                ],
                                "effects": [
                                  { "type": "GAIN_HP", "value": 5 }
                                ],
                                "nextNodeId": "EXIT"
                              }
                            ]
                          }
                        }
                      }
                    }
                  ]
                }
                """));

        assertTrue(errors.isEmpty());
    }

    @Test
    void dialogueGraphShouldRejectOrphanAndBrokenChoiceEdges() throws Exception {
        CustomChapterService service = new CustomChapterService(tempDir, new SaveService(tempDir.resolve("saves")));

        var errors = service.validate(objectMapper.readTree("""
                {
                  "chapterId": "sample",
                  "title": "Sample",
                  "startRoomId": "start",
                  "rooms": [
                    {
                      "roomId": "start",
                      "exits": {},
                      "dialogue": {
                        "dialogueGroupId": "dial_sample",
                        "startNodeId": "node_01",
                        "nodes": {
                          "node_01": {
                            "type": "CHOICE",
                            "choices": [
                              { "choiceId": "bad", "text": "bad", "nextNodeId": "missing" }
                            ]
                          },
                          "orphan": {
                            "type": "SPEECH",
                            "text": "lost",
                            "nextNodeId": "EXIT"
                          }
                        }
                      }
                    }
                  ]
                }
                """));

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(error -> error.contains("指向不存在节点")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("孤岛节点")));
    }
}
