package com.codeagent.mcp.dependency;

import com.codeagent.mcp.dependency.DependencyScanner.ScanResult;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 저장소의 라이브러리를 빌드 파일에서 읽고, 알려진 취약점(OSV)을 붙인다. (OWASP A03 공급망)
 */
public class DependencyService {

    private static final List<String> SEVERITY_ORDER = List.of("critical", "high", "medium", "low");

    private static final int MAX_DEPENDENCIES = 500;

    private final OsvClient osvClient;
    private final DependencyHistory history;

    public DependencyService(OsvClient osvClient, DependencyHistory history) {
        this.osvClient = osvClient;
        this.history = history;
    }

    /** 검사 한 번과 지난 검사와의 비교 (첫 검사면 diff 는 null) */
    public record ScanView(long id, String commitSha, OffsetDateTime scannedAt, DependencyReport report, DependencyDiff diff) {
    }

    /** 지금 저장소를 검사하고 기록으로 남긴 뒤, 지난 검사와 비교한다 */
    public ScanView scan(String repositoryId, Path repoRoot, String commitSha) {
        DependencyReport report = check(repoRoot);
        List<DependencyHistory.StoredScan> before = history.list(repositoryId);
        DependencyHistory.StoredScan saved = history.add(repositoryId, commitSha, report);
        DependencyDiff diff = before.isEmpty() ? null : DependencyDiff.between(before.get(0).id(), before.get(0).commitSha(),
                before.get(0).scannedAt(), before.get(0).report(), report);
        return new ScanView(saved.id(), commitSha, saved.scannedAt(), report, diff);
    }

    /** 라이브러리 하나와 그 취약점 */
    public record DependencyView(String group, String artifact, String version, String file, int line, String scope,
                                 List<Vulnerability> vulnerabilities) {
    }

    /**
     * @param files       읽은 빌드 파일
     * @param unversioned 버전을 알 수 없어 검사하지 못한 라이브러리
     * @param checked     OSV 에 물어봤는지 (꺼져 있거나 실패하면 false)
     * @param error       OSV 에 묻지 못한 이유
     */
    public record DependencyReport(List<String> files, List<DependencyView> dependencies, List<String> unversioned,
                                   boolean checked, String error) {

        public List<DependencyView> vulnerable() {
            return dependencies.stream().filter(d -> !d.vulnerabilities().isEmpty()).toList();
        }
    }

    public DependencyReport check(Path repoRoot) {
        ScanResult scan = DependencyScanner.scan(repoRoot);
        List<Dependency> dependencies = scan.dependencies();
        if (!osvClient.enabled() || dependencies.isEmpty()) {
            return report(scan, Map.of(), false, osvClient.enabled() ? null : "OSV 검사가 꺼져 있음 (MCP_CODE_AGENT_OSV=off)");
        }
        List<Dependency> asked = dependencies.stream().limit(MAX_DEPENDENCIES).toList();
        try {
            String note = asked.size() < dependencies.size()
                    ? "라이브러리가 많아 앞의 " + asked.size() + "개만 검사함" : null;
            return report(scan, osvClient.query(asked), true, note);
        } catch (OsvClient.OsvException e) {
            return report(scan, Map.of(), false, e.getMessage());
        }
    }

    private static DependencyReport report(ScanResult scan, Map<Dependency, List<Vulnerability>> vulns, boolean checked, String error) {
        List<DependencyView> views = scan.dependencies().stream()
                .map(d -> new DependencyView(d.group(), d.artifact(), d.version(), d.file(), d.line(), d.scope(),
                        vulns.getOrDefault(d, List.of()).stream().sorted(Comparator.comparingInt(v -> rank(v.severity()))).toList()))
                // 취약점이 있는 것, 심각한 것부터
                .sorted(Comparator.comparingInt((DependencyView v) -> v.vulnerabilities().isEmpty() ? 1 : 0)
                        .thenComparingInt(v -> v.vulnerabilities().isEmpty() ? 99 : rank(v.vulnerabilities().get(0).severity())))
                .toList();
        return new DependencyReport(scan.files(), views, scan.unversioned(), checked, error);
    }

    private static int rank(String severity) {
        int i = SEVERITY_ORDER.indexOf(severity);
        return i < 0 ? SEVERITY_ORDER.size() : i;
    }

    /** Claude 에게 보여줄 요약. 지난 검사가 있으면 달라진 점도 붙인다 */
    public static String summarize(ScanView scan) {
        String summary = summarize(scan.report());
        DependencyDiff diff = scan.diff();
        if (diff == null) {
            return summary + "\n지난 검사 기록 없음 (이번이 첫 검사)";
        }
        String previousCommit = diff.previousCommitSha() == null ? ""
                : ", 커밋 " + diff.previousCommitSha().substring(0, Math.min(12, diff.previousCommitSha().length()));
        StringBuilder out = new StringBuilder(summary).append("\n\n지난 검사(").append(diff.previousScannedAt().toLocalDate())
                .append(previousCommit).append(")와 비교: ");
        if (!diff.hasChanges()) {
            return out.append("달라진 것 없음").toString();
        }
        out.append('\n');
        diff.changed().forEach(c -> out.append("- 버전 변경 ").append(c.group()).append(':').append(c.artifact())
                .append(' ').append(c.from()).append(" → ").append(c.to()).append('\n'));
        diff.added().forEach(a -> out.append("- 추가 ").append(a).append('\n'));
        diff.removed().forEach(r -> out.append("- 제거 ").append(r).append('\n'));
        diff.newVulnerabilities().forEach(v -> out.append("- 새 취약점 ").append(v.displayId()).append(" [")
                .append(v.severity() == null ? "심각도 모름" : v.severity()).append("] ").append(v.group()).append(':')
                .append(v.artifact()).append(':').append(v.version()).append('\n'));
        diff.resolvedVulnerabilities().forEach(v -> out.append("- 해결된 취약점 ").append(v.displayId()).append(' ')
                .append(v.group()).append(':').append(v.artifact()).append('\n'));
        return out.toString().stripTrailing();
    }

    /** 라이브러리 목록과 취약점 요약 */
    public static String summarize(DependencyReport report) {
        if (report.files().isEmpty()) {
            return "빌드 파일(pom.xml, build.gradle)을 찾지 못함";
        }
        StringBuilder out = new StringBuilder();
        out.append("빌드 파일: ").append(String.join(", ", report.files())).append('\n');
        out.append("라이브러리 ").append(report.dependencies().size()).append("개");
        if (!report.checked()) {
            out.append(" (알려진 취약점 검사 못 함: ").append(report.error()).append(")\n");
        } else {
            List<DependencyView> vulnerable = report.vulnerable();
            out.append(", 알려진 취약점이 있는 라이브러리 ").append(vulnerable.size()).append("개\n");
            for (DependencyView d : vulnerable) {
                out.append("- ").append(d.group()).append(':').append(d.artifact()).append(':').append(d.version())
                        .append(" (").append(d.file()).append(d.line() > 0 ? ":" + d.line() : "").append(", ").append(d.scope()).append(")\n");
                for (Vulnerability v : d.vulnerabilities().stream().limit(5).toList()) {
                    out.append("    · ").append(v.displayId());
                    if (!v.displayId().equals(v.id())) {
                        out.append(" / ").append(v.id());
                    }
                    out.append(" [").append(v.severity() == null ? "심각도 모름" : v.severity()).append("] ")
                            .append(v.summary() == null ? "" : v.summary());
                    if (!v.fixed().isEmpty()) {
                        out.append(" · 고친 버전 ").append(String.join(", ", v.fixed()));
                    }
                    out.append('\n');
                }
                if (d.vulnerabilities().size() > 5) {
                    out.append("    · 외 ").append(d.vulnerabilities().size() - 5).append("개\n");
                }
            }
            if (report.error() != null) {
                out.append("참고: ").append(report.error()).append('\n');
            }
        }
        if (!report.unversioned().isEmpty()) {
            out.append("버전을 알 수 없어 검사하지 못한 라이브러리 ").append(report.unversioned().size()).append("개: ")
                    .append(report.unversioned().stream().limit(15).collect(Collectors.joining(", ")))
                    .append(report.unversioned().size() > 15 ? " …" : "").append('\n');
        }
        out.append("전이 의존성은 gradle.lockfile 이 있을 때만 검사됨");
        return out.toString();
    }
}
