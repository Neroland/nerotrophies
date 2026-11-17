package za.co.neroland.nerotrophies.compliance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Stage 6 log sweep over the source: no logger call at INFO or above, and no telemetry breadcrumb or handled-exception
 * report, takes a player, a UUID, a gamertag, a token, a position or free text. (Decision 007 "Logging rule".)
 */
class LogSweepTest {

    private static final Pattern CALL = Pattern.compile(
            "(LOGGER|LOG|log)\\s*\\.\\s*(info|warn|error|fatal)\\s*\\(|NeroTrophiesTelemetry\\.(breadcrumb|captureHandledException)\\s*\\(");
    private static final Pattern IDENTITY = Pattern.compile(
            "(?i)(getUUID|uuid|getGameProfile|getName\\(|getDisplayName|nameOf|\\bname\\(\\)|token|blockPosition|getX\\(|"
                    + "worldPosition|BlockPos|\\bpos\\b|player\\b|getScoreboardName|message|text)");

    @Test
    void noIdentityReachesALogLineAtInfoOrAbove() throws IOException {
        Path root = repoRoot();
        Assumptions.assumeTrue(root != null, "source tree not found from the test working directory");
        List<String> offenders = new ArrayList<>();
        int calls = 0;
        for (String loader : List.of("common", "fabric", "neoforge", "forge")) {
            Path src = root.resolve(loader).resolve("src").resolve("main").resolve("java");
            if (!Files.isDirectory(src)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(src)) {
                for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String text = Files.readString(f, StandardCharsets.UTF_8);
                    Matcher m = CALL.matcher(text);
                    while (m.find()) {
                        calls++;
                        String args = argsOf(text, m.end());
                        // The format string itself may say "player" in prose; only the arguments after it matter.
                        String afterFormat = args.replaceFirst("^\\s*\"(?:[^\"\\\\]|\\\\.)*\"(\\s*\\+\\s*\"(?:[^\"\\\\]|\\\\.)*\")*", "");
                        // An exception's class name is fine (and is what the family logs instead of its message).
                        afterFormat = afterFormat.replaceAll("\\w+\\.getClass\\(\\)\\.get(Simple)?Name\\(\\)", "");
                        if (IDENTITY.matcher(afterFormat).find()) {
                            offenders.add(root.relativize(f) + ": " + args.replaceAll("\\s+", " "));
                        }
                    }
                }
            }
        }
        assertTrue(calls > 0, "found the logger calls");
        assertTrue(offenders.isEmpty(), "log/telemetry calls that may carry identity: " + offenders);
    }

    private static String argsOf(String text, int from) {
        int depth = 1;
        int i = from;
        boolean inString = false;
        while (i < text.length() && depth > 0) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            i++;
        }
        return text.substring(from, Math.max(from, i - 1));
    }

    private static Path repoRoot() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null) {
            if (Files.isDirectory(p.resolve("common").resolve("src").resolve("main").resolve("java"))
                    && Files.exists(p.resolve("settings.gradle"))) {
                return p;
            }
            p = p.getParent();
        }
        return null;
    }
}
