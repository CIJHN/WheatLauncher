package net.wheatlauncher.internal.repository;

import javafx.beans.property.ListProperty;
import javafx.collections.MapChangeListener;
import jdk.internal.org.objectweb.asm.ClassReader;
import net.wheatlauncher.Mod;
import net.wheatlauncher.internal.mod.ModImpl;
import net.wheatlauncher.internal.mod.meta.ModInfo;
import net.wheatlauncher.internal.mod.meta.RuntimeAnnotation;
import net.wheatlauncher.utils.JsonSerializer;
import net.wheatlauncher.utils.Patterns;
import net.wheatlauncher.utils.resource.ArchiveResource;
import net.wheatlauncher.utils.resource.ResourceType;
import org.to2mbn.jmccc.internal.org.json.JSONArray;
import org.to2mbn.jmccc.internal.org.json.JSONObject;
import org.to2mbn.jmccc.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * @author ci010
 */
public class ModControl
{
	private ArchiveRepository<Mod.Release[]> archiveResource;
	private Map<String, ModImpl> modIdToMod = new HashMap<>();

	public ModControl()
	{
		ArchiveRepository.Builder<Mod.Release[]> builder = new ArchiveRepository.Builder<>("mods",
				new JsonSerializer<Mod.Release[]>()
				{
					@Override
					public Mod.Release[] deserialize(JSONObject jsonObject)
					{
						return null;
					}

					@Override
					public JSONObject serialize(Mod.Release[] data)
					{
						return null;
					}
				});
		Function<File, Mod.Release[]> zipJar = (file) -> {
			List<Mod.Release> meta = new ArrayList<>();
			JarFile jar = null;
			try
			{
				jar = new JarFile(file);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			try
			{
				ZipEntry modInfo = jar.getEntry("mcmod.info");
				if (modInfo != null)
				{
					String modInfoString = IOUtils.toString(jar.getInputStream(modInfo));

					JSONArray arr;
					if (modInfoString.startsWith("{"))
						arr = new JSONObject(modInfoString).getJSONArray("modList");
					else
						arr = new JSONArray(modInfoString);

					for (int i = 0; i < arr.length(); i++)
						meta.add(new ModInfo(arr.getJSONObject(i)));
				}
				Set<Map<String, Object>> set = new HashSet<>();

				for (JarEntry jarEntry : Collections.list(jar.entries()))
					if (Patterns.CLASS_FILE.matcher(jarEntry.getName()).matches())
					{
						set.clear();
						ClassReader reader = new ClassReader(jar.getInputStream(jarEntry));
						reader.accept(new RuntimeAnnotation.Visitor(set), 0);
						meta.addAll(set.stream().map(RuntimeAnnotation::new).collect(Collectors.toList()));
					}
			}
			catch (Exception ignored) {}
			return meta.toArray(new Mod.Release[meta.size()]);
		};
		builder.registerParser(ResourceType.JAR, zipJar).registerParser(ResourceType.ZIP, zipJar);
		archiveResource = builder.build();
		archiveResource.getAllStorage().addListener(new MapChangeListener<String, ArchiveResource<Mod.Release[]>>()
		{
			@Override
			public void onChanged(Change<? extends String, ? extends ArchiveResource<Mod.Release[]>> change)
			{
				for (Mod.Release modMeta : change.getValueAdded().getContainData())
					if (modIdToMod.containsKey(modMeta.getModId()))
						modIdToMod.get(modMeta.getModId()).register(modMeta);
					else
					{
						ModImpl entry = new ModImpl(modMeta.getModId());
						entry.register(modMeta);
						modIdToMod.put(modMeta.getModId(), entry);
					}
			}
		});
	}

	public Set<String> getAllModId()
	{
		return modIdToMod.keySet();
	}

	public Mod getMod(String modid)
	{
		return modIdToMod.get(modid);
	}

	private ListProperty<Mod.Release> selectMod;
//	private boolean register(ModFile file)
//	{
//		if (file == null)
//			return false;
//		if (allModFile.contains(file))
//			return false;
//		allModFile.add(file);
//		for (Artifact.Release modMeta : file.getContainData())
//			if (modIdToMod.containsKey(modMeta.getModId()))
//				modIdToMod.get(modMeta.getModId()).register(modMeta);
//			else
//			{
//				ModImpl entry = new ModImpl(modMeta.getModId());
//				entry.register(modMeta);
//				modIdToMod.put(modMeta.getModId(), entry);
//			}
//		return true;
//	}

}