package net.wheatlauncher.utils.nbt;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author ci010
 */
public enum NBTType
{
	NULL(null),
	BYTE(DataInput::readByte), SHORT(DataInput::readShort), INTEGER(DataInput::readInt),
	LONG(DataInput::readLong), FLOAT(DataInput::readFloat), DOUBLE(DataInput::readDouble),
	BYTE_ARR(input -> {
		byte[] data = new byte[input.readInt()];
		input.readFully(data);
		return data;
	}),
	STRING(DataInput::readUTF),
	LIST(input -> {
		byte type = input.readByte();
		int length = input.readInt();
		List<NBT> list = new ArrayList<>(length);
		for (int i = 0; i < length; ++i)
			list.add(readTag(input, type));
		return list;
	}),
	COMPOUND(input -> {
		Map<String, NBT> map = new HashMap<>();
		for (byte type; (type = input.readByte()) != 0; )
			map.put((String) STRING.readTagToRaw(input), readTag(input, type));
		return map;
	}),
	INT_ARR(input -> {
		int[] data = new int[input.readInt()];
		for (int i = 0; i < data.length; i++)
			data[i] = input.readInt();
		return data;
	});

	public boolean isNull() {return this == NULL;}

	public boolean isPrimitive() {return !isCompound() && !isList();}

	public boolean isList() {return this == LIST;}

	public boolean isCompound() {return this == COMPOUND;}

	public byte getId() {return (byte) this.ordinal();}

	private Object readTagToRaw(DataInput input) throws IOException {return readFunction.apply(input);}

	@SuppressWarnings("unchecked")
	private static NBT readTag(DataInput in, byte type) throws IOException
	{
		if (type < 1 || type > 10)
			throw new IOException("Invalid NBT tag type (1-10): " + type);
		NBTType nbtType = NBTType.values()[type];
		Object v = nbtType.readTagToRaw(in);
		if (nbtType.isCompound()) return new NBTCompound((Map<String, NBT>) v);
		if (nbtType.isList()) return new NBTList((List<NBT>) v);
		return new NBTPrimitive(v, nbtType);
	}

	@SuppressWarnings("unchecked")
	static NBT readTag(InputStream inputStream, boolean isCompressed) throws IOException
	{
		try (DataInputStream data = isCompressed ?
				new DataInputStream(new GZIPInputStream(inputStream)) : new DataInputStream(new BufferedInputStream(inputStream)))
		{
			int rootType = data.read();
			if (rootType == 0) return new NBTEnd();
			if (rootType != 10) throw new IOException("Root tag must be a named compound tag.");
			STRING.readTagToRaw(data); //I think this is the name of the file...
			Map<String, NBT> o = (Map<String, NBT>) COMPOUND.readTagToRaw(data);
			return new NBTCompound(o);
		}
	}

	static void writeTag(OutputStream outputStream, NBTCompound nbt, boolean isCompressed) throws IOException
	{
		try (DataOutputStream out = isCompressed ?
				new DataOutputStream(new GZIPOutputStream(outputStream)) : new DataOutputStream(new
				BufferedOutputStream(outputStream)))
		{
			out.writeByte(10);
			out.writeUTF("");
			writeTag(nbt, out);
		}
	}

	private static void writeTag(NBT tag, DataOutput out) throws IOException
	{
		NBTType tp = tag.getType();
		switch (tp)
		{
			case BYTE: out.writeByte(tag.getAsPrimitive().getAsByte()); break;
			case SHORT: out.writeShort(tag.getAsPrimitive().getAsShort()); break;
			case INTEGER: out.writeInt(tag.getAsPrimitive().getAsInt()); break;
			case LONG: out.writeLong(tag.getAsPrimitive().getAsLong()); break;
			case FLOAT: out.writeFloat(tag.getAsPrimitive().getAsFloat()); break;
			case DOUBLE: out.writeDouble(tag.getAsPrimitive().getAsDouble()); break;

			case BYTE_ARR:
				byte[] bytes = tag.getAsPrimitive().getAsByteArray();
				out.writeInt(bytes.length);
				out.write(bytes);
				break;
			case STRING: out.writeUTF(tag.getAsPrimitive().getAsString()); break;
			case LIST:
				NBTList list = tag.getAsList();
				byte type = list.isEmpty() ? 1 : list.get(0).getType().getId();
				out.writeByte(type);
				out.writeInt(list.size());
				for (NBT n : list) writeTag(n, out);
				break;
			case COMPOUND:
				NBTCompound map = tag.getAsCompound();
				for (Map.Entry<String, NBT> entry : map.entrySet())
				{
					out.writeByte(entry.getValue().getType().getId());
					out.writeUTF(entry.getKey());
					writeTag(entry.getValue(), out);
				}
				out.writeByte(0);
				break;
			case INT_ARR:
				int[] intArray = tag.getAsPrimitive().getAsIntArray();
				out.writeInt(intArray.length);
				for (int i = 0; i < intArray.length; i++)
					out.writeInt(intArray[i]);
				break;
		}
		throw new IOException("Invalid NBT tag type (1-11): " + tp.ordinal());
	}

	NBTType(ReadFunction read)
	{
		this.readFunction = read;
	}

	private interface ReadFunction
	{
		Object apply(DataInput input) throws IOException;
	}

	private ReadFunction readFunction;
}
