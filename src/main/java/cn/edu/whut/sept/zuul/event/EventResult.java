package cn.edu.whut.sept.zuul.event;

public record EventResult(EventResultType type, String message) {

    public static EventResult none() {
        return new EventResult(EventResultType.NONE, "");
    }

    public static EventResult message(String message) {
        return new EventResult(EventResultType.MESSAGE, message);
    }
}
