package net.launcher.game.nbt;

import java.io.*;

/**
 * @author ci010
 */
public abstract class NBT implements Cloneable
{
	NBT(NBTType type)
	{
		this.type = type;
	}

	private NBTType type;

	public NBTType getType() {return type;}

	public boolean isNull() {return this == NBT.empty();}

	public boolean isType(NBTType type) { return this.type == type;}

	public boolean isPrimitive() {return false;}

	public boolean isCompound() {return false;}

	public boolean isList() {return false;}

	public NBTPrimitive asPrimitive() {throw new UnsupportedOperationException();}

	public NBTCompound asCompound() {throw new UnsupportedOperationException();}

	public NBTList asList() {throw new UnsupportedOperationException();}

	public String asString() {throw new UnsupportedOperationException();}

	public String asString(String option) { try {return asString();}catch (Exception e) {return option;}}

	public int asInt() {throw new UnsupportedOperationException();}

	public int asInt(int option) { try {return asInt();}catch (Exception e) {return option;}}

	public float asFloat() {throw new UnsupportedOperationException();}

	public float asFloat(float option) { try {return asFloat();}catch (Exception e) {return option;}}

	public byte asByte() {throw new UnsupportedOperationException();}

	public byte asByte(byte option) { try {return asByte();}catch (Exception e) {return option;}}

	public long asLong() {throw new UnsupportedOperationException();}

	public long asLong(long option) { try {return asLong();}catch (Exception e) {return option;}}

	public short asShort() {throw new UnsupportedOperationException();}

	public short asShort(short option) { try {return asShort();}catch (Exception e) {return option;}}

	public double asDouble() {throw new UnsupportedOperationException();}

	public double asDouble(double option) { try {return asDouble();}catch (Exception e) {return option;}}

	public boolean asBool() {throw new UnsupportedOperationException();}

	public boolean asBool(boolean option) { try {return asBool();}catch (Exception e) {return option;}}

	public byte[] asByteArray() {throw new UnsupportedOperationException();}

	public byte[] asByteArray(byte[] option) { try {return asByteArray();}catch (Exception e) {return option;}}

	public int[] asIntArray() {throw new UnsupportedOperationException();}

	public int[] asIntArray(int[] option) { try {return asIntArray();}catch (Exception e) {return option;}}

	public boolean asBoolean() {throw new UnsupportedOperationException();}

	public boolean asIntArray(boolean option) { try {return asBool();}catch (Exception e) {return option;}}

	public abstract NBT clone();

	public static NBTPrimitive number(Number number)
	{
		try
		{
			NBTType nbtType = NBTType.valueOf(number.getClass().getSimpleName().toUpperCase());
			return new NBTPrimitive(number, nbtType);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException(String.format("Unknown type %s of number for NBT!", number.getClass()), e);
		}
	}

	public static NBTPrimitive bool(boolean bool)
	{
		return number((byte) (bool ? 1 : 0));
	}

	public static NBTPrimitive string(String s)
	{
		return new NBTPrimitive(s, NBTType.STRING);
	}

	public static NBTPrimitive intArr(int[] arr)
	{
		return new NBTPrimitive(arr, NBTType.INT_ARR);
	}

	public static NBTPrimitive byteArr(byte[] arr)
	{
		return new NBTPrimitive(arr, NBTType.BYTE_ARR);
	}

	public static NBTCompound compound()
	{
		return new NBTCompound();
	}

	public static NBTList list() {return new NBTList();}

	public static NBT read(InputStream stream, boolean isCompressed) throws IOException
	{
		return NBTType.readTag(stream, isCompressed);
	}

	public static NBT read(File file, boolean isCompressed) throws IOException
	{
		if (!file.exists()) return null;
		return read(new FileInputStream(file), isCompressed);
	}

	public static void write(File file, NBTCompound compound, boolean isCompressed) throws IOException
	{
		write(new FileOutputStream(file), compound, isCompressed);
	}

	public static void write(OutputStream stream, NBTCompound compound, boolean isCompressed) throws IOException
	{
		NBTType.writeTag(stream, compound, isCompressed);
	}

	public static void overwrite(File file, NBTCompound compound, boolean isCompressed) throws IOException
	{
		File parentFile = file.getParentFile();
		File cache = new File(parentFile, file.getName() + ".cache");
		write(new FileOutputStream(cache), compound, isCompressed);
		if (file.exists()) file.delete();
		cache.renameTo(file);
	}

	public static void overwriteAndBackup(File file, File backup, NBTCompound compound, boolean isCompressed) throws
			IOException
	{
		File parentFile = file.getParentFile();
		File cache = new File(parentFile, file.getName() + ".cache");
		write(new FileOutputStream(cache), compound, isCompressed);
		if (file.exists())
		{
			if (backup.exists())
				backup.delete();
			file.renameTo(backup);
		}
		cache.renameTo(file);
	}

	public static NBT empty()
	{
		if (NULL == null)
			NULL = new NBTEnd();
		return NULL;
	}

	private static NBT NULL;

}