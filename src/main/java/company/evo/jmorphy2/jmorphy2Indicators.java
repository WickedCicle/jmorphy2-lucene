package company.evo.jmorphy2;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class jmorphy2Indicators {

    public static void main(String[] args) throws IOException {

        HashMap<String, String> tags = new HashMap<>();

        fillTags(tags);

        String dictResourcePath = String.format("/company/evo/jmorphy2/%s/pymorphy2_dicts", "ru");

        MorphAnalyzer morph = new MorphAnalyzer.Builder()
                .fileLoader(new ResourceFileLoader(dictResourcePath))
                .charSubstitutes(null)
                .cacheSize(0)
                .build();

        List<ParsedWord> parseds;

        ArrayList<String> texts = new ArrayList<>();

        Files.walk(Paths.get("D:\\Downloads\\RNC_million\\RNC_million\\sample_ar\\TEXTS"))
                .filter(Files::isRegularFile)
                .forEach((file -> {
                    texts.add(file.toString());
                }));

        AtomicInteger intUnfamilliar = new AtomicInteger(); // ненайденные в словаре
        AtomicInteger intKnown = new AtomicInteger(); // найденные в словаре
        AtomicInteger wordCount = new AtomicInteger(); // суммарное количесвто слов
        AtomicInteger accuracy = new AtomicInteger(); // точно определённые слова
        AtomicInteger morphAccuracy = new AtomicInteger(); // первая же форма с подходящими морфологическими хар-ками
        AtomicBoolean isAdded = new AtomicBoolean(false);

        Instant start;
        Instant finish;
        long elapsed = 0;

        try {
            for (String text : texts) {
                System.out.println("next file" + text);
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = documentBuilder.parse(text);

                Node html = document.getDocumentElement();

                NodeList htmlProps = html.getChildNodes();
                for (int i = 0; i < htmlProps.getLength(); i++) {
                    Node body = htmlProps.item(i);
                    if (body.getNodeType() != Node.TEXT_NODE && body.getNodeName().equals("body")) {
                        NodeList bodyProps = body.getChildNodes();
                        for (int j = 0; j < bodyProps.getLength(); j++) {
                            Node paragraph = bodyProps.item(j);
                            if (paragraph.getNodeType() != Node.TEXT_NODE && (paragraph.getNodeName().equals("p") || paragraph.getNodeName().equals("speach"))) {
                                NodeList paragraphProps = paragraph.getChildNodes();
                                for (int k = 0; k < paragraphProps.getLength(); k++) {
                                    Node sentence = paragraphProps.item(k);
                                    if (sentence.getNodeType() != Node.TEXT_NODE && sentence.getNodeName().equals("se")) {
                                        NodeList sentenceProps = sentence.getChildNodes();
                                        for (int m = 0; m < sentenceProps.getLength(); m++) {
                                            Node word = sentenceProps.item(m);
                                            if (word.getNodeType() != Node.TEXT_NODE && word.getNodeName().equals("w")) {
                                                wordCount.getAndIncrement();
                                                NodeList wordProps = word.getChildNodes();
                                                start = Instant.now();
                                                parseds = morph.parse(word.getTextContent().toLowerCase(Locale.ROOT).replaceAll("[` ]", ""));
                                                for (int n = 0; n < wordProps.getLength(); n++) {
                                                    Node characteristics = wordProps.item(n);
                                                    if (isAdded.get()) {
                                                        continue;
                                                    }
                                                    if (characteristics.getNodeType() != Node.TEXT_NODE && characteristics.getNodeName().equals("ana")) {
                                                        if (!Objects.equals(parseds.get(0).word, parseds.get(0).foundWord) || parseds.get(0).tag.contains("UNKN")){
                                                            intUnfamilliar.getAndIncrement();
                                                        } else {
                                                            intKnown.getAndIncrement();
                                                            if (Objects.equals(parseds.get(0).normalForm.toLowerCase(Locale.ROOT).replaceAll("ё", "е"), characteristics.getAttributes().getNamedItem("lex").getNodeValue().toLowerCase(Locale.ROOT).replaceAll("ё", "е"))) {
                                                                accuracy.getAndIncrement();
                                                            }

                                                            isAdded.set(true);

                                                            final String[] tag = {""};

                                                            String[] tagSplit = parseds.get(0).tag.getTagString().replaceAll(" ", ",").split("[,]");
                                                            StringBuilder temp = new StringBuilder();

                                                            for (String value : tagSplit) {
                                                                temp.append(tags.get(value));
                                                                temp.append(",");
                                                            }
                                                            temp = new StringBuilder(temp.substring(0, temp.length() - 1));

                                                            String[] transformedTag = temp.toString().split(",");

                                                            List<String> list = new ArrayList<>();
                                                            for (String s : transformedTag) {
                                                                if (s != null && !Objects.equals(s, "null") && !s.equals("0") && s.length() > 0) {
                                                                    list.add(s);
                                                                }
                                                            }
                                                            transformedTag = list.toArray(new String[0]);

                                                            String[] markTags = characteristics.getAttributes().getNamedItem("gr").getNodeValue()
                                                                    .replaceAll("-PRO", "").replaceAll("PRO", "")
                                                                    .replaceAll("distort", "").replaceAll("persn", "")
                                                                    .replaceAll("patrn", "").replaceAll("indic", "")
                                                                    .replaceAll("imper", "").replaceAll("abbr", "")
                                                                    .replaceAll("ciph", "").replaceAll("INIT", "")
                                                                    .replaceAll("anom", "").replaceAll("famn", "")
                                                                    .replaceAll("zoon", "").replaceAll("pass", "")
                                                                    .replaceAll("inan", "").replaceAll("anim", "")
                                                                    .replaceAll("intr", "").replaceAll("tran", "")
                                                                    .replaceAll("act", "").replaceAll("ipf", "")
                                                                    .replaceAll("med", "").replaceAll("pf", "")
                                                                    .split("[,=]");

                                                            list = new ArrayList<>();
                                                            for (String s : markTags) {
                                                                if (s != null && !Objects.equals(s, "null") && !s.equals("0") && s.length() > 0) {
                                                                    list.add(s);
                                                                }
                                                            }
                                                            markTags = list.toArray(new String[0]);

                                                            for (String markTag : markTags) {
                                                                if (!Arrays.asList(transformedTag).contains(markTag)) {
                                                                    isAdded.set(false);
                                                                }
                                                            }

                                                            if (isAdded.get()){
                                                                morphAccuracy.getAndIncrement();
                                                            }
                                                        }
                                                        isAdded.set(true);
                                                    }
                                                }
                                                finish = Instant.now();
                                                elapsed += Duration.between(start, finish).toMillis();
                                                isAdded.set(false);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            System.out.println("Количество ненайдённых: " + intUnfamilliar);
            System.out.println("Количество найдённых в словаре: " + intKnown);
            System.out.println("Общее количество слов: " + wordCount);
            System.out.println("Точно определенных начальных форм слов: " + accuracy);
            System.out.println("Точно определенных форм слов с полными характеристиками: " + morphAccuracy);
            System.out.println("Процент ненайдённых:" + intUnfamilliar.doubleValue()/wordCount.doubleValue());
            System.out.println("Точность начальных форм: " + accuracy.doubleValue()/intKnown.doubleValue());
            System.out.println("Точность определения характеристик первой формы: " + morphAccuracy.doubleValue()/intKnown.doubleValue());
            System.out.println("Затраченное время: " + (double)elapsed/1000 + " секунд");


        } catch (ParserConfigurationException | SAXException | IOException ex) {
            ex.printStackTrace(System.out);
        }
    }
    static void fillTags(HashMap<String, String> tags){
        tags.put("NOUN","S");
        tags.put("NPRO", "S");
        tags.put("ADJF","A,plen");
        tags.put("ADJS","A,brev");
        tags.put("COMP","A,comp,comp2");
        tags.put("VERB","V");
        tags.put("INFN","V,inf");
        tags.put("PRTF","A,partcp,plen");
        tags.put("PRTS","A,partcp,brev");
        tags.put("GRND","A,ger");
        tags.put("NUMR","NUM");
        tags.put("ADVB","ADV");
        tags.put("PRED","PRAEDIC");
        tags.put("PREP","PR");
        tags.put("CONJ","CONJ");
        tags.put("PRCL","PART");
        tags.put("INTJ","INTJ");
        tags.put("pres","praes");
        tags.put("past","praet");
        tags.put("futr","fut");
        tags.put("nomn","nom");
        tags.put("gent","gen");
        tags.put("datv","dat");
        tags.put("accs","acc");
        tags.put("ablt","ins");
        tags.put("loct","loc");
        tags.put("voct","voc");
        tags.put("gen2","gen2");
        tags.put("acc2","acc2");
        tags.put("loc2","loc2");
        tags.put("sing","sg");
        tags.put("plur","pl");
        tags.put("masc","m");
        tags.put("femn","f");
        tags.put("neut","n");
        tags.put("Ms-f","m-f");
        tags.put("1per", "1p");
        tags.put("2per", "2p");
        tags.put("3per", "3p");
    }
}