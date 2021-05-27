package cz.nxs.interf.delegate;

import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;
import cz.nxs.l2j.delegate.IObjectData;

/**
 * @author hNoke
 *
 */
public class FenceData extends ObjectData implements IObjectData
{
	private L2FenceInstance _owner;
	
	public FenceData(L2FenceInstance cha)
	{
		super(cha);
		_owner = cha;
	}
	
	@Override
	public L2FenceInstance getOwner()
	{
		return _owner;
	}
	
	public void deleteMe()
	{
		L2WorldRegion region = _owner.getWorldRegion();
		_owner.decayMe();
		
		if (region != null)
			region.removeVisibleObject(_owner);
		
		_owner.getKnownList().removeAllKnownObjects();
		L2World.getInstance().removeObject(_owner);
	}
}
