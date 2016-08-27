package net.wheatlauncher.utils.nbt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author ci010
 */
public class NBTList extends NBT implements Iterable<NBT>
{
	private NBTType type;
	private List<NBT> lst = new ArrayList<>();

	NBTList(List<NBT> list) {this();}

	NBTList()
	{
		super(NBTType.LIST);
	}

	public int size()
	{
		return lst.size();
	}

	public boolean add(NBT base)
	{
		if (base.getType() == NBTType.NULL) return false;
		return validate(base) && lst.add(base);
	}

	public NBT get(int i)
	{
		return lst.get(i);
	}

	public NBT remove(int i)
	{
		return lst.remove(i);
	}

	public boolean isEmpty()
	{
		return lst.isEmpty();
	}

	public NBT set(int i, NBT base)
	{
		if (base.getType() == NBTType.NULL) return null;
		if (validate(base))
			return lst.set(i, base);
		return null;
	}

	private boolean validate(NBT base)
	{
		if (this.type == null) type = base.getType();
		return this.type == base.getType();
	}


	public NBTType getType()
	{
		return type;
	}

	@Override
	public Iterator<NBT> iterator()
	{
		return lst.iterator();
	}
}