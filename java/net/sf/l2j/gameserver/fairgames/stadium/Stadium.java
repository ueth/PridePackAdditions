package net.sf.l2j.gameserver.fairgames.stadium;

import cz.nxs.events.engine.base.Loc;

public class Stadium {
    private int _id;
    private Loc _locPlayer1;
    private Loc _locPlayer2;
    private Loc _locManager1;
    private Loc _locManager2;

    public Stadium(int _id, Loc _locPlayer1, Loc _locPlayer2, Loc _locManager1, Loc _locManager2) {
        this._id = _id;
        this._locPlayer1 = _locPlayer1;
        this._locPlayer2 = _locPlayer2;
        this._locManager1 = _locManager1;
        this._locManager2 = _locManager2;
    }

    public int getId() { return _id; }

    public void setId(int _id) { this._id = _id; }

    public Loc getLocPlayer1() { return _locPlayer1; }

    public void setLocPlayer1(Loc _locPlayer1) { this._locPlayer1 = _locPlayer1; }

    public Loc getLocPlayer2() { return _locPlayer2; }

    public void setLocPlayer2(Loc _locPlayer2) { this._locPlayer2 = _locPlayer2; }

    public Loc getLocManager1() { return _locManager1; }

    public void setLocManager1(Loc _locManager1) { this._locManager1 = _locManager1; }

    public Loc getLocManager2() { return _locManager2; }

    public void setLocManager2(Loc _locManager2) { this._locManager2 = _locManager2; }
}
