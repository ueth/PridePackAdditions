package net.sf.l2j.gameserver.fairgames.stadium;

import cz.nxs.events.engine.base.Loc;
import net.sf.l2j.gameserver.model.Location;

public class Stadium {
    private int _id;
    private Location _locPlayer1;
    private Location _locPlayer2;
    private Location _locManager1;
    private Location _locManager2;

    public Stadium(){}

    public Stadium(int _id, Location _locPlayer1, Location _locPlayer2, Location _locManager1, Location _locManager2) {
        this._id = _id;
        this._locPlayer1 = _locPlayer1;
        this._locPlayer2 = _locPlayer2;
        this._locManager1 = _locManager1;
        this._locManager2 = _locManager2;
    }

    public int getId() { return _id; }

    public void setId(int _id) { this._id = _id; }

    public Location getLocPlayer1() { return _locPlayer1; }

    public void setLocPlayer1(Location _locPlayer1) { this._locPlayer1 = _locPlayer1; }

    public Location getLocPlayer2() { return _locPlayer2; }

    public void setLocPlayer2(Location _locPlayer2) { this._locPlayer2 = _locPlayer2; }

    public Location getLocManager1() { return _locManager1; }

    public void setLocManager1(Location _locManager1) { this._locManager1 = _locManager1; }

    public Location getLocManager2() { return _locManager2; }

    public void setLocManager2(Location _locManager2) { this._locManager2 = _locManager2; }
}
