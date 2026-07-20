package mechanist;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Objects;

/** Reads one complete hosted-roster control-frame group through the canonical client authority. */
final class HostedRosterStreamReader {
    static HostedRosterClientAuthority.Snapshot read(
            BufferedReader reader,
            String beginLine,
            HostedRosterClientAuthority authority
    ) throws IOException {
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(authority, "authority");
        String[] begin = splitBegin(beginLine);
        int declaredEntries = parseDeclaredEntries(begin[4]);
        authority.accept(beginLine);
        for (int index = 0; index < declaredEntries; index++) {
            authority.accept(requireLine(
                    reader,
                    "HOSTED_ROSTER_ENTRY " + (index + 1)
                            + " of " + declaredEntries));
        }
        String end = requireLine(reader, "HOSTED_ROSTER_END");
        return authority.accept(end).orElseThrow(() ->
                new IOException(
                        "hosted roster end did not publish an immutable snapshot"));
    }

    private static String[] splitBegin(String line) throws IOException {
        if (line == null) {
            throw new IOException(
                    "server closed before HOSTED_ROSTER_BEGIN");
        }
        String[] fields = line.split("\\|", -1);
        if (fields.length != 7
                || !"MECH".equals(fields[0])
                || !"HOSTED_ROSTER_BEGIN".equals(fields[1])) {
            throw new IOException(
                    "expected MECH|HOSTED_ROSTER_BEGIN but received: " + line);
        }
        return fields;
    }

    private static int parseDeclaredEntries(String value) throws IOException {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0
                    || parsed > HostedRosterClientAuthority.MAX_VISIBLE_PLAYERS) {
                throw new NumberFormatException("outside visible-player bounds");
            }
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IOException(
                    "hosted roster declared an invalid visible-player count",
                    failure);
        }
    }

    private static String requireLine(
            BufferedReader reader,
            String expected
    ) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("server closed before " + expected);
        }
        return line;
    }

    private HostedRosterStreamReader() { }
}
