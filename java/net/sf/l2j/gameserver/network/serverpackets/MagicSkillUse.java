package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.L2Character;

public final class MagicSkillUse extends L2GameServerPacket
{
private static final String _S__5A_MAGICSKILLUSER = "[S] 48 MagicSkillUser";
private int _targetId, _tx, _ty, _tz;
private int _skillId;
private int _skillLevel;
private int _hitTime;
private int _reuseDelay;
private int _charObjId, _x, _y, _z;
/*	private int _flags;*/

public MagicSkillUse(L2Character cha, L2Character target, int skillId, int skillLevel, int hitTime, int reuseDelay)
{
	_charObjId = cha.getObjectId();
	
	if (target == null)
		_targetId = cha.getObjectId();
	else
		_targetId = target.getObjectId();
	
	_skillId = skillId;
	_skillLevel = skillLevel;
	_hitTime = hitTime;
	_reuseDelay = reuseDelay;
	_x = cha.getX();
	_y = cha.getY();
	_z = cha.getZ();
	_tx = target.getX();
	_ty = target.getY();
	_tz = target.getZ();
	/*		_flags |= 0x20;*/
}

public MagicSkillUse(L2Character cha, int skillId, int skillLevel, int hitTime, int reuseDelay)
{
	_charObjId = cha.getObjectId();
	_targetId = cha.getTargetId();
	_skillId = skillId;
	_skillLevel = skillLevel;
	_hitTime = hitTime;
	_reuseDelay = reuseDelay;
	_x = cha.getX();
	_y = cha.getY();
	_z = cha.getZ();
	_tx = cha.getX();
	_ty = cha.getY();
	_tz = cha.getZ();
	/*		_flags |= 0x20;*/
}

@Override
protected final void writeImpl()
{
	writeC(0x48);
	writeD(_charObjId);
	writeD(_targetId);
	writeD(_skillId);
	writeD(_skillLevel);
	writeD(_hitTime);
	writeD(_reuseDelay);
	writeD(_x);
	writeD(_y);
	writeD(_z);
	writeD(0); // unknown
	writeD(_tx);
	writeD(_ty);
	writeD(_tz);
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
 */
@Override
public String getType()
{
	return _S__5A_MAGICSKILLUSER;
}
}