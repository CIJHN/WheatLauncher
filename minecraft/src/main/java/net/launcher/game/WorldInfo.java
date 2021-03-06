package net.launcher.game;

import javafx.scene.image.Image;
import net.launcher.game.nbt.NBT;
import net.launcher.game.nbt.NBTCompound;
import net.launcher.utils.serial.Deserializer;
import net.launcher.utils.serial.SerializedWriter;
import org.to2mbn.jmccc.option.MinecraftDirectory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Collections;

/**
 * @author ci010
 */
public class WorldInfo
{
	private String fileName, displayName;
	private long size, lastPlayed;
	private GameType gameType;
	private boolean isHardCore, enabledCheat;

	private int spawnX, spawnY, spawnZ;

	public WorldInfo(String fileName, String displayName, long size, long lastPlayed, GameType gameType, boolean isHardCore, boolean enabledCheat, int spawnX, int spawnY, int spawnZ)
	{
		this.fileName = fileName;
		this.displayName = displayName;
		this.size = size;
		this.lastPlayed = lastPlayed;
		this.gameType = gameType;
		this.isHardCore = isHardCore;
		this.enabledCheat = enabledCheat;
		this.spawnX = spawnX;
		this.spawnY = spawnY;
		this.spawnZ = spawnZ;
	}

	public int getSpawnX()
	{
		return spawnX;
	}

	public int getSpawnY()
	{
		return spawnY;
	}

	public int getSpawnZ()
	{
		return spawnZ;
	}

	public String getFileName()
	{
		return fileName;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public long getSize()
	{
		return size;
	}

	public long getLastPlayed()
	{
		return lastPlayed;
	}

	public GameType getGameType()
	{
		return gameType;
	}

	public boolean isHardCore()
	{
		return isHardCore;
	}

	public boolean isEnabledCheat()
	{
		return enabledCheat;
	}

	public void setDisplayName(String displayName)
	{
		this.displayName = displayName;
	}

	public void setGameType(GameType gameType)
	{
		this.gameType = gameType;
	}

	public void setHardCore(boolean hardCore)
	{
		isHardCore = hardCore;
	}

	public void setEnabledCheat(boolean enabledCheat)
	{
		this.enabledCheat = enabledCheat;
	}

	public void setSpawnX(int spawnX)
	{
		this.spawnX = spawnX;
	}

	public void setSpawnY(int spawnY)
	{
		this.spawnY = spawnY;
	}

	public void setSpawnZ(int spawnZ)
	{
		this.spawnZ = spawnZ;
	}

	public static Image getIcon(WorldInfo info, MinecraftDirectory minecraftDirectory) throws FileNotFoundException
	{
		return new Image(new FileInputStream(minecraftDirectory.getRoot().toPath().resolve(info.getFileName())
				.resolve("icon.png").toFile()));
	}

	public static Image getIcon(WorldInfo info, Path saveDir) throws FileNotFoundException
	{
		return new Image(new FileInputStream(saveDir.resolve(info.getFileName()).resolve("icon.png").toFile()));
	}

	public static WorldInfo deserialize(Path levelFile) throws Exception
	{
		String fileName = levelFile.getParent().getFileName().toString();
		return SERIALIZER.deserialize(NBT.read(levelFile, true).asCompound(), Collections.singletonMap("fileName",
				fileName));
	}

	public static Deserializer<WorldInfo, NBTCompound> SERIALIZER = (compound, context) ->
	{
		compound = compound.get("Data").asCompound();
		long lastPlayed1 = compound.get("LastPlayed").asLong();
		long sizeOnDisk = compound.get("SizeOnDisk").asLong();
		String levelName = compound.get("LevelName").asString();
		boolean hardcore = compound.get("hardcore").asBool();
		GameType gameType = GameType.getByID(compound.get("GameType").asPrimitive().asInt());
		boolean allowCommands = compound.getOption("allowCommands").orElse(NBT.bool(gameType == GameType.CREATIVE))
				.asPrimitive().asBool();
		int spawnX1 = compound.get("SpawnX").asPrimitive().asInt();
		int spawnY1 = compound.get("SpawnY").asPrimitive().asInt();
		int spawnZ1 = compound.get("SpawnZ").asPrimitive().asInt();
		String fileName1 = (String) context.get("fileName");
		if (fileName1 == null) fileName1 = "";

		return new WorldInfo(fileName1, levelName, sizeOnDisk, lastPlayed1, gameType, hardcore, allowCommands, spawnX1,
				spawnY1, spawnZ1);
	};

	public static SerializedWriter<WorldInfo, NBTCompound> WRITER = (instance, serialized, context) ->
	{
		serialized.put("LastPlayed", instance.getLastPlayed());
		serialized.put("SizeOnDisk", instance.getSize());
		serialized.put("LevelName", instance.getDisplayName());
		serialized.put("hardcore", instance.isHardCore());
		serialized.put("GameType", instance.getGameType().getId());
		serialized.put("allowCommands", instance.isEnabledCheat());
		serialized.put("SpawnX", instance.getSpawnX());
		serialized.put("SpawnY", instance.getSpawnY());
		serialized.put("SpawnZ", instance.getSpawnZ());
		return serialized;
	};

	@Override
	public String toString()
	{
		return "WorldInfo{" +
				"fileName='" + fileName + '\'' +
				", displayName='" + displayName + '\'' +
				", size=" + size +
				", lastPlayed=" + lastPlayed +
				", gameType=" + gameType +
				", isHardCore=" + isHardCore +
				", enabledCheat=" + enabledCheat +
				", spawnX=" + spawnX +
				", spawnY=" + spawnY +
				", spawnZ=" + spawnZ +
				'}';
	}
}
