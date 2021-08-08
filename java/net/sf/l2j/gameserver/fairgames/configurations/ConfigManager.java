package net.sf.l2j.gameserver.fairgames.configurations;

import net.sf.l2j.gameserver.model.Location;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.StringTokenizer;

public class ConfigManager {
    private static ConfigManager _instance = null;

    private int _buildTime;
    private int _gameTime;
    private int _minPvp;
    private int _minLvl;
    private int _minPk;
    private int _clockPosition;
    private int _teleportTime;
    private Location _regNPCSpawnLocation = new Location();
    private int _minRegisteredPlayers;
    private int _registrationTime;

    public void loadConfigurations() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(new File("data/xml/fairGames/configs/configs.xml"));
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("configs");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    //int id = Integer.valueOf(eElement.getAttribute("id"));
                    _buildTime = Integer.valueOf(eElement.getElementsByTagName("buildTime").item(0).getTextContent());
                    _gameTime = Integer.valueOf(eElement.getElementsByTagName("gameTime").item(0).getTextContent());
                    _minPvp = Integer.valueOf(eElement.getElementsByTagName("minPvp").item(0).getTextContent());
                    _minLvl = Integer.valueOf(eElement.getElementsByTagName("minLvl").item(0).getTextContent());
                    _minPk = Integer.valueOf(eElement.getElementsByTagName("minPk").item(0).getTextContent());
                    _clockPosition = Integer.valueOf(eElement.getElementsByTagName("clockPosition").item(0).getTextContent());
                    _teleportTime = Integer.valueOf(eElement.getElementsByTagName("teleportTime").item(0).getTextContent());
                    _minRegisteredPlayers = Integer.valueOf(eElement.getElementsByTagName("minRegisteredPlayers").item(0).getTextContent());
                    _registrationTime = Integer.valueOf(eElement.getElementsByTagName("registrationTime").item(0).getTextContent());

                    StringTokenizer st = new StringTokenizer(eElement.getElementsByTagName("regNPCSpawnLocation").item(0).getTextContent(),";");

                    if(st.hasMoreTokens())
                        _regNPCSpawnLocation.setX(Integer.valueOf(st.nextToken()));
                    if(st.hasMoreTokens())
                        _regNPCSpawnLocation.setY(Integer.valueOf(st.nextToken()));
                    if(st.hasMoreTokens())
                        _regNPCSpawnLocation.setZ(Integer.valueOf(st.nextToken()));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("---------Loaded FG Configurations----------");
        }
    }

    public int getBuildTime() {return _buildTime;}

    public int getGameTime() {return _gameTime;}

    public int getMinPvp() {return _minPvp;}

    public int getMinLvl() {return _minLvl;}

    public int getMinPk() {return _minPk;}

    public int getClockPosition() {return _clockPosition;}

    public int getTeleportTime(){return _teleportTime;}

    public Location getRegNPCSpawnLocation(){return _regNPCSpawnLocation;}

    public int getMinRegisteredPlayers() {return _minRegisteredPlayers;}

    public int getRegistrationTime() {return _registrationTime;}

    public static ConfigManager getInstance() {
        if (_instance == null)
            _instance = new ConfigManager();
        return _instance;
    }
}
