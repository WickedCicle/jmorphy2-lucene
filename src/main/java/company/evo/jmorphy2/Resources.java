package company.evo.jmorphy2;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;


public class Resources {
    public static Map<Character,String> getCharSubstitutes(MorphAnalyzer.Lang lang) throws IOException {
        Map<Character,String> substitutes = new HashMap<>();
        for (String line : readLines(lang, "char_substitutes.txt")) {
            String[] parts = line.split("=>", 2);
            if (parts.length != 2) {
                continue;
            }
            String key = parts[0].trim();
            if (key.length() == 0) {
                continue;
            }
            String value = parts[1].trim();
            substitutes.put(key.charAt(0), value);
        }
        return substitutes;
    }

    public static Set<String> getKnownPrefixes(MorphAnalyzer.Lang lang) throws IOException {
        Set<String> prefixes = new HashSet<>();
        for (String line: readLines(lang, "known_prefixes.txt")) {
            prefixes.add(line.toLowerCase());
        }
        return prefixes;
    }

    private static List<String> readLines(MorphAnalyzer.Lang lang, String filename)
        throws IOException
    {
        String path = String.format("/lang/%s/%s", lang.name().toLowerCase(), filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            Resources.class.getResourceAsStream(path), StandardCharsets.UTF_8));
        List<String> lines = new ArrayList<>();
        String line = null;
        while ((line = reader.readLine()) != null) {
            String processedLine = processLine(parseString(line));
            if (!processedLine.equals("")) {
                lines.add(processedLine);
            }
        }
        return lines;
    }

    private static String processLine(String line) {
        String[] parts = line.split("#", 2);
        if (parts.length == 0) {
            return "";
        }
        String processedLine = parts[0].trim();
        return processedLine;
    }

    private static String parseString(String s) {
        int readPos = 0;
        int len = s.length();
        int writePos = 0;
        String out = "";
        while (readPos < len) {
            char c = s.charAt(readPos++);
            if (c == '\\') {
                if (readPos >= len)
                    throw new RuntimeException("Invalid escaped char in [" + s + "]");
                c = s.charAt(readPos++);
                switch (c) {
                    case '\\':
                        c = '\\';
                        break;
                    case 'n':
                        c = '\n';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case 'b':
                        c = '\b';
                        break;
                    case 'f':
                        c = '\f';
                        break;
                    case 'u':
                        if (readPos + 3 >= len)
                            throw new RuntimeException("Invalid escaped char in [" + s + "]");
                        c = (char) Integer.parseInt(s.substring(readPos, readPos + 4), 16);
                        readPos += 4;
                        break;
                }
            }
            out += c;
        }
        return out;
    }
}
