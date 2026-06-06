package cn.edu.whut.sept.zuul.controller;

import com.fasterxml.jackson.databind.JsonNode;
import cn.edu.whut.sept.zuul.creator.CustomChapterService;
import cn.edu.whut.sept.zuul.model.GameSnapshot;
import cn.edu.whut.sept.zuul.service.RoomService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/creator")
public class CreatorController {

    private final CustomChapterService customChapterService;
    private final RoomService roomService;

    public CreatorController(CustomChapterService customChapterService, RoomService roomService) {
        this.customChapterService = customChapterService;
        this.roomService = roomService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("unlocked", customChapterService.unlocked());
    }

    @GetMapping("/chapters")
    public List<GameSnapshot.CreatorChapterSummary> chapters() {
        return customChapterService.listChapters();
    }

    @PostMapping("/validate")
    public Map<String, Object> validate(@RequestBody JsonNode chapter) {
        List<String> errors = customChapterService.validate(chapter);
        return Map.of("valid", errors.isEmpty(), "errors", errors);
    }

    @PostMapping("/chapters")
    public GameSnapshot.CreatorChapterSummary save(@RequestBody JsonNode chapter) {
        return customChapterService.save(chapter);
    }

    @DeleteMapping("/chapters/{chapterId}")
    public Map<String, Object> delete(@PathVariable String chapterId) {
        return Map.of("deleted", customChapterService.delete(chapterId));
    }

    @PostMapping("/chapters/{chapterId}/play")
    public GameSnapshot play(@PathVariable String chapterId) {
        return roomService.perform(new cn.edu.whut.sept.zuul.model.GameActionRequest("CREATOR_PLAY", chapterId, null));
    }
}
