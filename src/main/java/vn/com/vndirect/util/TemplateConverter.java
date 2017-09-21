package vn.com.vndirect.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by naruto on 6/14/17.
 */
public class TemplateConverter {
    private static final String FIELD_PREFIX = "$email.getItem(";
    private static final String FIELD_SUFIX = ")";

    public String convert(String oldTemp) {
        oldTemp = oldTemp.replace("@", "@@");
        List<String> occurFields = new ArrayList<>();
        List<Integer> idxFields = new ArrayList<>();
        findOccurFields(oldTemp, occurFields, idxFields);
        return buildNewTemp(oldTemp, occurFields, idxFields);
    }

    private String buildNewTemp(String oldTemp, List<String> occurFields, List<Integer> idxFields) {
        if (occurFields.isEmpty()) {
            return oldTemp;
        }
        Set<String> fields = new TreeSet<>(occurFields);
        StringBuilder temp = new StringBuilder("@args (");
        for (String field : fields) {
            temp.append("String ").append(field).append(',');
        }
        temp.setCharAt(temp.length() - 1, ')');
        temp.append('\n');
        String field;
        int idx;
        int start = 0;
        for (int i = 0; i < occurFields.size(); ++i) {
            field = occurFields.get(i);
            idx = idxFields.get(i);
            temp.append(oldTemp.substring(start, idx - FIELD_PREFIX.length() - 1)).append('@').append(field);
            start = idx + field.length() + FIELD_SUFIX.length() + 1;
        }
        temp.append(oldTemp.substring(start));
        return temp.toString();
    }

    private void findOccurFields(String oldTemp, List<String> occurFields, List<Integer> idxFields) {
        int idx = 0;
        while ((idx = oldTemp.indexOf(FIELD_PREFIX, idx)) >= 0) {
            idx += FIELD_PREFIX.length();
            char c = oldTemp.charAt(idx++);
            int end = oldTemp.indexOf(c + FIELD_SUFIX, idx);
            if (end < 0) {
                return;
            }
            String field = oldTemp.substring(idx, end);
            occurFields.add(field);
            idxFields.add(idx);
            idx = end + FIELD_SUFIX.length() + 1;
        }
    }

    private static String namingTemplate(Path path) {
        String newTempName = path.getFileName().toString().toLowerCase().replace('-', '_');
        return newTempName.substring("template_".length(), newTempName.length() - ".html".length());
    }

    public static void main(String[] args) throws IOException {
        TemplateConverter converter = new TemplateConverter();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("oldtemp"))) {
            stream.forEach(path -> {
                try {
                    String oldTemp = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                    String newTemp = converter.convert(oldTemp);
                    Path tempDir = Paths.get("templates", namingTemplate(path));
                    if (!Files.isDirectory(tempDir)) {
                        Files.createDirectory(tempDir);
                    }
                    Files.write(tempDir.resolve("temp.rocker.html"), newTemp.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

    }
}
