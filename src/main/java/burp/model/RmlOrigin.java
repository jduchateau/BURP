package burp.model;

public interface RmlOrigin {
    OriginInfo originInfo = null;

    record OriginInfo(String file, Point start, Point end) {
        public record Point(int line, int column) {
        }
    }
}



