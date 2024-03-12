package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MorphTester {
    public static void morphTester(String[] args) throws IOException {
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
        MorphTester morphTester = new MorphTester();
        String text = "м р ч ть Повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";
        HashMap<String, Integer> lemmaMap = morphTester.turnToLemmas(text);
        for (Map.Entry<String, Integer> entry : lemmaMap.entrySet()) {
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }

    private HashMap<String, Integer> turnToLemmas(String text) throws IOException {
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
        HashMap<String, Integer> lemmaMap = new HashMap<>();
        String regex = "[^а-яА-Я\s]";
        System.out.println(text);
        text = text.replaceAll(regex, "").replaceAll(" +", " ").toLowerCase().trim();
        System.out.println(text);

        if (text.equals("")) {
            return null;
        }

        List<String> wordList = new ArrayList<>(List.of(text.split("\s")));
        wordList.forEach(word -> {
            List<String> wordInfo = List.of(luceneMorphology.getMorphInfo(word).get(0).split("\s"));
            System.out.println(wordInfo);
            if (!(wordInfo.get(1).equals("МЕЖД") || wordInfo.get(1).equals("СОЮЗ") ||
                    wordInfo.get(1).equals("ПРЕДЛ") || wordInfo.get(1).equals("ЧАСТ") || wordInfo.get(1).contains("МС"))) {
                word = luceneMorphology.getNormalForms(word).get(0);
                if(lemmaMap.containsKey(word)) {
                    lemmaMap.put(word, lemmaMap.get(word) + 1);
                }else {
                    lemmaMap.put(word, 1);
                }
            }
        });
        return lemmaMap;
    }
}
