package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves legacy semantic IDs into IDs that actually exist in the active
 * compiled registry. Structured atlas hints retain their row/column meaning;
 * free-form hints use conservative semantic scoring and never cross the
 * caller's permitted asset types.
 */
final class SemanticAssetHintResolver {
    static final String VERSION = "semantic-asset-hint-resolver-0.1";

    private static final Pattern STRUCTURED_HINT = Pattern.compile("^([A-Z0-9]+)-([0-9]{2})([0-9]{2})$");
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "the", "of", "for", "to", "with", "or",
            "item", "object", "generic", "fixture", "asset", "icon",
            "small", "large", "light", "heavy", "standard", "basic"
    );

    private SemanticAssetHintResolver() {}

    static Optional<String> resolve(String hintId, String semanticText, Set<AssetType> permittedTypes) {
        Set<AssetType> types = permittedTypes == null || permittedTypes.isEmpty()
                ? Set.of(AssetType.OBJECT, AssetType.FIXTURE, AssetType.MACHINE,
                AssetType.ITEM_ICON, AssetType.WEAPON_ICON, AssetType.ARMOR_ICON)
                : Set.copyOf(permittedTypes);

        Optional<AssetMetadata> exact = AssetManager.metadata(hintId)
                .filter(asset -> types.contains(asset.type()));
        if (exact.isPresent()) return exact.map(AssetMetadata::id);

        Optional<String> coordinate = resolveStructuredHint(hintId, semanticText, types);
        if (coordinate.isPresent()) return coordinate;

        return resolveSemanticText(semanticText, types);
    }

    private static Optional<String> resolveStructuredHint(String hintId, String semanticText,
                                                          Set<AssetType> permittedTypes) {
        if (hintId == null || hintId.isBlank()) return Optional.empty();
        Matcher matcher = STRUCTURED_HINT.matcher(hintId.trim().toUpperCase(Locale.ROOT));
        if (!matcher.matches()) return Optional.empty();

        String prefix = matcher.group(1);
        int row = Integer.parseInt(matcher.group(2));
        int col = Integer.parseInt(matcher.group(3));
        if (row <= 0 || col <= 0) return Optional.empty();

        String coordinate = String.format(Locale.ROOT, "r%02dc%02d", row, col);
        List<String> atlasTerms = atlasTerms(prefix);
        if (atlasTerms.isEmpty()) return Optional.empty();

        ArrayList<Candidate> candidates = new ArrayList<>();
        for (AssetMetadata asset : AssetManager.registry().all()) {
            if (!permittedTypes.contains(asset.type())) continue;
            String haystack = haystack(asset);
            if (!haystack.contains(coordinate)) continue;
            int atlasScore = bestAtlasScore(haystack, atlasTerms);
            if (atlasScore <= 0) continue;
            int score = 100 + atlasScore + semanticScore(haystack, semanticText);
            candidates.add(new Candidate(asset, score));
        }
        return best(candidates);
    }

    private static Optional<String> resolveSemanticText(String semanticText, Set<AssetType> permittedTypes) {
        String normalized = normalize(semanticText);
        if (normalized.isBlank()) return Optional.empty();
        List<String> tokens = meaningfulTokens(normalized);
        if (tokens.isEmpty()) return Optional.empty();

        ArrayList<Candidate> candidates = new ArrayList<>();
        for (AssetMetadata asset : AssetManager.registry().all()) {
            if (!permittedTypes.contains(asset.type())) continue;
            String haystack = haystack(asset);
            int score = semanticScore(haystack, normalized);
            int directMatches = 0;
            for (String token : tokens) {
                if (haystack.contains(token)) directMatches++;
            }
            if (directMatches == 0 || score < 10) continue;
            candidates.add(new Candidate(asset, score));
        }
        return best(candidates);
    }

    private static Optional<String> best(List<Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) return Optional.empty();
        return candidates.stream()
                .sorted(Comparator.comparingInt(Candidate::score).reversed()
                        .thenComparing(candidate -> candidate.asset().name(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(candidate -> candidate.asset().id()))
                .map(candidate -> candidate.asset().id())
                .findFirst();
    }

    private static int semanticScore(String haystack, String semanticText) {
        String normalized = normalize(semanticText);
        if (normalized.isBlank()) return 0;
        int score = haystack.contains(normalized) ? 40 : 0;
        for (String token : meaningfulTokens(normalized)) {
            if (haystack.contains(token)) score += token.length() >= 6 ? 10 : 7;
            for (String synonym : synonyms(token)) {
                if (haystack.contains(synonym)) score += 3;
            }
        }
        return score;
    }

    private static int bestAtlasScore(String haystack, List<String> terms) {
        int score = 0;
        for (String term : terms) {
            String normalized = normalize(term);
            if (!normalized.isBlank() && haystack.contains(normalized)) {
                score = Math.max(score, normalized.length() >= 8 ? 35 : 25);
            }
        }
        return score;
    }

    private static List<String> atlasTerms(String prefix) {
        return switch (prefix == null ? "" : prefix.toUpperCase(Locale.ROOT)) {
            case "WP1" -> List.of("weapons1", "weapons 1", "weapon1", "weapon 1");
            case "WP2" -> List.of("weapons2", "weapons 2", "weapon2", "weapon 2");
            case "WP3" -> List.of("weapons3", "weapons 3", "weapon3", "weapon 3");
            case "DOM" -> List.of("domestic", "habitation", "furniture");
            case "MAC", "MACH" -> List.of("machines", "machine", "machinery");
            case "FTR" -> List.of("fixtures", "fixture");
            case "SHF" -> List.of("shelves", "shelf", "storage");
            case "WAL", "WALL" -> List.of("walls", "wall", "bulkhead");
            case "POS" -> List.of("posh", "noble", "luxury");
            case "DOR" -> List.of("doors", "door", "entry");
            case "PRK" -> List.of("parking", "road");
            case "ROD", "ROAD" -> List.of("roads", "road", "street");
            case "COR", "CORR", "CRA", "CRB" -> List.of("corridor", "corridors", "walkway");
            default -> List.of();
        };
    }

    private static List<String> meaningfulTokens(String value) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String token : normalize(value).split(" ")) {
            if (token.length() < 3 || STOPWORDS.contains(token)) continue;
            out.add(token);
        }
        return List.copyOf(out);
    }

    private static List<String> synonyms(String token) {
        return switch (token) {
            case "knife", "shiv", "dagger", "scalpel" -> List.of("blade", "melee");
            case "bolter", "pistol", "rifle", "carbine", "autogun", "shotgun" -> List.of("weapon", "gun", "firearm");
            case "armor", "armour", "helmet", "vest", "coat", "clothing" -> List.of("armor", "armour", "clothing", "wearable");
            case "crate", "box", "locker", "cabinet", "shelf" -> List.of("storage", "container", "cargo");
            case "cot", "bed", "bunk", "berth" -> List.of("bed", "sleeping", "domestic");
            case "water", "canteen", "bottle", "barrel" -> List.of("water", "container", "domestic");
            case "terminal", "cogitator", "data" -> List.of("terminal", "data", "device", "computer");
            case "forge", "smelter", "assembler", "boiler", "condenser" -> List.of("machine", "industrial", "machinery");
            default -> List.of();
        };
    }

    private static String haystack(AssetMetadata asset) {
        return normalize(asset.id() + " " + asset.name() + " " + asset.pathOrUri()
                + " " + asset.type().displayName() + " " + asset.semanticDescription());
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("[^a-z0-9+./ ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record Candidate(AssetMetadata asset, int score) {}
}
