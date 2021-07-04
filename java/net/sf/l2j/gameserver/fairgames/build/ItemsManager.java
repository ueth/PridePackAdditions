package net.sf.l2j.gameserver.fairgames.build;

import net.sf.l2j.gameserver.fairgames.entities.FGItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemsManager {

    private static final Map<Integer, FGItem> _weapons = new HashMap<>();
    private static final Map<Integer, FGItem> _jewels = new HashMap<>();
    private static final Map<Integer, FGItem> _tattoos = new HashMap<>();
    private static final Map<Integer, List<FGItem>> _armorSets= new HashMap<>();

    public static void loadFairGameWeapons() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(new File("data/xml/fairGames/items/weapons.xml"));
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("item");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    int id = Integer.valueOf(eElement.getAttribute("id"));
                    int itemId = Integer.valueOf(eElement.getElementsByTagName("itemId").item(0).getTextContent());
                    String description = eElement.getElementsByTagName("description").item(0).getTextContent();

                    _weapons.put(id, new FGItem(itemId, description));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("---------Loaded FG Weapons ----------");
        }
    }

    public static void loadFairGameJewels() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(new File("data/xml/fairGames/items/jewels.xml"));
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("item");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    int id = Integer.valueOf(eElement.getAttribute("id"));
                    int itemId = Integer.valueOf(eElement.getElementsByTagName("itemId").item(0).getTextContent());
                    String description = eElement.getElementsByTagName("description").item(0).getTextContent();

                    _jewels.put(id, new FGItem(itemId, description));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("---------Loaded FG Jewels ----------");
        }
    }

    public static void loadFairGameTattoos() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(new File("data/xml/fairGames/items/tattoos.xml"));
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("item");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    int id = Integer.valueOf(eElement.getAttribute("id"));
                    int itemId = Integer.valueOf(eElement.getElementsByTagName("itemId").item(0).getTextContent());
                    String description = eElement.getElementsByTagName("description").item(0).getTextContent();

                    _tattoos.put(id, new FGItem(itemId, description));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("---------Loaded FG Tattoos ----------");
        }
    }

    public static void loadFairGameArmorSets() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(new File("data/xml/fairGames/items/tattoos.xml"));
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("item");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    int id = Integer.valueOf(eElement.getAttribute("id"));
                    int itemId = Integer.valueOf(eElement.getElementsByTagName("itemId").item(0).getTextContent());
                    String description = eElement.getElementsByTagName("description").item(0).getTextContent();

                    _tattoos.put(id, new FGItem(itemId, description));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("---------Loaded FG Tattoos ----------");
        }
    }
}
