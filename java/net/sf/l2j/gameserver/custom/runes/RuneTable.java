package net.sf.l2j.gameserver.custom.runes;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuneTable {
    private static Map<Integer, Rune> _runes = new HashMap<>();

    public static void loadRunes(){
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(new File("data/xml/runes.xml"));
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("rune");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    int id = Integer.valueOf(eElement.getAttribute("id"));
                    int level = Integer.valueOf(eElement.getElementsByTagName("level").item(0).getTextContent());
                    int maxLevel = Integer.valueOf(eElement.getElementsByTagName("maxLevel").item(0).getTextContent());
                    String name = eElement.getElementsByTagName("name").item(0).getTextContent();
                    int skillId = Integer.valueOf(eElement.getElementsByTagName("skillId").item(0).getTextContent());

                    _runes.put(id, new Rune(level, maxLevel, id, name, 0.0, skillId));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            System.out.println("---------Loaded " + _runes.size() + " runes-----------");
            for(Rune rune : _runes.values()){
                System.out.println(rune.toString());
            }
        }
    }

    public static Rune getRune(int id){
        return _runes.get(id);
    }

    public static Map<Integer, Rune> getRunes(){
        return _runes;
    }
}
