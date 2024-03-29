package net.sf.l2j.gameserver.fairgames.build;

import net.sf.l2j.gameserver.fairgames.entities.FGSkill;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SkillsManager {
    private static final Map<String, Map<Integer, FGSkill>> _classSkills = new HashMap<>();
    private static final Map<Integer, FGSkill> _buffs = new HashMap<>();
    private static final String[] _xmlName = {"mage", "warrior", "assassin", "archer"};

    public static void loadFairGameSkillsAndBuffs(){
        loadFairGameBuffs();
        loadFairGameSkills();
    }

    public static void loadFairGameSkills(){
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder db = dbf.newDocumentBuilder();

            for(int i=0; i<_xmlName.length; i++){
                Map<Integer, FGSkill> list = new HashMap<>();
                Document doc = db.parse(new File("data/xml/fairGames/skills/"+_xmlName[i]+"Skills.xml"));
                doc.getDocumentElement().normalize();

                NodeList nList = doc.getElementsByTagName("skill");

                for (int temp = 0; temp < nList.getLength(); temp++) {
                    Node nNode = nList.item(temp);

                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;

                        int id = Integer.valueOf(eElement.getAttribute("id"));
                        int skillId = Integer.valueOf(eElement.getElementsByTagName("skillId").item(0).getTextContent());
                        String name = eElement.getElementsByTagName("name").item(0).getTextContent();
                        String icon = eElement.getElementsByTagName("icon").item(0).getTextContent();
                        String description = eElement.getElementsByTagName("description").item(0).getTextContent();

                        list.put(id, new FGSkill(skillId, name, icon, description));
                    }
                }
                _classSkills.put(_xmlName[i], list);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            System.out.println("---------Loaded Class skills ----------");
        }
    }

    public static void loadFairGameBuffs() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(new File("data/xml/fairGames/skills/buffs.xml"));
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("skill");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    int id = Integer.valueOf(eElement.getAttribute("id"));
                    int skillId = Integer.valueOf(eElement.getElementsByTagName("skillId").item(0).getTextContent());
                    String name = eElement.getElementsByTagName("name").item(0).getTextContent();
                    String icon = eElement.getElementsByTagName("icon").item(0).getTextContent();
                    String description = eElement.getElementsByTagName("description").item(0).getTextContent();

                    _buffs.put(id, new FGSkill(skillId, name, icon, description));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("---------Loaded FG Buffs----------");
        }
    }

    public static Map<Integer, FGSkill> getClassSkills(String name){
        return _classSkills.get(name);
    }

    public static Map<Integer, FGSkill> getBuffs(){
        return _buffs;
    }

    public static FGSkill getFGBuff(int id){
        return _buffs.get(id);
    }
}
