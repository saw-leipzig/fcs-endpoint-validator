package eu.clarin.sru.fcs.validator.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to convert between various language codes.
 *
 * @author Yana Panchenko
 * @author Erik Körner
 * @see <a href="https://iso639-3.sil.org/code_tables/download_tables">SIL
 *      Language Code Table Download Page</a>
 * @see <a href=
 *      "https://github.com/clarin-eric/fcs-sru-aggregator/blob/master/src/main/java/eu/clarin/sru/fcs/aggregator/util/LanguagesISO693.java">Original
 *      Source from CLARIN SRU/FCS Aggregator</a>
 */
public class LanguagesISO693 {
    private static final Logger logger = LoggerFactory.getLogger(LanguagesISO693.class);

    public static final String LANGUAGES_FILE_PATH = "/lang/iso-639-3.tab";
    public static final String LANGUAGES_FILE_ENCODING = "UTF-8";

    private static LanguagesISO693 instance = null;

    public static class Language {
        // code is ISO-639-3 (3 letters) while code_1 is ISO-639-1 (2 letters)
        private final String code_3, code_1, name;

        public Language(String code_3, String code_1, String name) {
            this.code_3 = code_3;
            this.code_1 = code_1;
            this.name = name;
        }
    }

    private final Map<String, Language> codeToLang;
    private final Map<String, Language> nameToLang;

    private LanguagesISO693() {
        Map<String, Language> codeToLang = new HashMap<String, Language>();
        Map<String, Language> nameToLang = new HashMap<String, Language>();

        InputStream is = LanguagesISO693.class.getResourceAsStream(LANGUAGES_FILE_PATH);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, LANGUAGES_FILE_ENCODING))) {
            br.readLine(); // ignore first line (header)
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() > 0) {
                    String[] toks = line.split("\\t");
                    if (toks.length != 7 && toks.length != 8) {
                        logger.error("Line error in language codes file: '{}'", line);
                        continue;
                    }
                    String code_3 = toks[0].trim();
                    String code_1 = toks[3].trim().isEmpty() ? null : toks[3].trim();
                    if (code_1 != null && code_1.length() != 2) {
                        throw new RuntimeException("bad ISO-639-1 code: " + code_1);
                    }
                    String name = toks[6].trim();
                    Language l = new Language(code_3, code_1, name);
                    codeToLang.put(code_3, l);
                    if (code_1 != null) {
                        codeToLang.put(code_1, l);
                    }
                    nameToLang.put(name, l);
                }
            }
        } catch (IOException ex) {
            logger.error("Initialization of languages code to name mapping failed.", ex);
        }

        this.codeToLang = Collections.unmodifiableMap(codeToLang);
        this.nameToLang = Collections.unmodifiableMap(nameToLang);
    }

    public static synchronized LanguagesISO693 getInstance() {
        if (instance == null) {
            instance = new LanguagesISO693();
        }
        return instance;
    }

    public boolean isCode(String code) {
        return codeToLang.containsKey(code);
    }

    public boolean isCode_3(String code639_3) {
        if (code639_3 == null) {
            return false;
        }
        Language lang = codeToLang.get(code639_3);
        if (lang == null) {
            return false;
        }
        return lang.code_3.equals(code639_3);
    }

    public String code_3ForCode(String code639_1) {
        if (code639_1 == null) {
            return null;
        }
        Language lang = codeToLang.get(code639_1);
        if (lang == null) {
            logger.error("Unknown ISO-639-1 code: {}", code639_1);
            return null;
        }
        return lang.code_3;
    }

    public String code_1ForCode_3(String code639_3) {
        if (code639_3 == null) {
            return null;
        }
        Language lang = codeToLang.get(code639_3);
        if (lang == null) {
            logger.error("Unknown ISO-639-3 code: {}", code639_3);
            return null;
        }
        return lang.code_1;
    }

    public String code_3ForName(String name) {
        Language lang = nameToLang.get(name);
        if (lang == null) {
            logger.error("Unknown language name: {}", name);
            return null;
        }
        return lang.code_3;
    }

    public String nameForCode(String code) {
        Language lang = codeToLang.get(code);
        if (lang == null) {
            logger.error("Unknown language code: {}", code);
            return null;
        }
        return lang.name;
    }

    public Map<String, String> getLanguageMap(Set<String> codes) {
        Map<String, String> languages = new HashMap<String, String>();
        for (String code : codes) {
            String name = nameForCode(code);
            languages.put(code, name != null ? name : code);
        }
        return languages;
    }

    public Map<String, String> getLanguageMap(String... codes) {
        Map<String, String> languages = new HashMap<String, String>();
        for (String code : codes) {
            String name = nameForCode(code);
            languages.put(code, name != null ? name : code);
        }
        return languages;
    }

}
