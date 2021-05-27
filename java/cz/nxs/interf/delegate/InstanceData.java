package cz.nxs.interf.delegate;

import net.sf.l2j.gameserver.model.entity.Instance;
import cz.nxs.l2j.delegate.IInstanceData;

/**
 * @author hNoke
 *
 */
public class InstanceData implements IInstanceData
{
	protected Instance _instance;
	
	public InstanceData(Instance i)
	{
		_instance = i;
	}
	
	public Instance getOwner()
	{
		return _instance;
	}
	
	@Override
	public int getId()
	{
		return _instance.getId();
	}
	
	@Override
	public String getName()
	{
		return _instance.getName();
	}
}
