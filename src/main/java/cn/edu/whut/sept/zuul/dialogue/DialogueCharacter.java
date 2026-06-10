package cn.edu.whut.sept.zuul.dialogue;

public record DialogueCharacter(
        String id,
        String expression
) {
    public String assetKey() {
        String normalizedId = id == null || id.isBlank() ? "silhouette_fallback" : id;
        String normalizedExpression = expression == null || expression.isBlank() ? "default" : expression;
        return "character." + normalizedId + "." + normalizedExpression;
    }
}
