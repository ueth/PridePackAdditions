package net.sf.l2j.gameserver.fairgames.stadium;

import net.sf.l2j.gameserver.fairgames.entities.FGSkill;
import net.sf.l2j.gameserver.model.Location;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

public class StadiumManager {
    private Map<Integer,Stadium> _stadiums = new HashMap<>();
    private static StadiumManager _instance = null;

    public void loadStadiums(){
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(new File("data/xml/fairGames/stadiums/stadiums.xml"));
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("stadium");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    List<String> list = new ArrayList<>();
                    List<Location> locations = new ArrayList<>();

                    int id = Integer.valueOf(eElement.getAttribute("id"));
                    list.add(eElement.getElementsByTagName("playerOne").item(0).getTextContent());
                    list.add(eElement.getElementsByTagName("playerTwo").item(0).getTextContent());
                    list.add(eElement.getElementsByTagName("managerOne").item(0).getTextContent());
                    list.add(eElement.getElementsByTagName("managerTwo").item(0).getTextContent());

                    for(String string : list){
                        Location location = new Location();
                        StringTokenizer st = new StringTokenizer(string, ";");
                        if(st.hasMoreTokens())
                            location.setX(Integer.valueOf(st.nextToken()));
                        if(st.hasMoreTokens())
                            location.setY(Integer.valueOf(st.nextToken()));
                        if(st.hasMoreTokens())
                            location.setZ(Integer.valueOf(st.nextToken()));
                        if(st.hasMoreTokens())
                            location.setHeading(Integer.valueOf(st.nextToken()));

                        locations.add(location);
                    }

                    Stadium stadium = new Stadium(id, locations.get(0), locations.get(1), locations.get(2), locations.get(3));

                    _stadiums.put(id, stadium);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("---------Loaded FG Stadiums----------"+_stadiums.size());
        }
    }

    public Stadium getRandomStadium(){
        Random rand = new Random();
        return  _stadiums.get(rand.nextInt(_stadiums.size()+1));
    }

    public static StadiumManager getInstance(){
        if(_instance == null)
            _instance = new StadiumManager();
        return _instance;
    }
}
