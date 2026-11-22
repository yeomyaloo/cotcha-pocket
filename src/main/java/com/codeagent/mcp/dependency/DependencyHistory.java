package com.codeagent.mcp.dependency;

import com.codeagent.mcp.dependency.DependencyService.DependencyReport;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 저장소별 라이브러리 검사 기록. {home}/dependency-scans/{저장소}.json 에 최근 것부터 남긴다 (저장소마다 최대 20개).
 */
public class DependencyHistory {

    private static final int KEEP = 20;
    private static final TypeReference<List<StoredScan>> LIST = new TypeReference<>() {
    };

    /** 검사 한 번. id 는 저장소 안에서 1부터 늘어난다 */
    public record StoredScan(long id, String commitSha, OffsetDateTime scannedAt, DependencyReport report) {
    }

    private final Path dir;
    private final JsonMapper json = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    public DependencyHistory(Path dir) {
        this.dir = dir;
    }

    /** 최근 것부터 */
    public synchronized List<StoredScan> list(String repositoryId) {
        Path file = file(repositoryId);
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            return json.readValue(Files.readString(file), LIST);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (RuntimeException e) {
            // 형식이 바뀐 예전 파일은 기록이 없는 것으로 본다
            return List.of();
        }
    }

    /** 검사를 맨 앞에 저장하고, 저장한 검사를 돌려준다 */
    public synchronized StoredScan add(String repositoryId, String commitSha, DependencyReport report) {
        List<StoredScan> scans = new ArrayList<>(list(repositoryId));
        long id = scans.isEmpty() ? 1 : scans.get(0).id() + 1;
        StoredScan scan = new StoredScan(id, commitSha, OffsetDateTime.now(), report);
        scans.add(0, scan);
        try {
            Files.createDirectories(dir);
            Files.writeString(file(repositoryId), json.writeValueAsString(scans.subList(0, Math.min(KEEP, scans.size()))));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return scan;
    }

    private Path file(String repositoryId) {
        return dir.resolve(repositoryId.replaceAll("[^A-Za-z0-9._-]", "_") + ".json");
    }
}
