package cn.edu.whut.sept.zuul.puzzle;

import cn.edu.whut.sept.zuul.puzzle.builtin.DirectionSequencePuzzle;
import cn.edu.whut.sept.zuul.puzzle.builtin.ItemCombinationPuzzle;
import cn.edu.whut.sept.zuul.puzzle.builtin.PasswordPuzzle;
import cn.edu.whut.sept.zuul.puzzle.builtin.SealGatePuzzle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PuzzleConfig {

    @Bean
    public PuzzleRegistry puzzleRegistry() {
        PuzzleRegistry registry = new PuzzleRegistry();
        registry.register(new PasswordPuzzle());
        registry.register(new DirectionSequencePuzzle());
        registry.register(new ItemCombinationPuzzle());
        registry.register(new SealGatePuzzle());
        return registry;
    }
}
