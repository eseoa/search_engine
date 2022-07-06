package com.github.eseoa.searchEngine.lemmitization;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Collectors;

public class LemmasGenerator {

    private static final String NOT_RU_LETTERS = "[^А-Яа-яЙйЁё]";
    private static final String MULTIPLY_SPACE = "\\s{2,}";
    private static final String SPACE = " ";

    public static HashMap<String, Integer> getLemmaCountMap (String text) {
        HashMap<String, Integer> wordCount = new HashMap<>();
        try {
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            String[] fragments = getStringArray(text);
            wordCount.putAll(Arrays.stream(fragments)
                    .flatMap(s -> luceneMorph.getMorphInfo(s).stream())
                    .filter(LemmasGenerator::isNotOfficialPart)
                    .map(s -> s = s.substring(0, s.indexOf('|')))
                    .collect(Collectors.groupingBy(s -> s, Collectors.summingInt(value -> 1))));
        } catch (IOException e) {
            System.out.println("RussianLuceneMorphology() has an exception when creating a class object." +
                    "The map of lemmas will be empty");
            e.printStackTrace();
        }
        catch (ArrayIndexOutOfBoundsException | WrongCharaterException e) {
            System.out.println("LemmasGenerator has an exception when when tried to get the morphology of the word.\n" +
                    "Most likely the string was entered incorrectly. LemmasGenerator works only with Russian words.");
            System.out.println("String received by the method: |" + text + "|");
            System.out.println("Array of words of the given string " + Arrays.toString(getStringArray(text)));
            System.out.println("The map of lemmas will be empty");
            e.printStackTrace();
        }
        return wordCount;
    }

    private static boolean isNotOfficialPart (String s) {
        return !(s.contains("СОЮЗ") || s.contains("МЕЖД") || s.contains("ПРЕДЛ") || s.contains("ЧАСТ"));
    }

    public static String[] getStringArray (String text) {
        return text
                .replaceAll(NOT_RU_LETTERS, SPACE)
                .replaceAll(MULTIPLY_SPACE, SPACE)
                .toLowerCase(Locale.ROOT)
                .replaceAll("ё","е")
                .trim()
                .split(SPACE);
    }
}
